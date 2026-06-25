package cc.wlizhi.eddie.tools.tool;

import cc.wlizhi.eddie.common.tool.BuiltInToolProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * 网页抓取工具集
 * <p>
 * 两个工具供 AI 模型通过 function calling 调用：
 * <ul>
 *   <li>{@code fetch_markdown} — 抓取网页返回 Markdown</li>
 *   <li>{@code fetch_json} — 抓取网页返回 JSON</li>
 * </ul>
 */
@Component
public class WebFetchTools implements BuiltInToolProvider {

    private static final Logger log = LoggerFactory.getLogger(WebFetchTools.class);

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; EddieBot/1.0)";
    private static final int TIMEOUT_SECONDS = 15;
    private static final int DEFAULT_MAX_CHARS = 8_000;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WebFetchTools() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Tool(name = "built_in_fetch_markdown",
            description = "获取指定 URL 的网页内容，提取正文后返回干净的 Markdown 文本，适合 LLM 阅读")
    public String fetchMarkdown(
            @ToolParam(description = "要抓取的 URL 列表") List<String> urls,
            @ToolParam(required = false, description = "最大返回字符数，默认 8000") Integer maxCharacters,
            @ToolParam(required = false, description = "模式：article（提取正文）或 full（全文），默认 article") String mode) {

        if (urls == null || urls.isEmpty()) return "错误：未提供 URL";

        int maxChars = maxCharacters != null ? maxCharacters : DEFAULT_MAX_CHARS;
        String actualMode = mode != null ? mode : "article";
        boolean isFull = "full".equals(actualMode);

        StringBuilder result = new StringBuilder();
        int totalChars = 0;

        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            try {
                Document doc = fetchAndParse(url);
                String title = extractTitle(doc);
                Element article = findArticle(doc, isFull);
                String markdown = toMarkdown(article, title);

                if (urls.size() > 1) {
                    result.append("---\n来源 ").append(i + 1).append(": ").append(url).append("\n\n");
                }
                result.append(markdown).append("\n\n");
                totalChars = result.length();

                if (totalChars > maxChars) {
                    result.setLength(maxChars);
                    result.append("\n\n...（内容已截断）");
                    break;
                }

                log.info("[fetch_markdown] {} → {} chars", url, markdown.length());

            } catch (Exception e) {
                log.error("[fetch_markdown] 失败: {}", url, e);
                if (urls.size() > 1) {
                    result.append("---\n来源 ").append(i + 1).append(": ").append(url).append("\n\n");
                }
                result.append("抓取失败: ").append(e.getMessage()).append("\n\n");
            }
        }

        return result.toString().strip();
    }

    @Tool(name = "built_in_fetch_json",
            description = "获取指定 URL 的 JSON 数据（API 端点专用）。URL 必须返回 application/json，否则报错")
    public String fetchJson(
            @ToolParam(description = "要获取的 JSON 数据 URL") String url) {

        if (url == null || url.isBlank()) return "{\"error\": \"未提供 URL\"}";

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            String body = resp.body();
            String contentType = resp.headers().firstValue("Content-Type").orElse("");

            if (status < 200 || status >= 300) {
                log.error("[fetch_json] HTTP {}: {}", status, url);
                return "{\"error\": \"HTTP " + status + ": " + body.replace("\"", "'") + "\"}";
            }

            // 验证 Content-Type 是否为 JSON
            if (!contentType.toLowerCase().contains("application/json")) {
                log.error("[fetch_json] 非 JSON 响应 ({}): {}", contentType, url);
                return "{\"error\": \"响应不是 JSON 格式，Content-Type: " + contentType.replace("\"", "'") + "\"}";
            }

            // 用 ObjectMapper 解析验证，确保是合法 JSON，然后美化输出
            Object jsonNode = objectMapper.readValue(body, Object.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);

            log.info("[fetch_json] {} → {} bytes, HTTP {}", url, body.length(), status);
            return prettyJson;

        } catch (Exception e) {
            log.error("[fetch_json] 失败: {}", url, e);
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    // ==================== 私有方法 ====================

    private Document fetchAndParse(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        String charset = "UTF-8";
        if (contentType.toLowerCase().contains("charset=")) {
            charset = contentType.toLowerCase().split("charset=")[1].split(";")[0].trim();
        }
        return Jsoup.parse(new String(resp.body(), charset));
    }

    private String extractTitle(Document doc) {
        Element og = doc.selectFirst("meta[property=og:title]");
        if (og != null && !og.attr("content").isBlank()) return og.attr("content").strip();
        Element h1 = doc.selectFirst("h1");
        if (h1 != null && !h1.text().isBlank()) return h1.text().strip();
        String t = doc.title();
        return t != null ? t.strip() : "";
    }

    private Element findArticle(Document doc, boolean full) {
        if (full) {
            Element body = doc.body();
            if (body != null) {
                body.select("script, style, nav, footer, header, .ad, .ads, .advertisement").remove();
                return body;
            }
            return doc;
        }

        String[] selectors = {
                "article", "[role=main]", "main",
                ".post-content", ".article-content", ".entry-content",
                ".content", "#content", "#article"
        };
        for (String s : selectors) {
            Element el = doc.selectFirst(s);
            if (el != null) {
                cleanup(el);
                if (el.text().length() > 150) return el;
            }
        }

        Element body = doc.body();
        if (body != null) {
            cleanup(body);
            return body;
        }
        return doc;
    }

    private void cleanup(Element el) {
        el.select("script, style, nav, footer, header, " +
                ".ad, .ads, .advertisement, .adsbygoogle, " +
                ".sidebar, .comment, .comments, .related-posts, " +
                ".menu, .widget, .social-share").remove();
    }

    private String toMarkdown(Element article, String title) {
        StringBuilder md = new StringBuilder();
        if (title != null && !title.isBlank()) {
            md.append("# ").append(title.strip()).append("\n\n");
        }
        if (article != null) {
            convert(article, md, 0);
        }
        return md.toString().strip();
    }

    private void convert(Element el, StringBuilder md, int depth) {
        for (var child : el.childNodes()) {
            if (child instanceof TextNode tn) {
                String t = tn.text().strip();
                if (!t.isEmpty()) md.append(t).append(" ");
                continue;
            }
            if (!(child instanceof Element e)) continue;

            switch (e.tagName().toLowerCase()) {
                case "h1" -> md.append("\n# ").append(e.text().strip()).append("\n\n");
                case "h2" -> md.append("\n## ").append(e.text().strip()).append("\n\n");
                case "h3" -> md.append("\n### ").append(e.text().strip()).append("\n\n");
                case "h4" -> md.append("\n#### ").append(e.text().strip()).append("\n\n");
                case "h5" -> md.append("\n##### ").append(e.text().strip()).append("\n\n");
                case "h6" -> md.append("\n###### ").append(e.text().strip()).append("\n\n");

                case "p" -> {
                    convert(e, md, depth);
                    md.append("\n\n");
                }

                case "a" -> {
                    String href = e.attr("href");
                    String text = e.text().strip();
                    if (!text.isEmpty()) {
                        if (!href.isEmpty() && !href.startsWith("#")) {
                            md.append("[").append(text).append("](").append(href).append(") ");
                        } else {
                            md.append(text).append(" ");
                        }
                    }
                }
                case "strong", "b" -> md.append("**").append(e.text()).append("** ");
                case "em", "i" -> md.append("*").append(e.text()).append("* ");
                case "code" -> {
                    if (e.parent() == null || !"pre".equals(e.parent().tagName().toLowerCase())) {
                        md.append("`").append(e.text()).append("` ");
                    }
                }
                case "pre" -> {
                    md.append("\n```\n").append(e.text().strip()).append("\n```\n\n");
                }
                case "blockquote" -> {
                    for (Element line : e.children()) {
                        md.append("> ").append(line.text().strip()).append("\n");
                    }
                    md.append("\n");
                }
                case "ul" -> {
                    for (Element li : e.children()) {
                        if ("li".equals(li.tagName().toLowerCase())) {
                            md.append("- ").append(li.text().strip()).append("\n");
                        }
                    }
                    md.append("\n");
                }
                case "ol" -> {
                    int idx = 1;
                    for (Element li : e.children()) {
                        if ("li".equals(li.tagName().toLowerCase())) {
                            md.append(idx++).append(". ").append(li.text().strip()).append("\n");
                        }
                    }
                    md.append("\n");
                }
                case "img" -> {
                    String src = e.attr("src");
                    String alt = e.attr("alt");
                    if (!src.isEmpty()) {
                        md.append("![").append(alt.isEmpty() ? "image" : alt).append("](").append(src).append(") ");
                    }
                }
                case "hr" -> md.append("\n---\n\n");
                case "br" -> md.append("\n");

                case "div", "section", "article", "main", "li", "span",
                     "figure", "figcaption", "thead", "tbody", "tr" -> convert(e, md, depth);

                case "th", "td" -> {
                    md.append("| ").append(e.text().strip()).append(" ");
                }
                case "table" -> {
                    for (Element row : e.select("tr")) {
                        md.append("|");
                        for (Element cell : row.select("th, td")) {
                            md.append(" ").append(cell.text().strip()).append(" |");
                        }
                        md.append("\n");
                        if (row.parent() != null && "thead".equals(row.parent().tagName().toLowerCase())) {
                            int cols = row.select("th, td").size();
                            md.append("|");
                            for (int i = 0; i < cols; i++) md.append(" --- |");
                            md.append("\n");
                        }
                    }
                    md.append("\n");
                }
                default -> {
                    String t = e.text().strip();
                    if (!t.isEmpty()) md.append(t).append("\n\n");
                }
            }
        }
    }
}

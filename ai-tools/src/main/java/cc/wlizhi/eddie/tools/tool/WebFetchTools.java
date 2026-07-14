/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.tools.tool;

import cc.wlizhi.eddie.common.entity.dto.GeneralSettings;
import cc.wlizhi.eddie.common.tool.BuiltInToolProvider;
import cc.wlizhi.eddie.common.util.ConfigUtil;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import cc.wlizhi.eddie.tools.service.WebFetchSummarizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;

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

    @Override
    public String getMcpServerName() {
        return "BuiltInSearch";
    }

    private static final Logger log = LoggerFactory.getLogger(WebFetchTools.class);

    // 真实 Chrome 浏览器 User-Agent 池，每次随机取一个降低指纹识别风险
    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    );
    private static final List<String> MOBILE_USER_AGENTS = List.of(
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.6478.122 Mobile Safari/537.36"
    );
    private static final int TIMEOUT_SECONDS = 15;
    private static final int DEFAULT_MAX_CHARS = 10_000;
    private static final int MAX_CHARS = 20_000;
    private static final String SPA_STUB_MESSAGE = "该网站内容由 JavaScript 动态渲染，当前抓取方式无法获取正文。建议通过搜索引擎摘要或其他来源获取信息。";
    private static final Random RANDOM = new Random();

    private record FetchResult(Document document, String error, Integer httpStatus) {
        boolean isSuccess() {
            return document != null;
        }

        static FetchResult success(Document doc) {
            return new FetchResult(doc, null, null);
        }

        static FetchResult error(int httpStatus, String message) {
            return new FetchResult(null, message, httpStatus);
        }

        static FetchResult error(String message) {
            return new FetchResult(null, message, null);
        }
    }

    private final HttpClient httpClient;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private GlobalConfigContext globalConfigContext;

    @Resource
    private WebFetchSummarizer webFetchSummarizer;

    public WebFetchTools() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .cookieHandler(cookieManager)
                .build();
    }

    @Tool(name = "fetch",
            description = "获取指定 URL 列表的网页内容，提取正文后返回干净的 Markdown 文本，适合 LLM 阅读。每个 URL 独立截断，超限时优先用 LLM 生成摘要")
    public String fetchMarkdown(
            @ToolParam(description = "要抓取的 URL 列表") List<String> urls,
            @ToolParam(required = false, description = "每个网页最大返回字符数，默认 " + DEFAULT_MAX_CHARS + "，最大 " + MAX_CHARS + "，超出部分会用 LLM 生成摘要") Integer maxCharacters,
            @ToolParam(required = false, description = "模式：article（提取正文）或 full（全文），默认 article") String mode,
            @ToolParam(required = false, description = "摘要关注方向，帮助 LLM 聚焦相关内容（如：技术细节、价格信息、核心论点等）") String purpose) {

        if (urls == null || urls.isEmpty()) return "错误：未提供 URL";

        int maxChars = resolveMaxResults(maxCharacters);
        String actualMode = mode != null ? mode : "article";
        boolean isFull = "full".equals(actualMode);

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            FetchResult fr = fetchAndParse(url);
            if (!fr.isSuccess()) {
                log.warn("[fetch_markdown] 失败: {} [HTTP {}] {}", url, fr.httpStatus(), fr.error());
                if (urls.size() > 1) {
                    result.append("---\n来源 ").append(i + 1).append(": ").append(url).append("\n\n");
                }
                result.append("抓取失败")
                        .append(fr.httpStatus() != null ? " [HTTP " + fr.httpStatus() + "]" : "")
                        .append(": ").append(fr.error()).append("\n\n");
                continue;
            }
            Document doc = fr.document();
            String title = extractTitle(doc);
            Element article = findArticle(doc, isFull);
            String markdown = toMarkdown(article, title);

            // SPA 占位符检测：如果检测到疑似 JS 渲染的占位符，用移动端 UA 重试一次
            if (isSpaStub(markdown)) {
                log.warn("[fetch_markdown] SPA stub 检测到，尝试移动端 UA 重试: {}", url);
                fr = fetchAndParse(url, pickUserAgent(true));
                if (fr.isSuccess()) {
                    doc = fr.document();
                    title = extractTitle(doc);
                    article = findArticle(doc, isFull);
                    markdown = toMarkdown(article, title);
                }
                // 重试失败或仍为 SPA stub 则返回提示信息
                if (!fr.isSuccess() || isSpaStub(markdown)) {
                    log.warn("[fetch_markdown] 移动端 UA 仍为 SPA stub: {}", url);
                    if (urls.size() > 1) {
                        result.append("---\n来源 ").append(i + 1).append(": ").append(url).append("\n\n");
                    }
                    result.append(SPA_STUB_MESSAGE).append("\n\n");
                    continue;
                }
            }

            if (urls.size() > 1) {
                result.append("---\n来源 ").append(i + 1).append(": ").append(url).append("\n\n");
            }

            // 每个网页独立截断，优先用 LLM 摘要
            if (markdown.length() > maxChars) {
                String summary = webFetchSummarizer.summarize(markdown, purpose, maxChars >> 2);
                if (summary != null) {
                    markdown = summary;
                } else {
                    markdown = markdown.substring(0, maxChars) + "\n\n...（内容已截断）";
                }
            }
            result.append(markdown).append("\n\n");

            log.info("[fetch_markdown] {} → {} chars", url, markdown.length());
        }

        return result.toString().strip();
    }

    @Tool(name = "fetch_json",
            description = "获取指定 URL 的 JSON 数据（API 端点专用）。URL 必须返回 application/json，否则报错")
    public String fetchJson(
            @ToolParam(description = "要获取的 JSON 数据 URL") String url) {

        if (url == null || url.isBlank()) return "{\"error\": \"未提供 URL\"}";

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", pickUserAgent(false))
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

    private FetchResult fetchAndParse(String url) {
        return fetchAndParse(url, pickUserAgent(false));
    }

    /**
     * 发起 HTTP 请求并解析 HTML。支持：
     * <ul>
     *   <li>真实浏览器请求头伪装</li>
     *   <li>遇到 403/503/429 时自动用移动端 UA 重试</li>
     *   <li>从响应头自动检测 charset</li>
     * </ul>
     */
    private FetchResult fetchAndParse(String url, String userAgent) {
        try {
            HttpRequest req = buildBrowserRequest(url, userAgent);
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            int status = resp.statusCode();

            // 遇到 Cloudflare/反爬/限流状态码，用移动端 UA + 不同请求头组合重试一次
            if (status == 403 || status == 503 || status == 429) {
                log.warn("[fetch] HTTP {} 尝试移动端 UA 重试: {}", status, url);
                HttpRequest retryReq = buildBrowserRequest(url, pickUserAgent(true));
                resp = httpClient.send(retryReq, HttpResponse.BodyHandlers.ofByteArray());
                status = resp.statusCode();
                // 重试后仍为反爬状态码，直接返回错误结果，不抛异常
                if (status == 403 || status == 503 || status == 429) {
                    return FetchResult.error(status, "目标网站启用了反爬保护（Cloudflare/js challenge），无法直接抓取");
                }
            }

            return parseResponse(url, resp);
        } catch (UnknownHostException e) {
            log.warn("[fetch] DNS 解析失败: {}", url, e);
            return FetchResult.error("DNS 解析失败，域名无法访问: " + e.getMessage());
        } catch (HttpConnectTimeoutException e) {
            log.warn("[fetch] 连接超时: {}", url, e);
            return FetchResult.error("连接超时，目标服务器无响应");
        } catch (SocketTimeoutException e) {
            log.warn("[fetch] 读取超时: {}", url, e);
            return FetchResult.error("读取超时，目标服务器响应过慢");
        } catch (SSLException e) {
            log.warn("[fetch] SSL 握手失败: {}", url, e);
            return FetchResult.error("SSL/TLS 握手失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("[fetch] URL 格式错误: {}", url, e);
            return FetchResult.error("URL 格式错误: " + e.getMessage());
        } catch (InterruptedException e) {
            log.warn("[fetch] 请求被中断: {}", url, e);
            Thread.currentThread().interrupt();
            return FetchResult.error("请求被中断");
        } catch (IOException e) {
            log.warn("[fetch] 网络请求失败: {}", url, e);
            return FetchResult.error("网络请求失败: " + e.getMessage());
        }
    }

    /**
     * 从 UA 池中随机选取一个 User-Agent
     */
    private String pickUserAgent(boolean mobile) {
        List<String> pool = mobile ? MOBILE_USER_AGENTS : USER_AGENTS;
        return pool.get(RANDOM.nextInt(pool.size()));
    }

    /**
     * 构建模拟真实浏览器的 HTTP 请求
     */
    private HttpRequest buildBrowserRequest(String url, String userAgent) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .header("Upgrade-Insecure-Requests", "1")
                .header("DNT", "1")
                .header("Referer", extractReferer(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();
    }

    /**
     * 从 URL 提取基本 Referer（协议 + 主机）
     */
    private String extractReferer(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme() != null ? uri.getScheme() : "https";
            String host = uri.getHost();
            return scheme + "://" + (host != null ? host : "");
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * 解析 HTTP 响应为 Jsoup Document，自动处理 charset
     */
    private FetchResult parseResponse(String url, HttpResponse<byte[]> resp) {
        try {
            String contentType = resp.headers().firstValue("Content-Type").orElse("");
            String charset = detectCharset(contentType);
            String contentEncoding = resp.headers().firstValue("Content-Encoding").orElse("");
            log.debug("[fetch] {} → HTTP {}, Content-Type: {}, Content-Encoding: {}", url, resp.statusCode(), contentType, contentEncoding);

            byte[] body = resp.body();
            // 检测 Content-Encoding，仅处理 gzip（JDK 内置，兼容 AOT）
            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                try (var gzip = new GZIPInputStream(new ByteArrayInputStream(body))) {
                    body = gzip.readAllBytes();
                }
            }
            // deflate / br 已在 Accept-Encoding 中移除，服务器不会返回，无需处理
            return FetchResult.success(Jsoup.parse(new String(body, charset)));
        } catch (Exception e) {
            log.warn("[fetch] 解析响应失败: {}", url, e);
            return FetchResult.error("响应解析失败: " + e.getMessage());
        }
    }

    /**
     * 从 Content-Type 中提取 charset，不区分大小写
     */
    private String detectCharset(String contentType) {
        String lower = contentType.toLowerCase();
        int charsetIdx = lower.indexOf("charset=");
        if (charsetIdx >= 0) {
            String charset = lower.substring(charsetIdx + 8).split(";")[0].trim();
            if (!charset.isEmpty()) return charset;
        }
        return "UTF-8";
    }

    private String extractTitle(Document doc) {
        Element og = doc.selectFirst("meta[property=og:title]");
        if (og != null && !og.attr("content").isBlank()) return og.attr("content").strip();
        Element h1 = doc.selectFirst("h1");
        if (h1 != null && !h1.text().isBlank()) return h1.text().strip();
        String t = doc.title();
        return t != null ? t.strip() : "";
    }

    /**
     * 基于 Readability 评分算法查找正文容器。
     * <p>
     * 先尝试用常见选择器精确匹配，若命中且内容足够则直接返回。
     * 否则退化为启发式评分：遍历所有块级候选元素，按文本密度、段落数、
     * 类名模式、链接密度等指标打分，选择综合分数最高的。
     */
    private Element findArticle(Document doc, boolean full) {
        if (full) {
            Element body = doc.body();
            if (body != null) {
                body.select("script, style, nav, footer, header, .ad, .ads, .advertisement").remove();
                return body;
            }
            return doc;
        }

        // 第一轮：精确选择器快速命中
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

        // 第二轮：启发式评分 — 遍历块级候选，选最高分
        Element best = null;
        int bestScore = 0;
        for (Element el : doc.select("body > div, body > section, body > main, body > article, " +
                "div[class*=\"content\"], div[class*=\"main\"], div[class*=\"wrapper\"], " +
                "div[class*=\"container\"], div[class*=\"article\"], div[class*=\"post\"], " +
                "div[class*=\"doc\"], div[id*=\"content\"], div[id*=\"main\"]")) {
            int score = scoreElement(el);
            if (score > bestScore) {
                bestScore = score;
                best = el;
            }
        }

        if (best != null && bestScore > 20) {
            cleanup(best);
            return best;
        }

        // 第三轮：兜底 — body 并清理噪音
        Element body = doc.body();
        if (body != null) {
            cleanup(body);
            return body;
        }
        return doc;
    }

    /**
     * 对候选元素进行启发式评分，参考 Mozilla Readability 的核心思路。
     * <p>
     * 评分维度：
     * <ul>
     *   <li>类名/ID 模式匹配（内容类 +25，噪音类 -25）</li>
     *   <li>段落密度（每个 &lt;p&gt; +5）</li>
     *   <li>标题结构（每个 &lt;h1&gt;-&lt;h6&gt; +3）</li>
     *   <li>图片数量（每个 &lt;img&gt; +3）</li>
     *   <li>自然语言特征（逗号/句号密度 >1% +10）</li>
     *   <li>链接密度惩罚（链接文本占比 >5% 时扣分）</li>
     *   <li>代码文档特征（&lt;br&gt; 密集 +3）</li>
     *   <li>文本长度奖励（鼓励选择有内容的块）</li>
     * </ul>
     */
    private int scoreElement(Element el) {
        int score = 0;
        String cls = el.className().toLowerCase();
        String id = el.id().toLowerCase();

        // 类名/ID 模式匹配
        if (cls.contains("content") || cls.contains("article")
                || cls.contains("post") || cls.contains("entry")
                || cls.contains("doc") || cls.contains("documentation")
                || cls.contains("body") || cls.contains("text")
                || id.contains("content") || id.contains("article")
                || id.contains("main") || id.contains("doc")) {
            score += 25;
        }
        if (cls.contains("sidebar") || cls.contains("comment")
                || cls.contains("nav") || cls.contains("footer")
                || cls.contains("widget") || cls.contains("menu")
                || cls.contains("aside") || cls.contains("ad-")
                || id.contains("sidebar") || id.contains("comment")
                || id.contains("nav") || id.contains("footer")) {
            score -= 25;
        }

        // 段落密度 — 正文的标志
        int pCount = el.select("p").size();
        score += pCount * 5;

        // 标题结构
        int hCount = el.select("h1, h2, h3, h4, h5, h6").size();
        score += hCount * 3;

        // 图片数量
        score += el.select("img").size() * 3;

        // 自然语言特征：逗号/句号密度
        String text = el.text();
        if (text.length() > 50) {
            long punctuation = text.chars().filter(c -> c == ',' || c == '，' || c == '.' || c == '。' || c == ';' || c == '；').count();
            if ((double) punctuation / text.length() > 0.01) {
                score += 10;
            }
        }

        // 链接密度惩罚：链接文本占比过高说明是导航/目录/索引
        int linkTextLen = 0;
        for (Element a : el.select("a")) {
            linkTextLen += a.text().length();
        }
        if (text.length() > 0) {
            double linkDensity = (double) linkTextLen / text.length();
            if (linkDensity > 0.5) {
                score -= 50; // 链接远多于正文，极可能是导航
            } else if (linkDensity > 0.2) {
                score -= 20;
            } else if (linkDensity > 0.05) {
                score -= 5;
            }
        }

        // 代码文档特征（<br> 代替 <p> 密集换行）
        int brCount = el.select("br").size();
        if (brCount > 5) {
            score += 3;
        }

        // 文本长度奖励（鼓励有实质内容的块，但不过分）
        score += Math.min(text.length() / 100, 20);

        return score;
    }

    /**
     * 检测提取的文本是否为 SPA 占位符（页面需要 JS 渲染但没有执行）。
     * <p>
     * 检测策略：
     * <ol>
     *   <li>文本总长度过短（<100 字符）且包含 SPA 特征词 → SPA stub</li>
     *   <li>占位符关键词（loading/spinner 等）占比过高 → SPA stub</li>
     *   <li>Only 1-2 行且不含任何标点符号（纯 JS 渲染空白页）→ SPA stub</li>
     * </ol>
     */
    private boolean isSpaStub(String markdown) {
        if (markdown == null || markdown.isBlank()) return true;
        String lower = markdown.toLowerCase();

        // 策略 1：文本极短 + SPA 特征词 → 判定为占位符
        // 覆盖知乎 "发现更大的世界"、掘金 "⛽️" 等场景
        if (markdown.length() < 150) {
            String[] spaKeywords = {
                    "loading", "...", "spinner", "skeleton",
                    "正在加载", "加载中", "请稍候",
                    "发现更大的世界", // 知乎
                    "⛽️", "🚀",       // 掘金等 SPA 站点
                    "react", "vue", "angular", // SPA 框架标识
                    "app", "root",     // SPA 根容器
                    "欢迎", "欢迎来到", "欢迎访问",
                    "请启用 javascript", "enable javascript",
                    "redirecting", "跳转中"
            };
            for (String kw : spaKeywords) {
                if (lower.contains(kw)) return true;
            }
        }

        // 策略 2：计算各行中包含占位符关键词的比例
        long totalLines = lower.lines().count();
        if (totalLines > 3) {
            long placeholderCount = lower.lines()
                    .filter(l -> l.contains("loading") || l.contains("...")
                            || l.contains("spinner") || l.contains("skeleton")
                            || l.contains("正在加载") || l.contains("加载中"))
                    .count();
            if ((double) placeholderCount / totalLines > 0.3) return true;
        }

        // 策略 3：仅 1-2 行且不含任何中文/英文句号 → 极可能是空白页
        if (totalLines <= 2 && !lower.contains("。") && !lower.contains(".")
                && !lower.contains("，") && !lower.contains(",")
                && !lower.contains("！") && !lower.contains("!")) {
            return true;
        }

        return false;
    }

    /**
     * 智能清洗噪音元素。
     * <p>
     * 第一层：用 CSS 选择器精确匹配已知噪音模式（标签名、类名、ID），直接删除。
     * 第二层：对于可能误伤的选择器（如 banner、notice），删除前检查是否含显著段落（>= 3 个 &lt;p&gt;），
     * 有则跳过，避免误删正文。
     */
    private void cleanup(Element el) {
        // 第一层：精确匹配，直接删除
        el.select(
                // 技术标签（决不包含正文）
                "script, style, iframe, noscript, svg, canvas, " +
                        // 页面结构标签
                        "nav, footer, header, aside, " +
                        // Cookie / GDPR / 隐私弹窗
                        "div[class*=cookie], div[id*=cookie], " +
                        "div[class*=consent], div[id*=consent], " +
                        "div[class*=gdpr], div[id*=gdpr], " +
                        "div[class*=\"privacy\"], div[id*=\"privacy\"], " +
                        // 遮罩层 / 模态框 / 弹窗
                        "div[class*=overlay], div[id*=overlay], " +
                        "div[class*=modal], div[id*=modal], " +
                        "div[class*=popup], div[id*=popup], " +
                        "div[class*=dialog], div[id*=dialog], " +
                        // 广告
                        ".ad, .ads, .advertisement, .adsbygoogle, " +
                        "div[class*=ad-], div[id*=ad-], " +
                        "div[class*=sponsor], div[id*=sponsor], " +
                        "div[class*=\"promo\"], " +
                        // 侧栏 / 评论 / 推荐
                        ".sidebar, .comment, .comments, .comment-list, " +
                        ".related-posts, .recommend, " +
                        // 小工具 / 社交
                        ".menu, .widget, .social-share, .share-buttons, " +
                        "div[class*=social], " +
                        // 浮动通知 / 订阅
                        "div[class*=toast], div[class*=notification], " +
                        "div[class*=subscribe], div[id*=subscribe], " +
                        "div[class*=newsletter], div[id*=newsletter], " +
                        // 底部版权 / 面包屑 / 分页
                        ".copyright, .breadcrumb, .pagination, " +
                        // 登录/注册提示
                        "div[class*=login], div[class*=signup]"
        ).remove();

        // 第二层：带保护的删除 — 只删除不含显著段落的元素
        safeRemove(el, "div[class*=banner], div[id*=banner], " +
                "div[class*=notice], div[id*=notice], " +
                "div[class*=alert], " +
                "div[class*=toolbar], " +
                "div[class*=sticky]");
    }

    /**
     * 安全删除：只删除不包含显著正文内容的候选元素。
     * 显著正文判定：元素内 &lt;p&gt; 标签数量 >= 3。
     */
    private void safeRemove(Element parent, String cssQuery) {
        var candidates = parent.select(cssQuery);
        for (var e : candidates) {
            if (e.select("p").size() < 3) {
                e.remove();
            }
        }
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

    // ==================== 配置读取 ====================

    private int resolveMaxResults(Integer maxResults) {
        try {
            int minCount = 1000;
            GeneralSettings settings = globalConfigContext.getGeneralSettings();
            int maxCount = Math.min(settings.getWebFetchMaxChars(), MAX_CHARS);
            return ConfigUtil.resolveIntConfig(DEFAULT_MAX_CHARS, maxResults == null ? null : maxResults.toString(), minCount, maxCount);
        } catch (Exception e) {
            log.debug("[fetch_markdown] 解析配置失败，使用默认值", e);
            return DEFAULT_MAX_CHARS;
        }
    }
}

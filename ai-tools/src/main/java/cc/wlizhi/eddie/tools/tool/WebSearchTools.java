/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.tools.tool;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.dto.ConfigFieldDescriptor;
import cc.wlizhi.eddie.common.dto.ConfigSchema;
import cc.wlizhi.eddie.common.entity.McpServerEntity;
import cc.wlizhi.eddie.common.entity.dto.GeneralSettings;
import cc.wlizhi.eddie.common.tool.BuiltInToolProvider;
import cc.wlizhi.eddie.common.util.ConfigUtil;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import cc.wlizhi.eddie.memory.context.OwnerToolBindingContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 内置互联网搜索工具。<p>
 * 三引擎支持：DuckDuckGo Lite（默认）、Bing（国内直连）、Tavily（需 API Key）。<br>
 * 启动时异步探测 DuckDuckGo 可达性，搜索失败时自动降级并开启每分钟后台探测，
 * 探测到 DuckDuckGo 恢复后自动切回。<br>
 * 用户可通过 {@link #getServerConfigSchema()} 在设置中配置默认搜索引擎和 Tavily API Key，
 * 配置存储在 MCP Server 的 {@code source_config} 字段中，以 {@code "server"} 为命名空间 key。
 */
@Component
public class WebSearchTools implements BuiltInToolProvider {

    @Override
    public String getMcpServerName() {
        return "BuiltInSearch";
    }

    private static final Logger log = LoggerFactory.getLogger(WebSearchTools.class);

    private static final String DDG_URL = "https://lite.duckduckgo.com/lite/";
    private static final String BING_URL = "https://cn.bing.com/search";
    private static final String TAVILY_API_URL = "https://api.tavily.com/search";

    private static final int PROBE_TIMEOUT_MS = 3_000;
    private static final int DDG_TIMEOUT_MS = 8_000;
    private static final int BING_TIMEOUT_MS = 8_000;
    private static final int TAVILY_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_MAX_RESULTS = 8;
    private static final int PROBE_INTERVAL_MINUTES = 1;

    /** 内置搜索引擎枚举（不含 Tavily，Tavily 走 API 无需探测） */
    private enum SearchBackend {DUCKDUCKGO, BING}

    /** 可用的搜索引擎列表 */
    private static final List<String> SUPPORTED_ENGINES = List.of("DUCKDUCKGO", "BING", "TAVILY");

    private volatile SearchBackend currentBackend = SearchBackend.DUCKDUCKGO;
    private final AtomicBoolean probing = new AtomicBoolean(false);
    private ScheduledExecutorService probeScheduler;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private GlobalConfigContext globalConfigContext;
    @Resource
    private InitScheduler initScheduler;
    @Resource
    private OwnerToolBindingContext ownerToolBindingContext;

    // ==================== Schema：搜索引擎设置 ====================

    @Override
    public ConfigSchema getServerConfigSchema() {
        return new ConfigSchema("", "搜索引擎设置", "选择默认搜索引擎，Tavily 需填写 API Key",
                List.of(
                        new ConfigFieldDescriptor(
                                "engine", "select", "默认搜索引擎",
                                "AI 未指定搜索引擎时默认使用的引擎",
                                "DUCKDUCKGO", null, false, null, null,
                                List.of(
                                        new ConfigFieldDescriptor.SelectOption("DUCKDUCKGO", "DuckDuckGo（默认）"),
                                        new ConfigFieldDescriptor.SelectOption("BING", "Bing（国内直连）"),
                                        new ConfigFieldDescriptor.SelectOption("TAVILY", "Tavily（需 API Key）")
                                ),
                                null, null
                        ),
                        new ConfigFieldDescriptor(
                                "tavilyApiKey", "password", "Tavily API Key",
                                "从 [app.tavily.com](https://app.tavily.com/home) 注册获取",
                                "", "tvly-...", false, null, null, null,
                                "engine", "TAVILY"
                        )
                )
        );
    }

    // ==================== 搜索配置 ====================

    @Getter
    @Setter
    public static class SearchConfig {
        /** 默认搜索引擎：DUCKDUCKGO / BING / TAVILY */
        private String engine = "DUCKDUCKGO";
        /** Tavily API Key，仅 engine=TAVILY 时有效 */
        private String tavilyApiKey = "";
    }

    /**
     * 从 MCP Server 的 source_config 中加载搜索引擎配置。<p>
     * 配置以 {@code "server"} 为命名空间 key 存储在 sourceConfig JSON 中：<br>
     * {@code { "server": { "engine": "TAVILY", "tavilyApiKey": "tvly-xxx" } }}
     *
     * @return 解析后的配置，解析失败返回默认配置（DUCKDUCKGO）
     */
    private SearchConfig loadSearchConfig() {
        McpServerEntity server = ownerToolBindingContext.getBuiltInMcpServerByName(getMcpServerName());
        if (server == null) {
            return new SearchConfig();
        }
        String sourceConfig = server.getSourceConfig();
        if (sourceConfig == null || sourceConfig.isBlank() || "{}".equals(sourceConfig)) {
            return new SearchConfig();
        }
        try {
            JsonNode root = objectMapper.readTree(sourceConfig);
            // 新格式：以 "server" 为命名空间 key
            JsonNode serverCfg = root.get("server");
            if (serverCfg != null && !serverCfg.isNull() && !serverCfg.isEmpty()) {
                return objectMapper.treeToValue(serverCfg, SearchConfig.class);
            }
            // 兼容旧扁平格式：{ "engine": "...", "tavilyApiKey": "..." }
            return objectMapper.treeToValue(root, SearchConfig.class);
        } catch (Exception e) {
            log.warn("[搜索] 解析 sourceConfig 失败，使用默认配置: {}", e.getMessage());
            return new SearchConfig();
        }
    }

    // ==================== 启动探测 ====================

    @PostConstruct
    void init() {
        initScheduler.addTask(this.getClass().getSimpleName(), 1000000, this::doInit);
    }

    private void doInit() {
        CompletableFuture.runAsync(() -> {
            try {
                if (isDdgReachable()) {
                    currentBackend = SearchBackend.DUCKDUCKGO;
                    log.info("[搜索] 启动探测: DuckDuckGo 可达，使用 DuckDuckGo Lite");
                } else {
                    currentBackend = SearchBackend.BING;
                    log.warn("[搜索] 启动探测: DuckDuckGo 不可达，降级到 Bing（cn.bing.com）");
                }
            } catch (Exception e) {
                log.warn("[搜索] 启动探测异常，保持默认: {}", e.getMessage());
            }
        });
    }

    @PreDestroy
    void destroy() {
        stopProbe();
    }

    // ==================== 搜索入口 ====================

    @Tool(name = "search",
            description = "搜索互联网并返回网页标题、URL 和摘要。适合查找最新信息、技术文档、新闻等。如需阅读全文可再用 fetch 工具")
    public String search(
            @ToolParam(description = "搜索关键词") String query,
            @ToolParam(required = false, description = "可选参数，返回结果数量（1-20），默认值 8") Integer maxResults,
            @ToolParam(required = false, description = "可选参数，搜索引擎（DUCKDUCKGO / BING / TAVILY），未指定则使用用户配置的默认引擎") String engine) {

        int limit = resolveMaxResults(maxResults);
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        // 1. 如果 AI 未指定引擎，从用户配置读取
        if (engine == null || engine.isBlank()) {
            SearchConfig config = loadSearchConfig();
            engine = config.getEngine();
            // 如果配置了 TAVILY 但没有 API Key → 降级到 DDG
            if ("TAVILY".equalsIgnoreCase(engine)
                    && (config.getTavilyApiKey() == null || config.getTavilyApiKey().isBlank())) {
                log.warn("[搜索] Tavily 未配置 API Key，降级到 DuckDuckGo");
                engine = "DUCKDUCKGO";
            }
        }

        // 2. 按引擎执行，失败自动降级
        String resolvedEngine = engine.toUpperCase();
        if (!SUPPORTED_ENGINES.contains(resolvedEngine)) {
            resolvedEngine = "DUCKDUCKGO";
        }
        return searchWithFallback(encodedQuery, query, limit, resolvedEngine);
    }

    // ==================== 引擎执行 + 降级 ====================

    /**
     * 按指定引擎执行搜索，失败时按优先级自动降级到其他可用引擎。
     */
    private String searchWithFallback(String encodedQuery, String rawQuery, int limit, String engine) {
        // 获取 Tavily API Key（降级到 Tavily 时也需要）
        SearchConfig config = loadSearchConfig();
        String tavilyApiKey = config.getTavilyApiKey();

        // 降级优先级：如果首选是 TAVILY → DDG → BING；否则 DDG ↔ BING 互降
        List<String> fallbackOrder;
        if ("TAVILY".equals(engine)) {
            if (tavilyApiKey == null || tavilyApiKey.isBlank()) {
                fallbackOrder = List.of("DUCKDUCKGO", "BING");
            } else {
                fallbackOrder = List.of("TAVILY", "DUCKDUCKGO", "BING");
            }
        } else if ("DUCKDUCKGO".equals(engine)) {
            fallbackOrder = List.of("DUCKDUCKGO", "BING", "TAVILY");
        } else {
            fallbackOrder = List.of("BING", "DUCKDUCKGO", "TAVILY");
        }

        // 尝试每个引擎，直到成功
        for (int i = 0; i < fallbackOrder.size(); i++) {
            String current = fallbackOrder.get(i);
            try {
                String result;
                switch (current) {
                    case "TAVILY" -> result = searchTavily(rawQuery, limit, tavilyApiKey);
                    case "BING" -> result = searchBing(encodedQuery, rawQuery, limit);
                    default -> result = searchDdg(encodedQuery, rawQuery, limit);
                }
                // 如果经过了降级，添加提示
                if (i > 0) {
                    String tip = "⚠️ " + engine + " 暂时不可用，已自动降级到 " + current + " 搜索。\n\n";
                    return tip + result;
                }
                return result;
            } catch (Exception e) {
                log.warn("[搜索] {} 搜索失败: {}", current, e.getMessage());
                // 更新自动探测状态
                if ("DUCKDUCKGO".equals(current)) {
                    if (currentBackend != SearchBackend.BING) {
                        currentBackend = SearchBackend.BING;
                        startProbeIfNeeded();
                    }
                }
            }
        }

        log.error("[搜索] 所有引擎均搜索失败");
        return "搜索失败，请检查网络连接。\n提示：你可以在设置中添加 SearXNG 等 MCP 搜索服务器。";
    }

    // ==================== Tavily 搜索 ====================

    /**
     * 调用 Tavily Search API。
     */
    String searchTavily(String rawQuery, int limit, String apiKey) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "api_key", apiKey,
                "query", rawQuery,
                "search_depth", "advanced",
                "max_results", limit
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TAVILY_API_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(TAVILY_TIMEOUT_MS))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new RuntimeException("Tavily API 返回 " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());

        StringBuilder sb = new StringBuilder();
        sb.append("搜索 \"").append(rawQuery).append("\" 的结果：\n\n");

        // 可选：AI 生成的摘要回答
        JsonNode answer = root.get("answer");
        if (answer != null && !answer.isNull() && !answer.asText().isBlank()) {
            sb.append("📝 **AI 摘要**\n").append(answer.asText()).append("\n\n---\n\n");
        }

        // 结果列表
        JsonNode results = root.get("results");
        if (results != null && results.isArray()) {
            int count = 0;
            for (JsonNode item : results) {
                if (count >= limit) break;
                String title = item.has("title") ? item.get("title").asText().strip() : "";
                String url = item.has("url") ? item.get("url").asText().strip() : "";
                String content = item.has("content") ? item.get("content").asText().strip() : "";
                if (title.isEmpty() && url.isEmpty()) continue;

                sb.append(++count).append(". **").append(title.isEmpty() ? url : title).append("**\n");
                if (!content.isEmpty()) sb.append("   ").append(content).append("\n");
                if (!url.isEmpty()) sb.append("   🔗 ").append(url).append("\n");
                sb.append("\n");
            }
            if (count > 0) {
                sb.append("---\n共找到 ").append(count).append(" 条结果。\n💡 提示：如需阅读全文，可使用 fetch_markdown 工具");
                return sb.toString();
            }
        }

        // 兜底：检查是否有原始内容
        JsonNode rawContent = root.get("raw_content");
        if (rawContent != null && !rawContent.isNull()) {
            sb.append("（原始内容）\n").append(rawContent.asText());
            return sb.toString();
        }

        return "未找到搜索结果。";
    }

    // ==================== DuckDuckGo Lite 搜索 ====================

    String searchDdg(String encodedQuery, String rawQuery, int limit) throws Exception {
        Document doc = Jsoup.connect(DDG_URL + "?q=" + encodedQuery)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(DDG_TIMEOUT_MS)
                .get();

        StringBuilder sb = new StringBuilder();
        sb.append("搜索 \"").append(rawQuery).append("\" 的结果：\n\n");

        // 策略1: a.result-link 选择器
        Elements links = doc.select("a.result-link");
        if (!links.isEmpty()) {
            int count = 0;
            for (Element a : links) {
                if (count >= limit) break;
                String title = a.text().strip();
                if (title.isEmpty()) continue;

                String href = a.attr("href");
                if (!href.startsWith("http")) href = "https:" + href;

                Element row = a.closest("tr");
                String snippet = "";
                if (row != null) {
                    Element nextRow = row.nextElementSibling();
                    if (nextRow != null) {
                        Element snippetEl = nextRow.selectFirst(".result-snippet");
                        if (snippetEl != null) snippet = snippetEl.text().strip();
                    }
                }

                sb.append(++count).append(". **").append(title).append("**\n");
                if (!snippet.isEmpty()) sb.append("   ").append(snippet).append("\n");
                sb.append("   🔗 ").append(href).append("\n\n");
            }
            if (count > 0) {
                sb.append("---\n共找到 ").append(count).append(" 条结果。\n💡 提示：如需阅读全文，可使用 fetch_markdown 工具");
                return sb.toString();
            }
        }

        // 策略2: table 行分组
        Elements tables = doc.select("table");
        for (Element table : tables) {
            Elements rows = table.select("tr");
            int count = 0;
            for (int i = 0; i < rows.size() - 2 && count < limit; i++) {
                Element titleLink = rows.get(i).selectFirst("a[href]");
                if (titleLink == null) continue;
                String href = titleLink.attr("href");
                String title = titleLink.text().strip();
                if (title.isEmpty()) continue;
                if (!href.startsWith("http")) href = "https:" + href;
                String snippet = "";
                if (i + 1 < rows.size()) {
                    Element snipEl = rows.get(i + 1).selectFirst("td");
                    if (snipEl != null) snippet = snipEl.text().strip().replaceAll("^\\s+", "");
                }
                sb.append(++count).append(". **").append(title).append("**\n");
                if (!snippet.isEmpty()) sb.append("   ").append(snippet).append("\n");
                sb.append("   🔗 ").append(href).append("\n\n");
                i += 3;
            }
            if (count > 0) {
                sb.append("---\n共找到 ").append(count).append(" 条结果。\n💡 提示：如需阅读全文，可使用 fetch_markdown 工具");
                return sb.toString();
            }
        }

        // 策略3: 兜底
        Elements allLinks = doc.select("a[href]");
        int count = 0;
        for (Element a : allLinks) {
            if (count >= limit) break;
            String href = a.attr("href");
            String text = a.text().strip();
            if (text.isEmpty() || href.contains("duckduckgo.com")) continue;
            if (!href.startsWith("http")) href = "https:" + href;
            sb.append(++count).append(". **").append(text).append("**\n   🔗 ").append(href).append("\n\n");
        }
        if (count == 0) return "未找到搜索结果。";
        sb.append("---\n共找到 ").append(count).append(" 条结果。");
                return sb.toString();
    }

    // ==================== Bing 搜索（国内可直连，结果干净） ====================

    String searchBing(String encodedQuery, String rawQuery, int limit) throws Exception {
        Document doc = Jsoup.connect(BING_URL + "?q=" + encodedQuery + "&cc=cn")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .timeout(BING_TIMEOUT_MS)
                .get();

        StringBuilder sb = new StringBuilder();
        sb.append("搜索 \"").append(rawQuery).append("\" 的结果：\n\n");

        // 策略1: li.b_algo（Bing 标准结果容器，已稳定多年）
        Elements results = doc.select(".b_algo");
        int count = 0;
        for (Element item : results) {
            if (count >= limit) break;

            Element link = item.selectFirst("h2 a");
            if (link == null) continue;

            String href = link.attr("href");
            String title = link.text().strip();
            if (title.isEmpty() || href.contains("bing.com")) continue;

            Element snippetEl = item.selectFirst(".b_caption p, .b_lineclamp2");
            String snippet = snippetEl != null ? snippetEl.text().strip() : "";

            Element urlEl = item.selectFirst(".b_attribution, cite");
            String source = urlEl != null ? urlEl.text().strip() : "";

            sb.append(++count).append(". **").append(title).append("**\n");
            if (!snippet.isEmpty()) sb.append("   ").append(snippet).append("\n");
            sb.append("   🔗 ").append(href);
            if (!source.isEmpty()) sb.append(" (").append(source).append(")");
            sb.append("\n\n");
        }

        if (count == 0) {
            // 兜底
            Elements links = doc.select("a[href]");
            for (Element a : links) {
                if (count >= limit) break;
                String href = a.attr("href");
                String text = a.text().strip();
                if (text.isEmpty()) continue;
                if (href.contains("bing.com") || href.startsWith("#") || href.startsWith("javascript:")) continue;
                if (!href.startsWith("http")) continue;
                sb.append(++count).append(". **").append(text).append("**\n   🔗 ").append(href).append("\n\n");
            }
            if (count == 0) return "未找到搜索结果。";
        }

        sb.append("---\n共找到 ").append(count).append(" 条结果。\n💡 提示：如需阅读全文，可使用 fetch_markdown 工具");
                return sb.toString();
    }

    // ==================== 后台 DDG 可达性探测 ====================

    private void startProbeIfNeeded() {
        if (!probing.compareAndSet(false, true)) return;
        probeScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "search-probe");
            t.setDaemon(true);
            return t;
        });
        probeScheduler.scheduleAtFixedRate(() -> {
            try {
                if (isDdgReachable()) {
                    log.info("[搜索] 后台探测: DuckDuckGo 已恢复，切回 DuckDuckGo Lite");
                    currentBackend = SearchBackend.DUCKDUCKGO;
                    stopProbe();
                }
            } catch (Exception ignored) {
            }
        }, 0, PROBE_INTERVAL_MINUTES, TimeUnit.MINUTES);
        log.info("[搜索] 后台探测已启动，每分钟探测 DuckDuckGo 可达性");
    }

    private void stopProbe() {
        probing.set(false);
        if (probeScheduler != null && !probeScheduler.isShutdown()) {
            probeScheduler.shutdownNow();
            probeScheduler = null;
        }
    }

    private boolean isDdgReachable() {
        try {
            Document doc = Jsoup.connect(DDG_URL + "?q=test")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(PROBE_TIMEOUT_MS)
                    .get();
            return doc.selectFirst("a.result-link, table") != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 配置读取 ====================

    private int resolveMaxResults(Integer param) {
        try {
            // 绝对边界值
            int minCount = 1;
            GeneralSettings settings = globalConfigContext.getGeneralSettings();
            int maxCount = Math.min(settings.getSearchResultCount(), 20);
            return ConfigUtil.resolveIntConfig(DEFAULT_MAX_RESULTS, param == null ? null : param.toString(), minCount, maxCount);
        } catch (Exception ex) {
            return DEFAULT_MAX_RESULTS;
        }
    }
}

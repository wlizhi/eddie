/**
 * @author Eddie
 * {@code @date} 2026-06-25
 */

package cc.wlizhi.eddie.tools.tool;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.entity.dto.GeneralSettings;
import cc.wlizhi.eddie.common.tool.BuiltInToolProvider;
import cc.wlizhi.eddie.common.util.ConfigUtil;
import cc.wlizhi.eddie.memory.context.GlobalConfigContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 内置互联网搜索工具。<p>
 * 双后端自动切换：DuckDuckGo Lite（默认）→ Bing（降级）。<br>
 * 启动时异步探测 DuckDuckGo 可达性，搜索失败时自动降级并开启每分钟后台探测，
 * 探测到 DuckDuckGo 恢复后自动切回。
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

    private static final int PROBE_TIMEOUT_MS = 3_000;
    private static final int DDG_TIMEOUT_MS = 8_000;
    private static final int BING_TIMEOUT_MS = 8_000;
    private static final int DEFAULT_MAX_RESULTS = 8;
    private static final int PROBE_INTERVAL_MINUTES = 1;

    private enum SearchBackend {DUCKDUCKGO, BING}

    private volatile SearchBackend currentBackend = SearchBackend.DUCKDUCKGO;
    private final AtomicBoolean probing = new AtomicBoolean(false);
    private ScheduledExecutorService probeScheduler;

    @Resource
    private GlobalConfigContext globalConfigContext;
    @Resource
    private InitScheduler initScheduler;

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

    @Tool(name = "search",
            description = "搜索互联网并返回网页标题、URL 和摘要。适合查找最新信息、技术文档、新闻等。如需阅读全文可再用 fetch 工具")
    public String search(
            @ToolParam(description = "搜索关键词") String query,
            @ToolParam(required = false, description = "可选参数，返回结果数量（1-20），默认值 1") Integer maxResults,
            @ToolParam(required = false, description = "可选参数，搜索引擎（DUCKDUCKGO / BING），默认 DUCKDUCKGO") String engine) {

        int limit = resolveMaxResults(maxResults);
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        // 用户指定了搜索引擎
        if (engine != null && !engine.isBlank()) {
            return searchWithEngine(encodedQuery, query, limit, engine);
        }

        // 不传 engine → 走现有自动逻辑
        try {
            if (currentBackend == SearchBackend.DUCKDUCKGO) {
                return searchDdg(encodedQuery, query, limit);
            }
            return searchBing(encodedQuery, query, limit);

        } catch (Exception e) {
            log.warn("[搜索] {} 搜索失败: {}", currentBackend, e.getMessage());

            SearchBackend fallback = (currentBackend == SearchBackend.DUCKDUCKGO)
                    ? SearchBackend.BING : SearchBackend.DUCKDUCKGO;

            currentBackend = fallback;
            log.info("[搜索] 降级到 {}", fallback);

            if (fallback == SearchBackend.BING) {
                startProbeIfNeeded();
            }

            try {
                if (fallback == SearchBackend.DUCKDUCKGO) {
                    return searchDdg(encodedQuery, query, limit);
                }
                return searchBing(encodedQuery, query, limit);
            } catch (Exception ex) {
                log.error("[搜索] 降级后仍然失败", ex);
                return "搜索失败，请检查网络连接。\n提示：你可以在设置中添加 SearXNG 等 MCP 搜索服务器。";
            }
        }
    }

    // ==================== 指定引擎搜索 ====================

    /**
     * 使用用户指定的搜索引擎搜索，失败时自动降级到另一个引擎。
     */
    private String searchWithEngine(String encodedQuery, String rawQuery, int limit, String engine) {
        SearchBackend preferred = parseEngine(engine);
        if (preferred == null) {
            return "错误：不支持的搜索引擎：" + engine + "，可选值：DUCKDUCKGO / BING";
        }

        // 尝试首选引擎
        try {
            if (preferred == SearchBackend.DUCKDUCKGO) {
                return searchDdg(encodedQuery, rawQuery, limit);
            }
            return searchBing(encodedQuery, rawQuery, limit);
        } catch (Exception e) {
            log.warn("[搜索] 指定引擎 {} 搜索失败: {}", preferred, e.getMessage());
        }

        // 降级到另一个引擎
        SearchBackend fallback = (preferred == SearchBackend.DUCKDUCKGO)
                ? SearchBackend.BING : SearchBackend.DUCKDUCKGO;

        try {
            String fallbackResult;
            if (fallback == SearchBackend.DUCKDUCKGO) {
                fallbackResult = searchDdg(encodedQuery, rawQuery, limit);
            } else {
                fallbackResult = searchBing(encodedQuery, rawQuery, limit);
            }

            String tip = "⚠️ 你指定的 " + preferred + " 暂时不可用，已自动降级到 " + fallback + " 搜索。\n\n";
            return tip + fallbackResult;
        } catch (Exception ex) {
            log.error("[搜索] 指定引擎 {} 降级 {} 后仍然失败", preferred, fallback, ex);
            return "错误：你指定的 " + preferred + " 暂时不可用，降级到 " + fallback + " 后也搜索失败，请稍后再试。";
        }
    }

    private static SearchBackend parseEngine(String engine) {
        if (engine == null || engine.isBlank()) return null;
        return switch (engine.strip().toUpperCase()) {
            case "DUCKDUCKGO", "DDG" -> SearchBackend.DUCKDUCKGO;
            case "BING" -> SearchBackend.BING;
            default -> null;
        };
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

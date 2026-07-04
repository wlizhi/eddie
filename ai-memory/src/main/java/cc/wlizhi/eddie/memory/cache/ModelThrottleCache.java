/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.memory.cache;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.entity.ModelProviderEntity;
import cc.wlizhi.eddie.memory.context.ModelProviderContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型调用节流阀缓存。
 * <p>
 * Key: {@code providerId:modelCode}
 * Value: {@link ThrottleEntry} — 包含最近调用时间戳和最小调用间隔。
 * <p>
 * 启动时从 {@link ModelProviderContext} 读取全量 provider 的 models JSON，
 * 提取每个模型的 {@code id}（model code）和 {@code call_interval_sec} 构建缓存。
 * {@link ModelProviderContext#refresh()} 刷新时会联动触发本缓存刷新。
 * <p>
 * 调用方通过 {@link #checkAndThrottle(Long, String)} 执行节流判断，
 * 如果间隔未到期则阻塞等待，到期后更新最近调用时间并放行。
 *
 * @author Eddie
 * {@code @date} 2026-07-04
 */
@Slf4j
@Component
public class ModelThrottleCache {

    @Resource
    private InitScheduler initScheduler;

    @Resource
    private ModelProviderContext modelProviderContext;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * providerId:modelCode → ThrottleEntry
     */
    private final ConcurrentHashMap<String, ThrottleEntry> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        initScheduler.addTask("ModelThrottleCache", 1500, this::refresh);
    }

    /**
     * 从 {@link ModelProviderContext} 读取所有 provider，解析 models JSON，
     * 提取每个模型的 {@code id}（model code）和 {@code call_interval_sec}，写入缓存。
     */
    public void refresh() {
        try {
            List<ModelProviderEntity> providers = modelProviderContext.listAll();
            int count = 0;
            for (ModelProviderEntity provider : providers) {
                String modelsJson = provider.getModels();
                if (modelsJson == null || modelsJson.isEmpty() || "[]".equals(modelsJson)) {
                    continue;
                }
                try {
                    List<Map<String, Object>> models = objectMapper.readValue(
                            modelsJson, new TypeReference<List<Map<String, Object>>>() {
                            });
                    for (Map<String, Object> model : models) {
                        Object id = model.get("id");
                        Object interval = model.get("call_interval_sec");
                        if (id == null) {
                            continue;
                        }
                        String key = provider.getId() + ":" + id;
                        int sec = 0;
                        if (interval instanceof Number) {
                            sec = ((Number) interval).intValue();
                        }
                        cache.put(key, new ThrottleEntry(sec));
                        count++;
                    }
                } catch (Exception e) {
                    log.warn("解析 provider={} models JSON 失败", provider.getId(), e);
                }
            }
            log.info("ModelThrottleCache 刷新完成，共 {} 条节流规则", count);
        } catch (Exception e) {
            log.error("ModelThrottleCache 刷新异常", e);
        }
    }

    /**
     * 节流阀核心方法。
     * <p>
     * <ul>
     *   <li>如果未配置间隔（{@code callIntervalSec <= 0}），直接放行</li>
     *   <li>如果距离上次调用时间不足间隔，{@link Thread#sleep(long)} 阻塞等待</li>
     *   <li>等待结束后更新最近调用时间戳并返回</li>
     * </ul>
     *
     * @param providerId 模型服务商 ID
     * @param modelCode  模型 code
     */
    public void checkAndThrottle(Long providerId, String modelCode) {
        String key = providerId + ":" + modelCode;
        ThrottleEntry entry = cache.get(key);
        if (entry == null || entry.callIntervalSec <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long expectedNextCall = entry.lastCallTimestamp + (entry.callIntervalSec * 1000L);

        if (now < expectedNextCall) {
            long waitMs = expectedNextCall - now;
            log.debug("模型 {}/{} 节流等待 {}ms", providerId, modelCode, waitMs);
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        entry.lastCallTimestamp = System.currentTimeMillis();
    }

    /**
     * 节流条目
     */
    static class ThrottleEntry {
        /**
         * 最近一次调用放行的时间戳（毫秒），volatile 保证多线程可见性
         */
        volatile long lastCallTimestamp;
        /**
         * 最小调用间隔（秒），0 表示不限制
         */
        volatile int callIntervalSec;

        ThrottleEntry(int callIntervalSec) {
            this.lastCallTimestamp = 0L;
            this.callIntervalSec = callIntervalSec;
        }
    }
}

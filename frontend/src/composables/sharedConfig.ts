/**
 * @author Eddie
 * {@code @date} 2026-07-09
 *
 * 共享配置加载层
 *
 * 解决页面初始化时多个 composable 并发调用 fetchConfigs() 导致的重复请求问题。
 * 使用 Promise 缓存在请求 in-flight 期间做去重，请求完成后立即释放，不留长期缓存。
 */
import {fetchConfigs} from '@/api/settings'

let configPromise: Promise<Record<string, string>> | null = null

/**
 * 加载全局配置（请求去重）
 *
 * 并发调用时共享同一个 in-flight Promise，只发一次 HTTP 请求。
 * Promise resolve/reject 后自动清除缓存，下次调用重新请求。
 */
export function loadSharedConfigs(): Promise<Record<string, string>> {
    if (!configPromise) {
        configPromise = fetchConfigs().finally(() => {
            configPromise = null
        })
    }
    return configPromise
}

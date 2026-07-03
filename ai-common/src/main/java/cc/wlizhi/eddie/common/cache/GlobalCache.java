package cc.wlizhi.eddie.common.cache;

/**
 * 系统全局缓存
 *
 * @author Eddie
 */
public interface GlobalCache extends EddieCache {

    void refresh();
}

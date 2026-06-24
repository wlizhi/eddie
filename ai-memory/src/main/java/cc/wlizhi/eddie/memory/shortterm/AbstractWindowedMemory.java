package cc.wlizhi.eddie.memory.shortterm;

import org.springframework.ai.chat.messages.Message;
import org.springframework.lang.NonNull;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于窗口的短期记忆抽象实现
 * <p>
 * 提供三层淘汰策略：
 * <ol>
 *   <li><b>TTL 懒淘汰</b> — {@code add()} 时扫描并移除超过 {@link #TTL_MILLIS} 未活动的会话</li>
 *   <li><b>轮次裁剪</b> — 每会话按 {@link #resolveMaxRounds(String)} 返回值保留最近 N 轮</li>
 *   <li><b>LRU 容量淘汰</b> — 全局会话数超出 {@link #MAX_CONVERSATIONS} 时淘汰最久未访问的</li>
 *   <li><b>内存容量淘汰</b> — 缓存总字符数超出 {@link #MAX_TOTAL_CHARS} 时淘汰最久未访问的</li>
 * </ol>
 * <p>
 * 线程安全：使用 {@link ReentrantLock} 保护所有操作。
 * 子类需实现 {@link #resolveMaxRounds(String)} 提供动态轮次配置。
 */
public abstract class AbstractWindowedMemory implements ShortTermMemory {

    // ======================== 常量 ========================

    /**
     * 全局最大缓存会话数
     */
    protected static final int MAX_CONVERSATIONS = 100;

    /**
     * 会话无活动超时时间（毫秒）
     */
    protected static final long TTL_MILLIS = 30 * 60 * 1000L;

    /**
     * maxRounds 缓存刷新间隔（毫秒），get() 时检查，超过此间隔重新解析
     */
    private static final long REFRESH_INTERVAL = 60 * 1000L;

    /**
     * 全局消息总字符数上限（所有会话累计），超出后淘汰最久未使用的会话
     */
    private static final long MAX_TOTAL_CHARS = 50 * 1024 * 1024L;

    // ======================== 内部结构 ========================

    /**
     * 会话消息条目
     */
    protected static class ConversationEntry {
        final List<Message> messages = new ArrayList<>();
        int maxRounds;
        long totalChars;
        long lastResolvedTime;
        long lastAccessTime;

        ConversationEntry(int maxRounds) {
            this.maxRounds = maxRounds;
            this.lastResolvedTime = System.currentTimeMillis();
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    /**
     * LinkedHashMap — accessOrder=true，get() 自动将访问节点移至尾部
     */
    private final Map<String, ConversationEntry> store = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ConversationEntry> eldest) {
            return size() > MAX_CONVERSATIONS;
        }
    };

    private final ReentrantLock lock = new ReentrantLock();

    // ======================== 子类扩展点 ========================

    /**
     * 解析指定会话的记忆轮数
     *
     * @param conversationId 会话 ID
     * @return 记忆轮数（每轮 = user + assistant 两条消息）；{@code 0} 表示无记忆（不保留任何历史消息）
     */
    protected abstract int resolveMaxRounds(String conversationId);

    // ======================== ShortTermMemory 接口 ========================

    @Override
    public int getRoundsForConversation(String conversationId) {
        lock.lock();
        try {
            return resolveMaxRounds(conversationId);
        } finally {
            lock.unlock();
        }
    }

    // ======================== ChatMemory 接口 ========================

    @Override
    public void add(@NonNull String conversationId, @NonNull List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }

        lock.lock();
        try {
            // 1. TTL 懒淘汰：清理过期会话
            evictExpired();

            // 2. 获取或创建条目（创建时首次解析 maxRounds）
            ConversationEntry entry = store.computeIfAbsent(conversationId,
                    k -> new ConversationEntry(resolveMaxRounds(k)));

            // 3. 追加消息并累计字符数
            for (Message msg : messages) {
                String text = msg.getText();
                if (text != null) {
                    entry.totalChars += text.length();
                }
            }
            entry.messages.addAll(messages);
            entry.lastAccessTime = System.currentTimeMillis();

            // 4. 按缓存的 maxRounds 裁剪，不重新查库
            trimToRounds(entry, entry.maxRounds);

            // 5. 全局字符数淘汰（超出 MAX_TOTAL_CHARS 时淘汰最久未访问的会话）
            evictBySize();

            // 6. LRU 容量淘汰由 LinkedHashMap.removeEldestEntry 自动触发

        } finally {
            lock.unlock();
        }
    }

    @Override
    @NonNull
    public List<Message> get(@NonNull String conversationId) {
        lock.lock();
        try {
            ConversationEntry entry = store.get(conversationId);
            if (entry == null) {
                return List.of();
            }
            // accessOrder=true，get() 已将 entry 移至链表尾部

            // 周期性刷新 maxRounds 缓存，配置变更后最快 REFRESH_INTERVAL 内生效
            if (System.currentTimeMillis() - entry.lastResolvedTime > REFRESH_INTERVAL) {
                entry.maxRounds = resolveMaxRounds(conversationId);
                entry.lastResolvedTime = System.currentTimeMillis();
            }

            // 返回副本避免并发迭代问题
            return new ArrayList<>(entry.messages);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear(@NonNull String conversationId) {
        lock.lock();
        try {
            store.remove(conversationId);
        } finally {
            lock.unlock();
        }
    }

    // ======================== 内部淘汰逻辑 ========================

    /**
     * TTL 懒淘汰 — 移除超过 {@link #TTL_MILLIS} 未活动的会话
     */
    private void evictExpired() {
        long now = System.currentTimeMillis();
        store.values().removeIf(entry -> now - entry.lastAccessTime > TTL_MILLIS);
    }

    /**
     * 将会话消息裁剪到指定的轮次上限
     *
     * @param entry     会话条目
     * @param maxRounds 最大轮数；{@code 0} 表示清空全部消息（无记忆）
     */
    private void trimToRounds(ConversationEntry entry, int maxRounds) {
        List<Message> list = entry.messages;
        if (maxRounds <= 0) {
            entry.totalChars = 0;
            list.clear();
            return;
        }
        int maxMessages = maxRounds * 2;
        if (list.size() > maxMessages) {
            List<Message> removed = list.subList(0, list.size() - maxMessages);
            for (Message msg : removed) {
                String text = msg.getText();
                if (text != null) {
                    entry.totalChars -= text.length();
                }
            }
            removed.clear();
        }
    }

    private void evictBySize() {
        long total = store.values().stream()
                .mapToLong(e -> e.totalChars)
                .sum();
        if (total <= MAX_TOTAL_CHARS) return;

        List<String> keys = store.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(
                        Comparator.comparingLong(e -> e.lastAccessTime)))
                .map(Map.Entry::getKey)
                .toList();

        for (String key : keys) {
            if (total <= MAX_TOTAL_CHARS) break;
            ConversationEntry entry = store.remove(key);
            if (entry != null) {
                total -= entry.totalChars;
            }
        }
    }
}

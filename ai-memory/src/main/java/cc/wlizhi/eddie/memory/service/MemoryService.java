/**
 * @author Eddie
 * {@code @date} 2026-06-20
 */

package cc.wlizhi.eddie.memory.service;

import org.springframework.stereotype.Service;

/**
 * 三层记忆服务（骨架）
 * 后端内部模块，被 ai-chat 和 ai-agent 异步调用
 */
@Service
public class MemoryService {

    public String hello() {
        return "Hello from Memory";
    }
}

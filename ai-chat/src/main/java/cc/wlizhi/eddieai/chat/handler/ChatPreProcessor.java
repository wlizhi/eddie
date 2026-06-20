/**
 * ChatPreProcessor — 聊天请求预处理器
 * <p>
 * 扩展点：在聊天请求执行前进行预处理。
 * 职责包括但不限于：
 * - 请求参数校验
 * - 查询 ModelProvider 信息
 * - 查询 Assistant 获取 SystemPrompt（TODO: 后续实现）
 * - DTO 转换和数据准备
 * <p>
 * 所有处理结果写入 {@link cc.wlizhi.eddieai.chat.entity.dto.ChatContext}，
 * 后续阶段通过 Context 读取。
 * <p>
 * 新增预处理器时只需实现此接口，注册为 Spring Bean 即可。
 * 多个 ChatPreProcessor 会按顺序依次执行。
 */
package cc.wlizhi.eddieai.chat.handler;

import cc.wlizhi.eddieai.chat.entity.dto.ChatContext;

@FunctionalInterface
public interface ChatPreProcessor {

    /**
     * 执行预处理
     *
     * @param ctx 聊天上下文，处理结果写入此对象
     */
    void process(ChatContext ctx);
}

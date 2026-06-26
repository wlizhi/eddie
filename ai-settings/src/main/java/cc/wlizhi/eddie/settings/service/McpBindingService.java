package cc.wlizhi.eddie.settings.service;

import cc.wlizhi.eddie.common.enums.RoleType;

/**
 * MCP 绑定关系管理（Owner ↔ MCP 维度）
 * <p>
 * 以 MCP Server 为粒度，对指定 Owner（助手/智能体）进行绑定关系的新增或移除。
 * 每次操作只处理一个 MCP Server 下的所有工具。
 */
public interface McpBindingService {

    /**
     * 新增绑定：将指定 MCP Server 下的所有工具绑定给 Owner
     * <p>
     * 处理流程：
     * <ol>
     *   <li>从缓存校验 MCP Server 存在性</li>
     *   <li>从缓存获取该 MCP 下的工具列表</li>
     *   <li>工具列表为空则直接返回</li>
     *   <li>批量插入绑定记录（enabled=1）</li>
     *   <li>刷新 OwnerToolBindingContext 缓存</li>
     * </ol>
     *
     * @param ownerType   归属方类型（ASSISTANT / AGENT）
     * @param ownerId     归属方 ID
     * @param mcpServerId MCP Server ID
     */
    void addBinding(RoleType ownerType, Long ownerId, Long mcpServerId);

    /**
     * 移除绑定：将指定 MCP Server 下的所有工具从 Owner 解绑
     * <p>
     * 处理流程：
     * <ol>
     *   <li>从缓存获取该 MCP 下的工具列表</li>
     *   <li>工具列表为空则直接返回</li>
     *   <li>按 toolId 列表删除绑定记录</li>
     *   <li>刷新 OwnerToolBindingContext 缓存</li>
     * </ol>
     *
     * @param ownerType   归属方类型（ASSISTANT / AGENT）
     * @param ownerId     归属方 ID
     * @param mcpServerId MCP Server ID
     */
    void removeBinding(RoleType ownerType, Long ownerId, Long mcpServerId);
}

/**
 * @author Eddie
 * {@code @date} 2026-06-29
 */

package cc.wlizhi.eddie.tools.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;

/**
 * 传输异常类型判断，用于区分"连接断开"和"业务逻辑错误"
 */
enum TransportException {
    ;

    static boolean isTransportError(Throwable e) {
        if (e instanceof java.io.IOException) return true;
        if (e instanceof java.net.ConnectException) return true;
        if (e.getCause() instanceof java.io.IOException) return true;
        if (e.getMessage() != null) {
            String msg = e.getMessage().toLowerCase();
            return msg.contains("pipe") || msg.contains("broken")
                    || msg.contains("connection") || msg.contains("timeout")
                    || msg.contains("closed") || msg.contains("refused");
        }
        return false;
    }
}

/**
 * MCP 远程工具 → Spring AI {@link ToolCallback} 包装
 * <p>
 * 将 MCP 协议中的 {@link McpSchema.Tool} 包装为 Spring AI 可识别的回调，
 * 当 LLM 调用工具时，通过持有的 {@code McpSyncClient} 转发到远程 MCP Server。
 * <p>
 * <b>名称隔离机制</b>：注册给 AI 模型时使用「限定名（qualifiedName）」，
 * 格式为 {@code {mcpServerId}|{原始工具名}}，确保不同 MCP 服务的同名工具不冲突。
 * 调用 MCP 协议时仍使用原始工具名。
 */
@Slf4j
public class McpToolCallback implements ToolCallback {

    /**
     * 限定名分隔符
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final io.modelcontextprotocol.client.McpSyncClient mcpClient;
    private final ToolDefinition toolDefinition;
    /**
     * -- GETTER --
     * 获取所属 MCP 服务器 ID
     */
    @Getter
    private final Long mcpServerId;
    /**
     * -- GETTER --
     * 获取 MCP 协议中的原始工具名（非限定名）
     */
    @Getter
    private final String originalToolName;

    /**
     * 断开通知器：当检测到传输层异常时调用，用于触发 {@code McpClientHolder} 的重连
     */
    private final Runnable disconnectNotifier;

    public McpToolCallback(io.modelcontextprotocol.client.McpSyncClient mcpClient, McpSchema.Tool tool,
                           Long mcpServerId, Runnable disconnectNotifier) throws JsonProcessingException {
        this.mcpClient = mcpClient;
        this.mcpServerId = mcpServerId;
        this.originalToolName = tool.name();
        this.disconnectNotifier = disconnectNotifier;

        // 将 McpSchema.Tool 的 inputSchema 转为 JSON 字符串
        String inputSchemaJson = tool.inputSchema() != null ? objectMapper.writeValueAsString(tool.inputSchema()) : "{}";

        this.toolDefinition = ToolDefinition.builder()
                .name(originalToolName)
                .description(tool.description() != null ? tool.description() : "")
                .inputSchema(inputSchemaJson)
                .build();
    }

    @Override
    public @NonNull ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        log.debug("MCP 工具调用: qualifiedName={}, originalName={}", toolDefinition.name(), originalToolName);
        try {
            // toolInput 是 JSON 字符串 → 解析为 Map<String, Object>
            Map<String, Object> arguments = objectMapper.readValue(
                    toolInput, new TypeReference<Map<String, Object>>() {
                    });

            McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder(originalToolName)
                    .arguments(arguments)
                    .build();

            McpSchema.CallToolResult result = mcpClient.callTool(request);

            return serializeResult(result);
        } catch (Exception e) {
            log.error("MCP 工具调用失败: {} (original={}) - {}", toolDefinition.name(), originalToolName, e.getMessage(), e);
            // 检测到传输层异常 → 通知 Holder 断开并触发重连
            if (TransportException.isTransportError(e)) {
                log.warn("MCP 连接检测到传输异常，触发断开重连: {} (mcpServerId={})",
                        originalToolName, mcpServerId);
                disconnectNotifier.run();
            }
            throw new RuntimeException("MCP 工具调用失败: " + originalToolName, e);
        }
    }

    /**
     * 将 CallToolResult 序列化为 JSON 字符串
     */
    private String serializeResult(McpSchema.CallToolResult result) {
        try {
            ObjectNode root = objectMapper.createObjectNode();

            // content 数组
            ArrayNode contentArray = objectMapper.createArrayNode();
            List<McpSchema.Content> contents = result.content();
            if (contents != null) {
                for (McpSchema.Content content : contents) {
                    ObjectNode item = objectMapper.createObjectNode();
                    item.put("type", content.type());
                    if (content instanceof McpSchema.TextContent textContent) {
                        item.put("text", textContent.text());
                    } else if (content instanceof McpSchema.ImageContent imageContent) {
                        item.put("mimeType", imageContent.mimeType());
                        item.put("data", imageContent.data());
                    } else if (content instanceof McpSchema.EmbeddedResource embeddedResource) {
                        item.put("resource", embeddedResource.resource().toString());
                    }
                    contentArray.add(item);
                }
            }
            root.set("content", contentArray);
            root.put("isError", result.isError() != null && result.isError());

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("序列化 CallToolResult 失败", e);
            return "{\"error\":\"序列化工具结果失败\"}";
        }
    }

    public McpToolCallback cloneForNewName(String name) {
        try {
            McpSchema.Tool tool = McpSchema.Tool.builder(name, objectMapper.readValue(this.toolDefinition.inputSchema(), new TypeReference<>() {
            })).build();
            return new McpToolCallback(this.mcpClient, tool, this.mcpServerId, this.disconnectNotifier);
        } catch (JsonProcessingException e) {
            return this;
        }
    }
}

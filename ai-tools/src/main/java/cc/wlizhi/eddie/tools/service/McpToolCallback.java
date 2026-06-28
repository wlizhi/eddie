package cc.wlizhi.eddie.tools.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;

/**
 * MCP 远程工具 → Spring AI {@link ToolCallback} 包装
 * <p>
 * 将 MCP 协议中的 {@link McpSchema.Tool} 包装为 Spring AI 可识别的回调，
 * 当 LLM 调用工具时，通过持有的 {@code McpSyncClient} 转发到远程 MCP Server。
 */
@Slf4j
public class McpToolCallback implements ToolCallback {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final io.modelcontextprotocol.client.McpSyncClient mcpClient;
    private final ToolDefinition toolDefinition;
    private final String mcpToolName;

    public McpToolCallback(io.modelcontextprotocol.client.McpSyncClient mcpClient, McpSchema.Tool tool) {
        this.mcpClient = mcpClient;
        this.mcpToolName = tool.name();

        // 将 McpSchema.Tool 的 inputSchema 转为 JSON 字符串
        String inputSchemaJson = tool.inputSchema() != null ? tool.inputSchema().toString() : "{}";

        this.toolDefinition = ToolDefinition.builder()
                .name(tool.name())
                .description(tool.description() != null ? tool.description() : "")
                .inputSchema(inputSchemaJson)
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        log.debug("MCP 工具调用: name={}, arguments={}", mcpToolName, toolInput);
        try {
            // toolInput 是 JSON 字符串 → 解析为 Map<String, Object>
            Map<String, Object> arguments = objectMapper.readValue(
                    toolInput, new TypeReference<Map<String, Object>>() {
                    });

            McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder(mcpToolName)
                    .arguments(arguments)
                    .build();

            McpSchema.CallToolResult result = mcpClient.callTool(request);

            return serializeResult(result);
        } catch (Exception e) {
            log.error("MCP 工具调用失败: {} - {}", mcpToolName, e.getMessage(), e);
            throw new RuntimeException("MCP 工具调用失败: " + mcpToolName, e);
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
}

/**
 * @author Eddie
 * {@code @date} 2026-07-10
 */

package cc.wlizhi.eddie.common.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 将换行分隔的文本或 JSON 数组反序列化为 {@code List<String>}。<p>
 * 兼容前端 textarea 控件保存的纯文本格式（换行分隔）和标准的 JSON 数组格式。
 * 用于 {@link ShellToolConfig#blacklist} 和 {@link ShellToolConfig#whitelist} 字段的兜底反序列化。
 *
 * @author Eddie
 */
public class NewlineStringToListDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();

        // 标准 JSON 数组格式：["rm", "dd", "mkfs"]
        if (token == JsonToken.START_ARRAY) {
            List<String> result = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                result.add(p.getValueAsString());
            }
            return result;
        }

        // textarea 保存的换行分隔文本格式："rm\ndd\nmkfs"
        if (token == JsonToken.VALUE_STRING) {
            String text = p.getValueAsString();
            if (text == null || text.isBlank()) {
                return List.of();
            }
            return Arrays.stream(text.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        return List.of();
    }
}

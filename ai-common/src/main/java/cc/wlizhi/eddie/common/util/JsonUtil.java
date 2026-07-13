package cc.wlizhi.eddie.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String unwrapJsonString(String text) {
        if (text == null || !text.startsWith("\"")) {
            return text; // 不是 JSON 字符串，原样返回
        }
        try {
            // 尝试解析为 JSON 字符串
            Object parsed = objectMapper.readValue(text, Object.class);
            if (parsed instanceof String) {
                return (String) parsed; // 是 JSON 字符串 → 解包
            }
        } catch (Exception ignored) {
            // 解析失败 → 不是 JSON，原样返回
        }
        return text;
    }
}

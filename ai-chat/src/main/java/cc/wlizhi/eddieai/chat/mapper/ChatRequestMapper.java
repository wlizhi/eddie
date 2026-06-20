package cc.wlizhi.eddieai.chat.mapper;

import cc.wlizhi.eddieai.chat.entity.dto.ChatClientGetDTO;
import cc.wlizhi.eddieai.chat.entity.request.ChatRequest;
import org.mapstruct.Mapper;

/**
 * ChatRequest → ChatClientGetDTO 映射器
 * <p>
 * MapStruct 编译期生成纯 setter/getter 调用代码，零反射，兼容 GraalVM AOT 编译。
 */
@Mapper(componentModel = "spring")
public interface ChatRequestMapper {

    ChatClientGetDTO toDto(ChatRequest request);
}

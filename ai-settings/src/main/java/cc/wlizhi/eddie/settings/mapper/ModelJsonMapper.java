/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.settings.mapper;

import cc.wlizhi.eddie.common.entity.dto.ModelJsonItem;
import cc.wlizhi.eddie.settings.entity.response.ModelVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * ModelJsonItem（JSON 存储格式） → ModelVO（API 响应格式）转换
 * <p>
 * capabilities 在 ModelJsonItem 中为 {@code List<String>}（原始 JSON），
 * 在 ModelVO 中为 {@code List<ModelCapability>}，由 Service 层 {@code ModelCapabilityResolver} 统一处理，
 * 因此 Mapper 中忽略该字段。
 */
@Mapper(componentModel = "spring")
public interface ModelJsonMapper {

    @Mapping(source = "id", target = "code")
    @Mapping(target = "capabilities", ignore = true)
    @Mapping(target = "created", ignore = true)
    ModelVO toVo(ModelJsonItem item);

    @Mapping(target = "created", ignore = true)
    List<ModelVO> toVoList(List<ModelJsonItem> items);
}

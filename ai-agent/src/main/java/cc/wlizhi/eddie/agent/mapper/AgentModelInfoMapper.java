/**
 * @author Eddie
 * {@code @date} 2026-07-05
 */

package cc.wlizhi.eddie.agent.mapper;

import cc.wlizhi.eddie.agent.entity.dto.AgentModelInfo;
import cc.wlizhi.eddie.common.entity.dto.ModelJsonItem;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * AgentModelInfo（模型调用统计信息） ↔ ModelJsonItem（模型存储配置项）转换
 * <p>
 * 两个类的字段名完全一致，MapStruct 自动映射，无需额外配置。
 */
@Mapper(componentModel = "spring")
public interface AgentModelInfoMapper {

    /**
     * AgentModelInfo → ModelJsonItem
     */
    ModelJsonItem toModelJsonItem(AgentModelInfo source);

    /**
     * ModelJsonItem → AgentModelInfo
     */
    AgentModelInfo toAgentModelInfo(ModelJsonItem source);

    /**
     * List<AgentModelInfo> → List<ModelJsonItem>
     */
    List<ModelJsonItem> toModelJsonItemList(List<AgentModelInfo> sources);

    /**
     * List<ModelJsonItem> → List<AgentModelInfo>
     */
    List<AgentModelInfo> toAgentModelInfoList(List<ModelJsonItem> sources);
}

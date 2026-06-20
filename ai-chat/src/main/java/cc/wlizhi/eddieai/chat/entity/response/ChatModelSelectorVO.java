package cc.wlizhi.eddieai.chat.entity.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 聊天模型选择器分组响应 VO
 * 按供应商实例分组，供应商不可选中，models 为可选用的模型列表
 */
@Getter
@Setter
public class ChatModelSelectorVO {

    /**
     * 供应商实例 ID，选中模型时需携带
     */
    private Long providerId;

    /**
     * 供应商 code，仅作分组标识，不可选中
     */
    private String providerCode;

    /**
     * 供应商名称，分组头展示（前端用浅色/灰色）
     */
    private String providerName;

    /**
     * 该供应商下可选用的模型列表
     */
    private List<ChatModelItemVO> models;
}

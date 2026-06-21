/**
 * 模型参数定义（静态配置）
 *
 * 来源：AssistantDialog.vue modelParamDefs
 */
export interface ModelParamDef {
    key: string
    label: string
    tip: string
    step: number
    min: number
    max?: number
}

export const MODEL_PARAM_DEFS: ModelParamDef[] = [
    {
        key: 'temperature',
        label: 'Temperature',
        tip: '控制回答的随机性。越高越有创造力，越低越保守准确。范围 0~2，推荐 0.5~1.2',
        step: 0.1,
        min: 0,
        max: 2
    },
    {
        key: 'maxTokens',
        label: 'Max Tokens',
        tip: '单次回答的最大长度。越大可生成越长内容，但更耗资源。推荐 1024~4096',
        step: 1,
        min: 1
    },
    {
        key: 'topP',
        label: 'Top P',
        tip: '候选词筛选阈值。值越小回答越保守，通常配合 Temperature 使用。范围 0~1',
        step: 0.1,
        min: 0,
        max: 1
    },
    {
        key: 'frequencyPenalty',
        label: 'Frequency Penalty',
        tip: '减少词语重复。值越大越避免重复已有词汇。范围 -2~2，推荐 0~1',
        step: 0.1,
        min: -2,
        max: 2
    },
    {
        key: 'presencePenalty',
        label: 'Presence Penalty',
        tip: '鼓励谈论新话题。值越大越倾向讨论不同内容。范围 -2~2，推荐 0~1',
        step: 0.1,
        min: -2,
        max: 2
    },
]

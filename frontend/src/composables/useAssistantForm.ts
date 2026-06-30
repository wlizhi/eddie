/**
 * @author Eddie
 * @date 2026-06-21
 */

/**
 * useAssistantForm — 助手表单状态管理与业务逻辑
 *
 * 来源：AssistantDialog.vue script 部分
 * 提取后组件只保留 glue code + template + style
 */
import {computed, reactive, ref, watch} from 'vue'
import {useDialog} from 'naive-ui'
import {useAssistantStore} from '@/stores/assistant'
import {useChatStore} from '@/stores/chat'
import type {McpServerItem} from '@/api/assistant'
import {fetchAssistantDetail, fetchEnabledMcpServers, updateAssistantAvatar} from '@/api/assistant'
import type {AssistantDetailVO, AssistantPreferences} from '@/types/assistant'
import {fetchConfigs} from '@/api/settings'
import {MODEL_PARAM_DEFS} from '@/constants/modelParams'
import {showToast} from '@/composables/useToast'

export function useAssistantForm(
    props: {
        assistantId: number | null
        createVisible?: boolean
    },
    emit: {
        (e: 'update:assistantId', value: number | null): void
        (e: 'update:createVisible', value: boolean): void
    }
) {
    const assistantStore = useAssistantStore()
    const chatStore = useChatStore()
    const dialog = useDialog()

    /** 是否为创建模式 */
    const isCreateMode = computed(() => props.createVisible === true && props.assistantId === null)

    const detail = ref<AssistantDetailVO | null>(null)
    const saving = ref(false)

    /** 字段级错误状态 */
    const fieldErrors = reactive<Record<string, string>>({})

    function clearFieldError(field: string) {
        delete fieldErrors[field]
    }

    // ========== 表单数据 ==========
    const formName = ref('')
    const formAvatar = ref('')
    const formDescription = ref('')
    const formSystemPrompt = ref('')
    const formProviderId = ref<number | null>(null)
    const formModelId = ref('')
    const formMemoryRounds = ref(10)
    const formEnabled = ref<number>(1)

    // ========== MCP 工具选择 ==========
    const formEnabledMcpServerIds = ref<number[]>([])
    const mcpServerList = ref<McpServerItem[]>([])

    const formModelParams = reactive<Record<string, any>>(
        Object.fromEntries(MODEL_PARAM_DEFS.map(d => [d.key, d.componentType === 'select' ? 'auto' : null]))
    )

    /** 助手偏好设置（UI 默认状态） */
    const formPreferences = reactive<AssistantPreferences>({
        webSearchEnabled: false,
        mcpToolMode: 'auto',
    })

    const show = ref(false)
    const showPicker = ref(false)
    const originalAvatar = ref('')
    const pendingAvatarFile = ref<File | null>(null)

    /** System Prompt textarea 的 DOM 引用（用于光标位置插入变量） */
    const systemPromptRef = ref<HTMLTextAreaElement | null>(null)

    // ========== Watchers ==========
    watch(() => props.assistantId, async (id) => {
        if (id === null) {
            if (!props.createVisible) {
                show.value = false
            }
            return
        }
        if (chatStore.modelSelectors.length === 0) {
            await chatStore.loadModels()
        }
        await Promise.all([
            loadDetail(id),
            loadMcpServerList(),
        ])
        show.value = true
    })

    watch(() => props.createVisible, async (visible) => {
        if (visible) {
            resetFormForCreate()
            if (chatStore.modelSelectors.length === 0) {
                await chatStore.loadModels()
            }
            // 加载已启用的 MCP 列表
            await loadMcpServerList()
            // 检测全局默认模型并自动选中
            await tryAutoSelectDefaultModel()
            show.value = true
        } else if (props.assistantId === null) {
            show.value = false
        }
    })

    // ========== 表单操作 ==========
    function resetFormForCreate() {
        detail.value = null
        formName.value = ''
        formAvatar.value = ''
        originalAvatar.value = ''
        formDescription.value = ''
        formSystemPrompt.value = ''
        formProviderId.value = null
        formModelId.value = ''
        formMemoryRounds.value = 10
        formEnabled.value = 1
        formEnabledMcpServerIds.value = []
        formPreferences.webSearchEnabled = false
        formPreferences.mcpToolMode = 'auto'
        for (const def of MODEL_PARAM_DEFS) {
            formModelParams[def.key] = def.componentType === 'select' ? 'auto' : null
        }
        pendingAvatarFile.value = null
        for (const key of Object.keys(fieldErrors)) {
            delete fieldErrors[key]
        }
    }

    /** 加载已启用的 MCP 服务列表 */
    async function loadMcpServerList() {
        try {
            mcpServerList.value = await fetchEnabledMcpServers()
        } catch (err) {
            console.error('加载 MCP 列表失败:', err)
            mcpServerList.value = []
        }
    }

    async function loadDetail(id: number) {
        try {
            const d = await fetchAssistantDetail(id)
            detail.value = d
            formName.value = d.name
            formAvatar.value = d.avatar ?? ''
            originalAvatar.value = d.avatar ?? ''
            formDescription.value = d.description ?? ''
            formSystemPrompt.value = d.systemPrompt ?? ''
            formProviderId.value = d.providerId
            formModelId.value = d.modelId
            formMemoryRounds.value = d.memoryRounds ?? 20
            formEnabled.value = d.enabled === true || d.enabled === 1 ? 1 : 0
            formEnabledMcpServerIds.value = d.boundMcpServerIds ?? []
            const mp = d.modelParams || {}
            for (const def of MODEL_PARAM_DEFS) {
                formModelParams[def.key] = (mp as any)[def.key] ?? null
            }
            // 加载助手偏好设置
            const prefs = d.preferences ?? {}
            formPreferences.webSearchEnabled = prefs.webSearchEnabled ?? false
            formPreferences.mcpToolMode = prefs.mcpToolMode ?? 'auto'
            pendingAvatarFile.value = null
        } catch (err) {
            showToast('加载助手详情失败', 'error')
            console.error(err)
            setTimeout(() => close(), 1500)
        }
    }

    // ========== 模型选择 ==========
    /** 选项唯一标识：服务商ID:模型ID，防止不同服务商同编号模型冲突 */
    const MODEL_KEY_SEPARATOR = '::'

    const groupedModelOptions = computed(() =>
        chatStore.modelSelectors.map((s) => ({
            type: 'group' as const,
            label: s.providerName,
            key: s.providerCode,
            children: s.models.map((m) => ({
                label: m.displayName ?? m.modelId,
                value: `${m.providerId}${MODEL_KEY_SEPARATOR}${m.modelId}`,
            })),
        }))
    )

    /** 当前选中项的复合键，用于 NSelect 的 :value 绑定 */
    const selectedModelKey = computed<string | null>(() => {
        if (!formProviderId.value || !formModelId.value) return null
        return `${formProviderId.value}${MODEL_KEY_SEPARATOR}${formModelId.value}`
    })

    function onModelSelect(compositeKey: string | null) {
        if (!compositeKey) return
        const sepIdx = compositeKey.indexOf(MODEL_KEY_SEPARATOR)
        if (sepIdx === -1) return
        const providerId = Number(compositeKey.substring(0, sepIdx))
        const modelId = compositeKey.substring(sepIdx + MODEL_KEY_SEPARATOR.length)
        if (!providerId || !modelId) return
        formProviderId.value = providerId
        formModelId.value = modelId
    }

    /** 新建助手时自动选中全局默认模型（若存在于候选列表中） */
    async function tryAutoSelectDefaultModel() {
        try {
            const configs = await fetchConfigs()
            const raw = configs['DEFAULT_MODEL']
            if (!raw || raw === '{}') return

            const val = JSON.parse(raw)
            const {providerId, modelId, modelParams} = val
            if (!providerId || !modelId) return

            const compositeKey = `${providerId}${MODEL_KEY_SEPARATOR}${modelId}`
            for (const group of groupedModelOptions.value) {
                for (const child of group.children) {
                    if (child.value === compositeKey) {
                        formProviderId.value = providerId
                        formModelId.value = modelId

                        // 自动填充默认模型的参数
                        if (modelParams && typeof modelParams === 'object') {
                            for (const def of MODEL_PARAM_DEFS) {
                                const v = modelParams[def.key]
                                if (v !== undefined && v !== null) {
                                    formModelParams[def.key] = v
                                }
                            }
                        }

                        return
                    }
                }
            }
        } catch {
            // 配置不存在或解析失败，不做处理
        }
    }

    // ========== 头像 ==========
    function onAvatarPicked(value: string | null, file: File | null) {
        if (file) {
            pendingAvatarFile.value = file
            formAvatar.value = URL.createObjectURL(file)
        } else if (value) {
            formAvatar.value = value
            pendingAvatarFile.value = null
        } else {
            // 空白输入 → 清空头像，走默认首字显示
            formAvatar.value = ''
            pendingAvatarFile.value = null
        }
        showPicker.value = false
    }

    /**
     * 在 System Prompt textarea 光标位置插入变量模板
     * 无焦点或 textarea 不可用时，追加到末尾
     */
    function insertVariable(template: string) {
        const ta = systemPromptRef.value
        if (ta) {
            const start = ta.selectionStart
            const end = ta.selectionEnd
            formSystemPrompt.value =
                formSystemPrompt.value.slice(0, start) +
                template +
                formSystemPrompt.value.slice(end)
            // 下一帧将光标定位到插入内容之后并聚焦
            requestAnimationFrame(() => {
                const pos = start + template.length
                ta.setSelectionRange(pos, pos)
                ta.focus()
            })
        } else {
            formSystemPrompt.value += template
        }
    }

    // ========== 保存 ==========
    async function handleSave() {
        for (const key of Object.keys(fieldErrors)) {
            delete fieldErrors[key]
        }

        // 创建模式
        if (isCreateMode.value) {
            let hasError = false
            if (!formName.value.trim()) {
                fieldErrors['formName'] = '请输入助手名称'
                hasError = true
            }
            if (!formProviderId.value) {
                fieldErrors['formModelId'] = '请选择一个模型'
                hasError = true
            }
            if (hasError) return

            saving.value = true
            try {
                const modelParams = buildModelParams()
                const preferences = buildPreferences()

                const created = await assistantStore.create({
                    name: formName.value,
                    avatar: formAvatar.value || undefined,
                    description: formDescription.value || undefined,
                    systemPrompt: formSystemPrompt.value || undefined,
                    providerId: formProviderId.value ?? undefined,
                    modelId: formModelId.value,
                    memoryRounds: formMemoryRounds.value,
                    modelParams: Object.keys(modelParams).length > 0 ? modelParams : undefined,
                    preferences: Object.keys(preferences).length > 0 ? preferences : undefined,
                    enabledMcpServerIds: formEnabledMcpServerIds.value.length > 0 ? formEnabledMcpServerIds.value : undefined,
                })

                if (created) {
                    if (pendingAvatarFile.value) {
                        const fd = new FormData()
                        fd.append('file', pendingAvatarFile.value)
                        await updateAssistantAvatar(created.id, fd)
                    }
                    showToast('创建成功')
                    close()
                } else {
                    showToast('创建失败', 'error')
                }
            } catch (err) {
                showToast('创建失败', 'error')
                console.error(err)
            } finally {
                saving.value = false
            }
            return
        }

        // 编辑模式
        if (!detail.value) return
        if (!formProviderId.value) {
            fieldErrors['formModelId'] = '请选择一个模型'
            return
        }

        saving.value = true
        try {
            if (pendingAvatarFile.value) {
                const fd = new FormData()
                fd.append('file', pendingAvatarFile.value)
                const updated = await updateAssistantAvatar(detail.value.id, fd)
                formAvatar.value = updated.avatar ?? ''
                pendingAvatarFile.value = null
            } else if (formAvatar.value !== originalAvatar.value) {
                const fd = new FormData()
                fd.append('avatar', formAvatar.value)
                await updateAssistantAvatar(detail.value.id, fd)
            }

            const modelParams = buildModelParams()
            const preferences = buildPreferences()

            await assistantStore.update(detail.value.id, {
                name: formName.value,
                avatar: formAvatar.value,
                description: formDescription.value,
                systemPrompt: formSystemPrompt.value,
                providerId: formProviderId.value ?? undefined,
                modelId: formModelId.value || undefined,
                memoryRounds: formMemoryRounds.value,
                enabled: formEnabled.value,
                modelParams: Object.keys(modelParams).length > 0 ? modelParams : undefined,
                preferences: Object.keys(preferences).length > 0 ? preferences : undefined,
                enabledMcpServerIds: formEnabledMcpServerIds.value.length > 0 ? formEnabledMcpServerIds.value : undefined,
            })
            // 同步 chatStore 中的 MCP 绑定和偏好，确保聊天工具栏实时更新
            await chatStore.loadBoundMcpTools(detail.value.id)
            // 联动逻辑：如果 BuiltInSearch 不在绑定列表中，强制关闭联网搜索
            const hasBuiltInSearch = chatStore.boundMcpTools.some(
                t => t.transportType === 'BUILT_IN'
            )
            if (!hasBuiltInSearch) {
                chatStore.webSearchEnabled = false
            } else {
                chatStore.webSearchEnabled = formPreferences.webSearchEnabled ?? false
            }
            chatStore.mcpToolMode = formPreferences.mcpToolMode as 'disabled' | 'auto' | 'manual'
            showToast('保存成功')
            close()
        } catch (err) {
            showToast('保存失败', 'error')
            console.error(err)
        } finally {
            saving.value = false
        }
    }

    function buildModelParams(): Record<string, unknown> {
        const modelParams: Record<string, unknown> = {}
        for (const def of MODEL_PARAM_DEFS) {
            const v = formModelParams[def.key]
            if (v !== null && v !== undefined) modelParams[def.key] = v
        }
        return modelParams
    }

    function buildPreferences(): Record<string, unknown> {
        // 始终返回完整偏好，包括默认值，确保后端能覆盖旧值
        return {
            webSearchEnabled: formPreferences.webSearchEnabled ?? false,
            mcpToolMode: formPreferences.mcpToolMode ?? 'auto',
        }
    }

    async function handleDelete() {
        if (!detail.value) return
        const id = detail.value.id
        const name = formName.value || '此助手'
        dialog.warning({
            title: '删除助手',
            content: `确定删除「${name}」？删除后不可恢复。`,
            positiveText: '确认删除',
            negativeText: '取消',
            onPositiveClick: async () => {
                close()
                await assistantStore.remove(id)
                showToast('已删除')
            },
        })
    }

    function close() {
        show.value = false
        showPicker.value = false
        if (isCreateMode.value) {
            emit('update:createVisible', false)
        }
        emit('update:assistantId', null)
    }

    return {
        // 状态
        show,
        saving,
        fieldErrors,
        isCreateMode,
        showPicker,
        originalAvatar,

        // 表单字段
        formName,
        formAvatar,
        formDescription,
        formSystemPrompt,
        systemPromptRef,
        formProviderId,
        formModelId,
        formMemoryRounds,
        formEnabled,
        formModelParams,
        formPreferences,

        // MCP 工具选择
        formEnabledMcpServerIds,
        mcpServerList,

        // 方法
        clearFieldError,
        loadDetail,
        onModelSelect,
        onAvatarPicked,
        insertVariable,
        handleSave,
        handleDelete,
        close,

        // 计算属性
        groupedModelOptions,
        selectedModelKey,
    }
}

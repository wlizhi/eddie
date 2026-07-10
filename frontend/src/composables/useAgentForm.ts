/**
 * @author Eddie
 * @date 2026-07-04
 */

/**
 * useAgentForm — 智能体表单状态管理与业务逻辑
 *
 * 参照 useAssistantForm，字段按智能体需求调整：
 * - 无 memoryRounds（智能体没有记忆轮数）
 * - 有子代理模型配置（subProviderId + subModelId）
 * - 有执行控制参数（semaphore / maxIterations / maxExecutionTimeSec）
 * - 有 executionMode / toolSelectionMode
 * - mainModelParams / subModelParams 为 JSON 字符串
 */
import {computed, reactive, ref, watch} from 'vue'
import {useDialog} from 'naive-ui'
import {useAgentStore} from '@/stores/agent'
import {useChatStore} from '@/stores/chat'
import {fetchAgentDetail, fetchEnabledMcpServers} from '@/api/agent'
import {fetchConfigs} from '@/api/settings'
import type {AgentDetailVO, AgentMcpServerBinding} from '@/types/agent'
import type {AssistantPreferences} from '@/types/assistant'
import type {McpServerItem} from '@/api/assistant'
import {MODEL_PARAM_DEFS} from '@/constants/modelParams'
import {showToast} from '@/composables/useToast'
import {updateAgentAvatar} from '@/api/agent'

export function useAgentForm(
    props: {
        agentId: number | null
        createVisible?: boolean
    },
    emit: {
        (e: 'update:agentId', value: number | null): void
        (e: 'update:createVisible', value: boolean): void
    }
) {
    const agentStore = useAgentStore()
    const chatStore = useChatStore()
    const dialog = useDialog()

    /** 是否为创建模式 */
    const isCreateMode = computed(() => props.createVisible === true && props.agentId === null)

    const detail = ref<AgentDetailVO | null>(null)
    const saving = ref(false)

    /** 字段级错误状态 */
    const fieldErrors = reactive<Record<string, string>>({})

    function clearFieldError(field: string) {
        delete fieldErrors[field]
    }

    // ========== 表单数据（基本信息） ==========
    const formName = ref('')
    const formAvatar = ref('')
    const formDescription = ref('')
    const formSystemPrompt = ref('')
    const formEnabled = ref<number>(1)

    // ========== 主模型配置 ==========
    const formMainProviderId = ref<number | null>(null)
    const formMainModelId = ref('')

    // ========== 子代理模型配置 ==========
    const formSubProviderId = ref<number | null>(null)
    const formSubModelId = ref('')

    // ========== 执行控制 ==========
    const formSemaphore = ref(1)
    const formMaxIterations = ref(100)
    const formMaxExecutionTimeSec = ref(300)
    const formExecutionMode = ref('FOREGROUND')
    const formToolSelectionMode = ref('auto')

    const formMemoryRounds = ref(20)

    const formMainModelParams = reactive<Record<string, any>>(
        Object.fromEntries(MODEL_PARAM_DEFS.map(d => [d.key, d.componentType === 'select' ? 'auto' : null]))
    )
    const formSubModelParams = reactive<Record<string, any>>(
        Object.fromEntries(MODEL_PARAM_DEFS.map(d => [d.key, d.componentType === 'select' ? 'auto' : null]))
    )

    /** 偏好设置 */
    const formPreferences = reactive<AssistantPreferences>({
        webSearchEnabled: false,
        mcpToolMode: 'auto',
    })

    // ========== MCP 工具选择 ==========
    const formMcpServerBindings = ref<AgentMcpServerBinding[]>([])
    const mcpServerList = ref<McpServerItem[]>([])

    const show = ref(false)
    const showPicker = ref(false)
    const originalAvatar = ref('')
    const pendingAvatarFile = ref<File | null>(null)

    /** System Prompt textarea 的 DOM 引用 */
    const systemPromptRef = ref<HTMLTextAreaElement | null>(null)

    // ========== Watchers ==========
    watch(() => props.agentId, async (id) => {
        if (id === null) {
            if (!props.createVisible) {
                show.value = false
            }
            return
        }
        if (chatStore.modelSelectors.length === 0) {
            await chatStore.loadModels()
        }
        // 先加载 MCP 服务列表（loadDetail 依赖其数据补全 binding）
        await loadMcpServerList()
        await loadDetail(id)
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
            await tryAutoSelectDefaultModel()
            show.value = true
        } else if (props.agentId === null) {
            show.value = false
        }
    })

    // ========== 表单操作 ==========
    function resetFormForCreate() {
        detail.value = null
        formName.value = ''
        formAvatar.value = ''
        originalAvatar.value = ''
        pendingAvatarFile.value = null
        formDescription.value = ''
        formSystemPrompt.value = ''
        formEnabled.value = 1

        formMainProviderId.value = null
        formMainModelId.value = ''
        formSubProviderId.value = null
        formSubModelId.value = ''

        formSemaphore.value = 1
        formMaxIterations.value = 100
        formMaxExecutionTimeSec.value = 300
        formExecutionMode.value = 'FOREGROUND'
        formToolSelectionMode.value = 'auto'

        formMemoryRounds.value = 20

        formMcpServerBindings.value = []

        formPreferences.webSearchEnabled = false
        formPreferences.mcpToolMode = 'auto'

        for (const def of MODEL_PARAM_DEFS) {
            formMainModelParams[def.key] = def.componentType === 'select' ? 'auto' : null
            formSubModelParams[def.key] = def.componentType === 'select' ? 'auto' : null
        }

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
            const d = await fetchAgentDetail(id)
            detail.value = d
            formName.value = d.name
            formAvatar.value = d.avatar ?? ''
            originalAvatar.value = d.avatar ?? ''
            pendingAvatarFile.value = null
            formDescription.value = d.description ?? ''
            formSystemPrompt.value = d.systemPrompt ?? ''
            formEnabled.value = d.enabled === true || d.enabled === 1 ? 1 : 0

            // 主模型
            formMainProviderId.value = d.mainProviderId
            formMainModelId.value = d.mainModelId
            parseModelParams(d.mainModelParams, formMainModelParams)

            // 子代理模型
            formSubProviderId.value = d.subProviderId
            formSubModelId.value = d.subModelId ?? ''
            parseModelParams(d.subModelParams, formSubModelParams)

            // 执行控制
            formSemaphore.value = d.semaphore ?? 1
            formMaxIterations.value = d.maxIterations ?? 100
            formMaxExecutionTimeSec.value = d.maxExecutionTimeSec ?? 300
            formExecutionMode.value = d.executionMode ?? 'FOREGROUND'
            formToolSelectionMode.value = d.toolSelectionMode ?? 'auto'

            formMemoryRounds.value = d.memoryRounds ?? 20

            // MCP 工具绑定 — 补全所有工具（未出现在绑定中的默认为 0=禁用）
            const rawBindings = d.mcpServerBindings ?? []
            formMcpServerBindings.value = mcpServerList.value.map(mcp => ({
                mcpServerId: mcp.id,
                tools: (mcp.tools ?? []).map(tool => {
                    const existing = rawBindings
                        .find(b => b.mcpServerId === mcp.id)
                        ?.tools.find(t => t.toolId === tool.id)
                    return { toolId: tool.id, status: existing?.status ?? 0 }
                }),
            }))

            // 偏好设置
            const prefs = d.preferences ?? {}
            formPreferences.webSearchEnabled = prefs.webSearchEnabled ?? false
            formPreferences.mcpToolMode = prefs.mcpToolMode ?? 'auto'
        } catch (err) {
            showToast('加载智能体详情失败', 'error')
            console.error(err)
            setTimeout(() => close(), 1500)
        }
    }

    /** 解析模型参数字符串到 reactive 对象 */
    function parseModelParams(jsonStr: string | null, target: Record<string, any>) {
        if (!jsonStr || jsonStr === '{}') return
        try {
            const parsed = JSON.parse(jsonStr)
            if (parsed && typeof parsed === 'object') {
                for (const def of MODEL_PARAM_DEFS) {
                    const v = parsed[def.key]
                    if (v !== undefined && v !== null) {
                        target[def.key] = v
                    }
                }
            }
        } catch {
            // 解析失败，保留默认值
        }
    }

    /** 构建模型参数 JSON 字符串 */
    function buildModelParamsJson(source: Record<string, any>): string {
        const params: Record<string, unknown> = {}
        for (const def of MODEL_PARAM_DEFS) {
            const v = source[def.key]
            if (v !== null && v !== undefined) params[def.key] = v
        }
        return Object.keys(params).length > 0 ? JSON.stringify(params) : '{}'
    }

    // ========== 模型选择 ==========
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

    /** 主模型选中项复合键 */
    const selectedMainModelKey = computed<string | null>(() => {
        if (!formMainProviderId.value || !formMainModelId.value) return null
        return `${formMainProviderId.value}${MODEL_KEY_SEPARATOR}${formMainModelId.value}`
    })

    /** 子代理模型选中项复合键 */
    const selectedSubModelKey = computed<string | null>(() => {
        if (!formSubProviderId.value || !formSubModelId.value) return null
        return `${formSubProviderId.value}${MODEL_KEY_SEPARATOR}${formSubModelId.value}`
    })

    function onMainModelSelect(compositeKey: string | null) {
        if (!compositeKey) return
        const sepIdx = compositeKey.indexOf(MODEL_KEY_SEPARATOR)
        if (sepIdx === -1) return
        const providerId = Number(compositeKey.substring(0, sepIdx))
        const modelId = compositeKey.substring(sepIdx + MODEL_KEY_SEPARATOR.length)
        if (!providerId || !modelId) return
        formMainProviderId.value = providerId
        formMainModelId.value = modelId
    }

    function onSubModelSelect(compositeKey: string | null) {
        if (!compositeKey) {
            formSubProviderId.value = null
            formSubModelId.value = ''
            return
        }
        const sepIdx = compositeKey.indexOf(MODEL_KEY_SEPARATOR)
        if (sepIdx === -1) return
        const providerId = Number(compositeKey.substring(0, sepIdx))
        const modelId = compositeKey.substring(sepIdx + MODEL_KEY_SEPARATOR.length)
        if (!providerId || !modelId) return
        formSubProviderId.value = providerId
        formSubModelId.value = modelId
    }

    /** 新建时自动选中全局默认模型（主模型） */
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
                        formMainProviderId.value = providerId
                        formMainModelId.value = modelId

                        if (modelParams && typeof modelParams === 'object') {
                            for (const def of MODEL_PARAM_DEFS) {
                                const v = modelParams[def.key]
                                if (v !== undefined && v !== null) {
                                    formMainModelParams[def.key] = v
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

    /** 在 System Prompt textarea 光标位置插入变量模板 */
    function insertVariable(template: string) {
        const ta = systemPromptRef.value
        if (ta) {
            const start = ta.selectionStart
            const end = ta.selectionEnd
            formSystemPrompt.value =
                formSystemPrompt.value.slice(0, start) +
                template +
                formSystemPrompt.value.slice(end)
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
                fieldErrors['formName'] = '请输入智能体名称'
                hasError = true
            }
            if (!formMainProviderId.value) {
                fieldErrors['formMainModelId'] = '请选择一个主模型'
                hasError = true
            }
            if (hasError) return

            saving.value = true
            try {
                const mainModelParams = buildModelParamsJson(formMainModelParams)
                const subModelParams = buildModelParamsJson(formSubModelParams)
                const preferences = buildPreferences()

                // formMainProviderId 已在前面校验非空
                const created = await agentStore.create({
                    name: formName.value,
                    avatar: formAvatar.value || undefined,
                    description: formDescription.value || undefined,
                    systemPrompt: formSystemPrompt.value || undefined,

                    mainProviderId: formMainProviderId.value!,
                    mainModelId: formMainModelId.value,
                    mainModelParams,

                    subProviderId: formSubProviderId.value ?? undefined,
                    subModelId: formSubModelId.value || undefined,
                    subModelParams: formSubModelId.value ? subModelParams : undefined,

                    semaphore: formSemaphore.value,
                    maxIterations: formMaxIterations.value,
                    maxExecutionTimeSec: formMaxExecutionTimeSec.value,
                    executionMode: formExecutionMode.value,

                    memoryRounds: formMemoryRounds.value,

                    toolSelectionMode: formToolSelectionMode.value,

                    mcpServerBindings: formMcpServerBindings.value.length > 0 ? formMcpServerBindings.value : undefined,

                    preferences: Object.keys(preferences).length > 0 ? preferences : undefined,
                })

                if (created) {
                    if (pendingAvatarFile.value) {
                        const fd = new FormData()
                        fd.append('file', pendingAvatarFile.value)
                        await updateAgentAvatar(created.id, fd)
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
        if (!formMainProviderId.value) {
            fieldErrors['formMainModelId'] = '请选择一个主模型'
            return
        }

        saving.value = true
        try {
            // 先处理头像上传
            if (pendingAvatarFile.value) {
                const fd = new FormData()
                fd.append('file', pendingAvatarFile.value)
                const updated = await updateAgentAvatar(detail.value.id, fd)
                formAvatar.value = updated.avatar ?? ''
                pendingAvatarFile.value = null
            } else if (formAvatar.value !== originalAvatar.value) {
                const fd = new FormData()
                fd.append('avatar', formAvatar.value)
                await updateAgentAvatar(detail.value.id, fd)
            }

            const mainModelParams = buildModelParamsJson(formMainModelParams)
            const subModelParams = buildModelParamsJson(formSubModelParams)
            const preferences = buildPreferences()

            await agentStore.update(detail.value.id, {
                name: formName.value,
                avatar: formAvatar.value,
                description: formDescription.value,
                systemPrompt: formSystemPrompt.value,

                mainProviderId: formMainProviderId.value,
                mainModelId: formMainModelId.value,
                mainModelParams,

                subProviderId: formSubProviderId.value ?? null,
                subModelId: formSubModelId.value || null,
                subModelParams: formSubModelId.value ? subModelParams : null,

                semaphore: formSemaphore.value,
                maxIterations: formMaxIterations.value,
                maxExecutionTimeSec: formMaxExecutionTimeSec.value,
                executionMode: formExecutionMode.value,

                memoryRounds: formMemoryRounds.value,

                toolSelectionMode: formToolSelectionMode.value,

                mcpServerBindings: formMcpServerBindings.value.length > 0 ? formMcpServerBindings.value : undefined,

                preferences: Object.keys(preferences).length > 0 ? preferences : undefined,

                enabled: formEnabled.value,
            })
            showToast('保存成功')
            close()
        } catch (err) {
            showToast('保存失败', 'error')
            console.error(err)
        } finally {
            saving.value = false
        }
    }

    function buildPreferences(): Record<string, unknown> {
        return {
            webSearchEnabled: formPreferences.webSearchEnabled ?? false,
            mcpToolMode: formPreferences.mcpToolMode ?? 'auto',
        }
    }

    async function handleDelete() {
        if (!detail.value) return
        const id = detail.value.id
        const name = formName.value || '此智能体'
        dialog.warning({
            title: '删除智能体',
            content: `确定删除「${name}」？删除后关联的会话和消息也会一并清除，不可恢复。`,
            positiveText: '确认删除',
            negativeText: '取消',
            onPositiveClick: async () => {
                close()
                await agentStore.remove(id)
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
        emit('update:agentId', null)
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
        formEnabled,

        // 主模型
        formMainProviderId,
        formMainModelId,
        formMainModelParams,

        // 子代理模型
        formSubProviderId,
        formSubModelId,
        formSubModelParams,

        // 执行控制
        formSemaphore,
        formMaxIterations,
        formMaxExecutionTimeSec,
        formExecutionMode,
        formToolSelectionMode,

        // 记忆轮数
        formMemoryRounds,

        // 偏好
        formPreferences,

        // MCP 工具选择
        formMcpServerBindings,
        mcpServerList,

        // 方法
        clearFieldError,
        onMainModelSelect,
        onSubModelSelect,
        onAvatarPicked,
        insertVariable,
        handleSave,
        handleDelete,
        close,

        // 计算属性
        groupedModelOptions,
        selectedMainModelKey,
        selectedSubModelKey,
    }
}


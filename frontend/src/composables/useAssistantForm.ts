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
import {fetchAssistantDetail, updateAssistantAvatar} from '@/api/assistant'
import type {AssistantDetailVO} from '@/types/assistant'
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

    const formModelParams = reactive<Record<string, any>>(
        Object.fromEntries(MODEL_PARAM_DEFS.map(d => [d.key, d.componentType === 'select' ? 'auto' : null]))
    )

    const show = ref(false)
    const showPicker = ref(false)
    const originalAvatar = ref('')
    const pendingAvatarFile = ref<File | null>(null)

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
        await loadDetail(id)
        show.value = true
    })

    watch(() => props.createVisible, async (visible) => {
        if (visible) {
            resetFormForCreate()
            if (chatStore.modelSelectors.length === 0) {
                await chatStore.loadModels()
            }
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
        for (const def of MODEL_PARAM_DEFS) {
            formModelParams[def.key] = def.componentType === 'select' ? 'auto' : null
        }
        pendingAvatarFile.value = null
        for (const key of Object.keys(fieldErrors)) {
            delete fieldErrors[key]
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
            const mp = d.modelParams || {}
            for (const def of MODEL_PARAM_DEFS) {
                formModelParams[def.key] = (mp as any)[def.key] ?? null
            }
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
            const {providerId, modelId} = val
            if (!providerId || !modelId) return

            const compositeKey = `${providerId}${MODEL_KEY_SEPARATOR}${modelId}`
            for (const group of groupedModelOptions.value) {
                for (const child of group.children) {
                    if (child.value === compositeKey) {
                        formProviderId.value = providerId
                        formModelId.value = modelId
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

                const created = await assistantStore.create({
                    name: formName.value,
                    avatar: formAvatar.value || undefined,
                    description: formDescription.value || undefined,
                    systemPrompt: formSystemPrompt.value || undefined,
                    providerId: formProviderId.value ?? undefined,
                    modelId: formModelId.value,
                    memoryRounds: formMemoryRounds.value,
                    modelParams: Object.keys(modelParams).length > 0 ? modelParams : undefined,
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

    function buildModelParams(): Record<string, unknown> {
        const modelParams: Record<string, unknown> = {}
        for (const def of MODEL_PARAM_DEFS) {
            const v = formModelParams[def.key]
            if (v !== null && v !== undefined) modelParams[def.key] = v
        }
        return modelParams
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
        formProviderId,
        formModelId,
        formMemoryRounds,
        formEnabled,
        formModelParams,

        // 方法
        clearFieldError,
        loadDetail,
        onModelSelect,
        onAvatarPicked,
        handleSave,
        handleDelete,
        close,

        // 计算属性
        groupedModelOptions,
        selectedModelKey,
    }
}

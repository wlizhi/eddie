/**
 * useAssistantForm — 助手表单状态管理与业务逻辑
 *
 * 来源：AssistantDialog.vue script 部分
 * 提取后组件只保留 glue code + template + style
 */
import {computed, reactive, ref, watch} from 'vue'
import {useAssistantStore} from '@/stores/assistant'
import {useChatStore} from '@/stores/chat'
import {fetchAssistantDetail, updateAssistantAvatar} from '@/api/assistant'
import type {AssistantDetailVO} from '@/types/assistant'
import {MODEL_PARAM_DEFS} from '@/constants/modelParams'

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

    /** 是否为创建模式 */
    const isCreateMode = computed(() => props.createVisible === true && props.assistantId === null)

    const detail = ref<AssistantDetailVO | null>(null)
    const saving = ref(false)
    const feedback = ref('')

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
    const formMemoryRounds = ref(20)
    const formEnabled = ref<number>(1)

    const formModelParams = reactive<Record<string, number | null>>(
        Object.fromEntries(MODEL_PARAM_DEFS.map(d => [d.key, null]))
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
        feedback.value = ''
        if (chatStore.modelSelectors.length === 0) {
            await chatStore.loadModels()
        }
        await loadDetail(id)
        show.value = true
    })

    watch(() => props.createVisible, async (visible) => {
        if (visible) {
            feedback.value = ''
            resetFormForCreate()
            if (chatStore.modelSelectors.length === 0) {
                await chatStore.loadModels()
            }
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
        formMemoryRounds.value = 20
        formEnabled.value = 1
        for (const def of MODEL_PARAM_DEFS) {
            formModelParams[def.key] = null
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
            feedback.value = '加载助手详情失败'
            console.error(err)
            setTimeout(() => close(), 1500)
        }
    }

    // ========== 模型选择 ==========
    const groupedModelOptions = computed(() =>
        chatStore.modelSelectors.map((s) => ({
            type: 'group' as const,
            label: s.providerName,
            key: s.providerCode,
            children: s.models.map((m) => ({
                label: m.displayName ?? m.modelId,
                value: m.modelId,
            })),
        }))
    )

    function onModelSelect(modelId: string | null) {
        if (!modelId) return
        formModelId.value = modelId
        for (const sel of chatStore.modelSelectors) {
            const found = sel.models.find(m => m.modelId === modelId)
            if (found) {
                formProviderId.value = found.providerId
                return
            }
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
                    feedback.value = '✅ 创建成功'
                    close()
                } else {
                    feedback.value = '❌ 创建失败'
                }
            } catch (err) {
                feedback.value = '❌ 创建失败'
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
            feedback.value = '✅ 保存成功'
            close()
        } catch (err) {
            feedback.value = '❌ 保存失败'
            console.error(err)
        } finally {
            saving.value = false
        }
    }

    function buildModelParams(): Record<string, unknown> {
        const modelParams: Record<string, unknown> = {}
        for (const def of MODEL_PARAM_DEFS) {
            const v = formModelParams[def.key]
            if (v !== null) modelParams[def.key] = v
        }
        return modelParams
    }

    async function handleDelete() {
        if (!detail.value) return
        const name = formName.value || '此助手'
        const ok = confirm(`确定删除「${name}」？删除后不可恢复。`)
        if (!ok) return
        const id = detail.value.id
        close()
        await assistantStore.remove(id)
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
        feedback,
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
    }
}

<template>
  <n-modal
      :show="true"
      :mask-closable="false"
      preset="card"
      style="width: 90%; max-width: 35rem; max-height: 85vh;"
      header-style="flex-shrink: 0;"
      content-style="overflow-y: auto; min-height: 0;"
      footer-style="flex-shrink: 0;"
      @close="$emit('close')"
  >
    <template #header>
      <div class="mcp-modal-header">
        <span>{{ editing ? '编辑 MCP 服务器' : '新增 MCP 服务器' }}</span>
        <div class="mcp-modal-header-right">
          <span class="mcp-toggle-label">启用</span>
          <n-switch
              :value="enabled"
              :loading="toggling"
              :disabled="toggling"
              @update:value="handleToggle"
          />
        </div>
      </div>
    </template>

    <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="top"
        label-width="auto"
    >
      <n-form-item label="名称" path="name">
        <n-input v-model:value="form.name" placeholder="MCP 服务名称"/>
      </n-form-item>

      <n-form-item label="描述" path="description">
        <n-input v-model:value="form.description" placeholder="可选，MCP 服务说明" type="textarea" :rows="2"/>
      </n-form-item>

      <n-form-item label="传输方式" path="transportType">
        <n-radio-group v-model:value="form.transportType">
          <n-radio value="STDIO">STDIO</n-radio>
          <n-radio value="SSE">SSE</n-radio>
          <n-radio value="STREAMABLE_HTTP">Streamable HTTP</n-radio>
        </n-radio-group>
      </n-form-item>

      <!-- STDIO -->
      <template v-if="form.transportType === 'STDIO'">
        <n-form-item label="启动命令" path="command">
          <n-input v-model:value="form.command" placeholder="如 npx, node, python"/>
        </n-form-item>
        <n-form-item label="参数" path="args">
          <n-input v-model:value="form.args" placeholder='["arg1", "arg2"]'
                   type="textarea" :rows="2"/>
        </n-form-item>
        <n-form-item label="环境变量" path="env">
          <n-input v-model:value="form.env" placeholder="KEY=VALUE（每行一个）" type="textarea" :rows="2"/>
        </n-form-item>
      </template>

      <!-- SSE / HTTP -->
      <template v-else>
        <n-form-item label="服务端 URL" path="url">
          <n-input v-model:value="form.url" placeholder="https://mcp.example.com/sse"/>
        </n-form-item>
        <n-form-item label="自定义请求头" path="headers">
          <n-input v-model:value="form.headers" placeholder="Content-Type=application/json（每行一个）" type="textarea"
                   :rows="2"/>
        </n-form-item>
      </template>

      <n-form-item label="超时时间（秒）" path="timeoutSeconds">
        <n-input-number v-model:value="form.timeoutSeconds" :min="1" :max="600" style="width: 120px"/>
      </n-form-item>

      <n-form-item label="排序序号" path="sortOrder">
        <n-input-number v-model:value="form.sortOrder" :min="0" :max="9999" style="width: 120px"/>
      </n-form-item>

      <n-form-item label="重连间隔（秒）" path="reconnectIntervalSec">
        <n-input-number v-model:value="form.reconnectIntervalSec" :min="1" :max="300" placeholder="留空=5秒"
                        style="width: 120px" clearable/>
        <span style="margin-left: 8px; font-size: 12px; color: var(--text-quaternary);">留空=5秒</span>
      </n-form-item>

      <n-form-item label="最大重试次数" path="maxReconnectAttempts">
        <n-input-number v-model:value="form.maxReconnectAttempts" :min="0" :max="100" placeholder="留空=5次"
                        style="width: 120px" clearable/>
        <span style="margin-left: 8px; font-size: 12px; color: var(--text-quaternary);">留空=5次，0=不重试</span>
      </n-form-item>
    </n-form>

    <!-- 连接结果 -->
    <n-alert
        v-if="connectResult !== null"
        :type="connectResult.connected ? 'success' : 'error'"
        :title="connectResult.connected ? '连接成功' : '连接失败'"
        style="margin-top: 12px;"
    >
      <template #default>
        <div>{{ connectResult.message }}</div>
        <div v-if="connectResult.connected && connectResult.tools.length > 0" style="margin-top: 8px;">
          <n-tag
              v-for="tool in connectResult.tools"
              :key="tool.id"
              size="small"
              style="margin-right: 6px; margin-bottom: 4px;"
          >
            {{ tool.displayName || tool.name }}
          </n-tag>
        </div>
      </template>
    </n-alert>

    <template #footer>
      <div style="display: flex; justify-content: flex-end; gap: 8px;">
        <n-button @click="$emit('close')">取消</n-button>
        <n-button
            :loading="testing"
            :disabled="submitting"
            @click="handleTestConnection"
        >
          测试连接
        </n-button>
        <n-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editing ? '保存' : '创建' }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import {reactive, ref, watch} from 'vue'
import type {FormInst, FormRules} from 'naive-ui'
import {
  NAlert,
  NButton,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NModal,
  NRadio,
  NRadioGroup,
  NSwitch,
  NTag,
} from 'naive-ui'
import {createMcpServer, testMcpConnection, updateMcpStatus} from '@/api/mcpServer'
import type {McpConnectResult, McpServer} from '@/types/mcpServer'
import {showToast} from '@/composables/useToast'

const props = defineProps<{
  editing: McpServer | null
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)
const testing = ref(false)
const toggling = ref(false)
const enabled = ref(false)

/** 已创建的服务器 ID（测试连接/启用后记录） */
const savedServerId = ref<number | null>(null)
/** 连接测试结果 */
const connectResult = ref<McpConnectResult | null>(null)

const form = reactive({
  name: '',
  description: '',
  transportType: 'STDIO' as 'STDIO' | 'SSE' | 'STREAMABLE_HTTP',
  command: '',
  args: '',
  env: '',
  url: '',
  headers: '',
  timeoutSeconds: 60,
  sortOrder: 0,
  reconnectIntervalSec: null as number | null,
  maxReconnectAttempts: null as number | null,
})

// 编辑时预填
watch(
    () => props.editing,
    (val) => {
      if (val) {
        form.name = val.name
        form.description = val.description || ''
        form.transportType = val.transportType as 'STDIO' | 'SSE' | 'STREAMABLE_HTTP'
        form.command = val.command || ''
        form.args = val.args || ''
        form.env = val.env || ''
        form.url = val.url || ''
        form.headers = val.headers || ''
        form.timeoutSeconds = val.timeoutSeconds || 60
        form.sortOrder = val.sortOrder || 0
        form.reconnectIntervalSec = val.reconnectIntervalSec ?? null
        form.maxReconnectAttempts = val.maxReconnectAttempts ?? null
        enabled.value = val.enabled
        savedServerId.value = val.id
      } else {
        form.name = ''
        form.description = ''
        form.transportType = 'STDIO'
        form.command = ''
        form.args = ''
        form.env = ''
        form.url = ''
        form.headers = ''
        form.timeoutSeconds = 60
        form.sortOrder = 0
        form.reconnectIntervalSec = null
        form.maxReconnectAttempts = null
        enabled.value = false
        savedServerId.value = null
        connectResult.value = null
      }
    },
    {immediate: true}
)

function buildCreatePayload() {
  return {
    name: form.name,
    description: form.description || undefined,
    transportType: form.transportType,
    command: form.transportType === 'STDIO' ? form.command : undefined,
    args: form.transportType === 'STDIO' ? form.args : undefined,
    env: form.transportType === 'STDIO' ? form.env : undefined,
    url: form.transportType !== 'STDIO' ? form.url : undefined,
    headers: form.transportType !== 'STDIO' ? form.headers : undefined,
    timeoutSeconds: form.timeoutSeconds,
    sortOrder: form.sortOrder,
    reconnectIntervalSec: form.reconnectIntervalSec || undefined,
    maxReconnectAttempts: form.maxReconnectAttempts || undefined,
  }
}

const rules: FormRules = {
  name: [
    {required: true, message: '请输入 MCP 服务名称', trigger: 'blur'},
  ],
  transportType: [
    {required: true, message: '请选择传输方式', trigger: 'blur'},
  ],
  command: [
    {
      validator: (_rule: any, value: string) => {
        if (form.transportType === 'STDIO' && !value) {
          return new Error('STDIO 模式下启动命令不能为空')
        }
        return true
      },
      trigger: 'blur',
    },
  ],
  url: [
    {
      validator: (_rule: any, value: string) => {
        if (form.transportType !== 'STDIO' && !value) {
          return new Error('SSE/HTTP 模式下 URL 不能为空')
        }
        return true
      },
      trigger: 'blur',
    },
  ],
}

/** 创建服务器 */
async function ensureServerCreated(): Promise<number> {
  if (savedServerId.value !== null) return savedServerId.value
  const server = await createMcpServer(buildCreatePayload())
  savedServerId.value = server.id
  return server.id
}

/** 启用/禁用切换 */
async function handleToggle(value: boolean) {
  if (value === enabled.value) return

  // 启用前先验证表单
  if (value) {
    try {
      await formRef.value?.validate()
    } catch {
      showToast('请先填写必填信息', 'info')
      return
    }
  }

  toggling.value = true
  try {
    if (value) {
      // 启用 → 先创建服务器（如未创建），再更新状态（自动测试连接）
      const id = await ensureServerCreated()
      const result = await updateMcpStatus({
        mcpServerId: id,
        mcpEnabled: true,
      })
      enabled.value = true
      connectResult.value = result
      if (result.connected) {
        showToast('连接成功', 'success')
      } else {
        showToast(result.message || '连接失败', 'error')
      }
    } else {
      // 禁用
      if (savedServerId.value !== null) {
        await updateMcpStatus({
          mcpServerId: savedServerId.value,
          mcpEnabled: false,
        })
      }
      enabled.value = false
      connectResult.value = null
      showToast('MCP 服务已禁用', 'info')
    }
  } catch (e: any) {
    showToast(e.message || '操作失败', 'error')
    // 恢复切换状态
    enabled.value = !value
  } finally {
    toggling.value = false
  }
}

/** 测试连接 */
async function handleTestConnection() {
  try {
    await formRef.value?.validate()
  } catch {
    showToast('请先填写必填信息', 'info')
    return
  }

  testing.value = true
  try {
    // 直接传表单参数测试连接，不创建服务器、不写 DB、不改启用状态
    const result = await testMcpConnection(buildCreatePayload())
    connectResult.value = result
    if (result.connected) {
      showToast('连接成功', 'success')
    } else {
      showToast(result.message || '连接失败', 'error')
    }
  } catch (e: any) {
    showToast(e.message || '测试连接失败', 'error')
    connectResult.value = null
  } finally {
    testing.value = false
  }
}

/** 保存 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (savedServerId.value !== null) {
      // 编辑模式下已存在，直接刷新列表
      showToast('MCP 服务已更新', 'success')
    } else {
      // 纯新增（未测试连接）
      await createMcpServer(buildCreatePayload())
      showToast('MCP 服务已创建', 'success')
    }
    emit('saved')
  } catch (e: any) {
    showToast(e.message || '操作失败', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.mcp-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.mcp-modal-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mcp-toggle-label {
  font-size: 13px;
  color: var(--text-secondary);
}
</style>

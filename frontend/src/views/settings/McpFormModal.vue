<!--
 * @author Eddie
 * @date 2026-06-26
-->

<template>
  <n-modal
      :show="true"
      :mask-closable="false"
      preset="card"
      style="width: 90%; max-width: 35rem;"
      @close="$emit('close')"
  >
    <template #header>
      <div class="mcp-modal-header">
        <span>{{ editing ? '编辑 MCP 服务器' : '新增 MCP 服务器' }}</span>
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
          <n-radio value="STREAMABLE_HTTP">Streamable HTTP</n-radio>
          <n-radio value="SSE">SSE</n-radio>
          <n-radio value="STDIO">STDIO</n-radio>
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
              :key="tool.name"
              size="small"
              style="margin-right: 6px; margin-bottom: 4px;"
          >
            {{ truncateName(tool.name) }}
          </n-tag>
        </div>
      </template>
    </n-alert>

    <!-- 工具列表可视化（可折叠） -->
    <div v-if="connectResult?.connected && connectResult.tools.length > 0" class="tool-list-section">
      <div class="tool-list-header" @click="toolListExpanded = !toolListExpanded">
        <span>工具列表（{{ connectResult.tools.length }} 个）</span>
        <NIcon :size="14" class="tool-list-chevron" :class="{ rotated: toolListExpanded }">
          <ChevronDown/>
        </NIcon>
      </div>
      <div v-show="toolListExpanded" class="tool-list-items">
        <div v-for="tool in connectResult.tools" :key="tool.name" class="tool-list-item">
          <div class="tool-item-left">
            <span class="tool-item-name" :title="tool.name">{{ truncateName(tool.name) }}</span>
            <span class="tool-item-desc">{{ tool.description || '无描述' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 启用开关 -->
    <div class="mcp-enable-section">
      <span class="mcp-enable-label">启用</span>
      <label class="sidebar-toggle" @click.stop="handleToggle">
        <input
            type="checkbox"
            :checked="enabled"
            @click.stop
            @change.stop
        />
        <span class="toggle-track"></span>
      </label>
    </div>

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
  NIcon,
  NInput,
  NInputNumber,
  NModal,
  NRadio,
  NRadioGroup,
  NTag,
} from 'naive-ui'
import {ChevronDown} from '@lucide/vue'
import {createMcpServer, testMcpConnection, updateMcpServer as updateMcpServerApi,} from '@/api/mcpServer'
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
const enabled = ref(false)

/** 连接测试结果 */
const connectResult = ref<McpConnectResult | null>(null)
/** 工具列表展开状态 */
const toolListExpanded = ref(true)

const form = reactive({
  name: '',
  description: '',
  transportType: 'STREAMABLE_HTTP' as 'STDIO' | 'SSE' | 'STREAMABLE_HTTP',
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
      } else {
        form.name = ''
        form.description = ''
        form.transportType = 'STREAMABLE_HTTP'
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
        connectResult.value = null
      }

      // 编辑时如有已有工具列表，展示出来
      if (val && val.tools && val.tools.length > 0) {
        connectResult.value = {
          connected: true,
          message: `已有 ${val.tools.length} 个工具`,
          tools: val.tools,
        }
      } else if (!val) {
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

/** 启用/禁用切换 - 仅改本地状态，不调接口，最终保存时落盘 */
function handleToggle() {
  enabled.value = !enabled.value
  connectResult.value = null
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
    const msg = e.message || '测试连接失败'
    showToast(msg, 'error')
    // API 失败时也展示错误信息在连接结果栏中
    connectResult.value = {
      connected: false,
      message: msg,
      tools: [],
    }
  } finally {
    testing.value = false
  }
}

/** 工具名称截断（>50 字显示 ...） */
function truncateName(name: string): string {
  return name.length > 50 ? name.substring(0, 50) + '...' : name
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
    const payload = {
      ...buildCreatePayload(),
      enabled: enabled.value,
    }

    if (props.editing) {
      // 编辑模式
      await updateMcpServerApi(props.editing.id, payload)
      showToast('MCP 服务已更新', 'success')
    } else {
      // 新建模式
      await createMcpServer(payload)
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
  align-items: center;
  width: 100%;
}

/* ===== 启用开关 ===== */
.mcp-enable-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-default);
}

.mcp-enable-label {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--text-primary);
}

/* ===== 工具列表可视化 ===== */
.tool-list-section {
  margin-top: 12px;
  border: 1px solid var(--border-default);
  border-radius: 8px;
  overflow: hidden;
}

.tool-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  font-size: var(--font-size-small);
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--bg-hover);
  border-bottom: 1px solid var(--border-default);
  cursor: pointer;
  user-select: none;
}

.tool-list-chevron {
  color: var(--text-quaternary);
  transition: transform 0.2s;
}

.tool-list-chevron.rotated {
  transform: rotate(180deg);
}

.tool-list-items {
  max-height: 360px;
  overflow-y: auto;
}

.tool-list-item {
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-default);
}

.tool-list-item:last-child {
  border-bottom: none;
}

.tool-item-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tool-item-name {
  font-size: var(--font-size-small);
  font-weight: 500;
  color: var(--text-primary);
  font-family: monospace;
}

.tool-item-desc {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  line-height: 1.4;
  word-break: break-all;
}

/* ===== 自定义 Toggle 开关（同内置工具统一大小，rem 单位跟随全局字号缩放） ===== */
.sidebar-toggle {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 2rem;
  height: 1.125rem;
  cursor: pointer;
  flex-shrink: 0;
}

.sidebar-toggle.toggling {
  opacity: 0.6;
  pointer-events: none;
}

.sidebar-toggle input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.sidebar-toggle .toggle-track {
  position: absolute;
  inset: 0;
  background: var(--border-hover);
  border-radius: 9px;
  transition: background 0.2s;
}

.sidebar-toggle .toggle-track::before {
  content: '';
  position: absolute;
  top: 0.125rem;
  left: 0.125rem;
  width: 0.875rem;
  height: 0.875rem;
  background: #fff;
  border-radius: 50%;
  transition: transform 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}

.sidebar-toggle input:checked + .toggle-track {
  background: var(--accent-default);
}

.sidebar-toggle input:checked + .toggle-track::before {
  transform: translateX(0.875rem);
}
</style>

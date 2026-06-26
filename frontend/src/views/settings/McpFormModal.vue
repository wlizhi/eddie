<template>
  <n-modal
      :show="true"
      :mask-closable="false"
      preset="card"
      style="width: 520px; max-width: 90vw;"
      :title="editing ? '编辑 MCP 服务器' : '新增 MCP 服务器'"
      @close="$emit('close')"
      @positive-click="handleSubmit"
  >
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
          <n-input v-model:value="form.args" placeholder='["-y", "@modelcontextprotocol/server-filesystem"]'
                   type="textarea" :rows="2"/>
        </n-form-item>
        <n-form-item label="环境变量" path="env">
          <n-input v-model:value="form.env" placeholder='{"KEY": "VALUE"}' type="textarea" :rows="2"/>
        </n-form-item>
      </template>

      <!-- SSE / HTTP -->
      <template v-else>
        <n-form-item label="服务端 URL" path="url">
          <n-input v-model:value="form.url" placeholder="https://mcp.example.com/sse"/>
        </n-form-item>
      </template>

      <n-form-item label="超时时间（秒）" path="timeoutSeconds">
        <n-input-number v-model:value="form.timeoutSeconds" :min="1" :max="600" style="width: 120px"/>
      </n-form-item>
    </n-form>

    <template #footer>
      <div style="display: flex; justify-content: flex-end; gap: 8px;">
        <n-button @click="$emit('close')">取消</n-button>
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
import {NButton, NForm, NFormItem, NInput, NInputNumber, NModal, NRadio, NRadioGroup, useMessage} from 'naive-ui'
import {createMcpServer} from '@/api/mcpServer'
import type {McpServer} from '@/types/mcpServer'

const props = defineProps<{
  editing: McpServer | null
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

const message = useMessage()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)

const form = reactive({
  name: '',
  transportType: 'STDIO' as 'STDIO' | 'SSE' | 'STREAMABLE_HTTP',
  command: '',
  args: '[]',
  env: '{}',
  url: '',
  timeoutSeconds: 60,
})

// 编辑时预填
watch(
    () => props.editing,
    (val) => {
      if (val) {
        form.name = val.name
        form.transportType = val.transportType as 'STDIO' | 'SSE' | 'STREAMABLE_HTTP'
        form.command = val.command || ''
        form.args = val.args || '[]'
        form.env = val.env || '{}'
        form.url = val.url || ''
        form.timeoutSeconds = val.timeoutSeconds || 60
      } else {
        form.name = ''
        form.transportType = 'STDIO'
        form.command = ''
        form.args = '[]'
        form.env = '{}'
        form.url = ''
        form.timeoutSeconds = 60
      }
    },
    {immediate: true}
)

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

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await createMcpServer({
      name: form.name,
      transportType: form.transportType,
      command: form.transportType === 'STDIO' ? form.command : undefined,
      args: form.transportType === 'STDIO' ? form.args : undefined,
      env: form.transportType === 'STDIO' ? form.env : undefined,
      url: form.transportType !== 'STDIO' ? form.url : undefined,
      timeoutSeconds: form.timeoutSeconds,
    })
    message.success(props.editing ? 'MCP 服务已更新' : 'MCP 服务已创建')
    emit('saved')
  } catch (e: any) {
    message.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <NModal
      :show="visible"
      preset="card"
      :title="title"
      style="max-width: 380px; width: 90%;"
      :mask-closable="false"
      @update:show="(v: boolean) => { if (!v) $emit('cancel') }"
  >
    <p class="confirm-message">{{ message }}</p>

    <template #footer>
      <div class="confirm-footer">
        <button class="btn-cancel" @click="$emit('cancel')">取消</button>
        <button class="btn-danger" @click="$emit('confirm')">确认删除</button>
      </div>
    </template>
  </NModal>
</template>

<script setup lang="ts">
import {NModal} from 'naive-ui'

defineProps<{
  visible: boolean
  title: string
  message: string
}>()

defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<style scoped>
.confirm-message {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}

.confirm-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-cancel {
  height: 32px;
  padding: 0 16px;
  border: 1px solid var(--border-default);
  border-radius: 6px;
  background: var(--bg-primary);
  font-size: 13px;
  color: var(--text-quaternary);
  cursor: pointer;
  transition: border-color 0.12s, color 0.12s;
}

.btn-cancel:hover {
  border-color: var(--border-hover);
  color: var(--text-primary);
}

.btn-danger {
  height: 32px;
  padding: 0 16px;
  border: none;
  border-radius: 6px;
  background: var(--danger-default);
  font-size: 13px;
  color: var(--text-inverse);
  cursor: pointer;
  transition: background 0.12s;
}

.btn-danger:hover {
  background: var(--danger-hover);
}
</style>

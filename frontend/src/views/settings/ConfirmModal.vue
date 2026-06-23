<template>
  <div v-if="visible" class="modal-overlay" @click.self="$emit('cancel')">
    <div class="modal-content confirm-modal">
      <div class="modal-header">
        <h3>{{ title }}</h3>
        <button class="modal-close" @click="$emit('cancel')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      <div class="modal-body">
        <p class="confirm-message">{{ message }}</p>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="$emit('cancel')">取消</button>
        <button class="btn-danger" @click="$emit('confirm')">确认删除</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
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
/* ===== Modal 基础样式（与 model-provider.css 保持一致） ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #ffffff;
  border-radius: 12px;
  width: 520px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.confirm-modal {
  width: 380px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e6e8ec;
  flex-shrink: 0;
}

.modal-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1f1f1f;
  margin: 0;
}

.modal-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}

.modal-close:hover {
  background: #f0f1f3;
  color: #1f1f1f;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #e6e8ec;
  flex-shrink: 0;
}

.confirm-message {
  font-size: 14px;
  color: #374151;
  margin: 0;
  line-height: 1.6;
}

.btn-cancel {
  height: 32px;
  padding: 0 16px;
  border: 1px solid #e6e8ec;
  border-radius: 6px;
  background: #ffffff;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: border-color 0.12s, color 0.12s;
}

.btn-cancel:hover {
  border-color: #d1d5db;
  color: #1f1f1f;
}

.btn-danger {
  height: 32px;
  padding: 0 16px;
  border: none;
  border-radius: 6px;
  background: #ef4444;
  font-size: 13px;
  color: #ffffff;
  cursor: pointer;
  transition: background 0.12s;
}

.btn-danger:hover {
  background: #dc2626;
}
</style>

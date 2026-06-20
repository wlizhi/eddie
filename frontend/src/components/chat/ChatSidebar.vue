<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {ChevronDown, MessageSquare, Plus, Search, Settings, Trash2} from '@lucide/vue'
import {useAssistantStore} from '@/stores/assistant'
import AssistantAvatar from '../common/AssistantAvatar.vue'
import AssistantDialog from '../assistant/AssistantDialog.vue'

const assistantStore = useAssistantStore()

const DEFAULT_SHOWN = 3
const assistantListCollapsed = ref(false)
const showAllAssistants = ref(true)
const editAssistantId = ref<number | null>(null)

onMounted(() => {
  assistantStore.loadList()
})

const displayedAssistants = computed(() => {
  const items = assistantStore.enabledList
  return showAllAssistants.value ? items : items.slice(0, DEFAULT_SHOWN)
})

// Mock 会话列表（按当前助手不同返回不同数据）
const sessionsMap: Record<string, { id: string; title: string; updatedAt: string }[]> = {
  '1': [
    {id: 's1', title: '帮我写一个排序算法', updatedAt: '10分钟前'},
    {id: 's2', title: '解释一下 React Hooks', updatedAt: '1小时前'},
    {id: 's3', title: 'Vue 3 和 React 对比', updatedAt: '昨天'},
    {id: 's4', title: 'Python 异步编程入门', updatedAt: '2天前'},
  ],
  '2': [
    {id: 's5', title: '分析销售数据趋势', updatedAt: '30分钟前'},
    {id: 's6', title: '生成月度报告', updatedAt: '3小时前'},
  ],
  '3': [
    {id: 's7', title: '帮我优化这段 SQL', updatedAt: '15分钟前'},
    {id: 's8', title: '实现一个二分查找', updatedAt: '昨天'},
    {id: 's9', title: '代码审查反馈', updatedAt: '2天前'},
  ],
  '4': [
    {id: 's10', title: '翻译一篇技术文档', updatedAt: '1小时前'},
  ],
  '5': [
    {id: 's11', title: '审查 PR #42', updatedAt: '昨天'},
    {id: 's12', title: '检查安全性问题', updatedAt: '3天前'},
  ],
}

const sessions = computed(() => sessionsMap[String(assistantStore.activeId)] ?? [])
</script>

<template>
  <div class="sidebar">
    <!-- 助手列表折叠/展开按钮 -->
    <button class="collapse-assistant-btn" @click="assistantListCollapsed = !assistantListCollapsed">
      <ChevronDown :size="14" :stroke-width="2" class="collapse-icon" :class="{ rotated: !assistantListCollapsed }"/>
      <span>{{ assistantListCollapsed ? '展开助手列表' : '收起助手列表' }}</span>
    </button>

    <!-- 助手列表区域 -->
    <div class="assistant-section" v-show="!assistantListCollapsed">
      <div class="assistant-list">
        <div
            v-for="assistant in displayedAssistants"
            :key="assistant.id"
            class="assistant-item"
            :class="{ active: assistantStore.activeId === assistant.id }"
            @click="assistantStore.select(assistant.id)"
        >
          <AssistantAvatar :name="assistant.name" :avatar="assistant.avatar" :size="26"/>
          <span class="assistant-name">{{ assistant.name }}</span>
          <button
              class="assistant-settings"
              title="助手设置"
              @click.stop="editAssistantId = assistant.id"
          >
            <Settings :size="13" :stroke-width="2"/>
          </button>
        </div>

        <!-- 展开/收起 -->
        <button
            v-if="assistantStore.enabledList.length > DEFAULT_SHOWN"
            class="toggle-btn"
            @click="showAllAssistants = !showAllAssistants"
        >
          {{ showAllAssistants ? '▲ 收起' : `>>> 展示更多 (${assistantStore.enabledList.length - DEFAULT_SHOWN})` }}
        </button>
      </div>
    </div>

    <div class="divider"/>

    <!-- 会话列表区域 -->
    <div class="session-section">
      <!-- 搜索框 -->
      <div class="search-box">
        <Search :size="13" :stroke-width="2" class="search-icon"/>
        <input type="text" class="search-input" placeholder="搜索会话..."/>
      </div>

      <!-- 会话列表 -->
      <div class="session-list">
        <div
            v-for="session in sessions"
            :key="session.id"
            class="session-item"
        >
          <div class="session-icon">
            <MessageSquare :size="14" :stroke-width="1.8"/>
          </div>
          <div class="session-info">
            <div class="session-title">{{ session.title }}</div>
            <div class="session-time">{{ session.updatedAt }}</div>
          </div>
          <button class="session-delete" title="删除会话">
            <Trash2 :size="12" :stroke-width="2"/>
          </button>
        </div>
      </div>

      <!-- 新对话按钮 -->
      <button class="new-chat-btn">
        <Plus :size="15" :stroke-width="2"/>
        <span>新对话</span>
      </button>
    </div>
  </div>

  <!-- 助手设置弹窗 -->
  <AssistantDialog v-model:assistant-id="editAssistantId"/>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* ===== 通用 ===== */
.divider {
  height: 1px;
  background: #e6e8ec;
  margin: 4px 12px;
  flex-shrink: 0;
}

/* ===== 折叠助手列表按钮 ===== */
.collapse-assistant-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  padding: 7px 5px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #9ca3af;
  transition: background 0.15s, color 0.15s;
  flex-shrink: 0;
  border-bottom: 1px solid #e6e8ec;
}

.collapse-assistant-btn:hover {
  background: #f0f1f3;
  color: #6b7280;
}

.collapse-icon {
  transition: transform 0.2s;
}

.collapse-icon.rotated {
  transform: rotate(0deg);
}

/* ===== 助手列表 ===== */
.assistant-section {
  flex-shrink: 0;
}

.assistant-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 0 8px;
}

.assistant-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  width: 100%;
  text-align: left;
  transition: background 0.15s;
}

.assistant-item:hover {
  background: #f0f1f3;
}

.assistant-item.active {
  background: #e8f0fe;
}

.assistant-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f1f1f;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assistant-settings {
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d1d5db;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s, background 0.15s;
  flex-shrink: 0;
}

.assistant-item:hover .assistant-settings {
  opacity: 1;
}

.assistant-settings:hover {
  color: #6b7280;
  background: #e0e2e6;
}

/* 展开/收起按钮 */
.toggle-btn {
  display: block;
  width: 100%;
  padding: 6px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  color: #6b7280;
  text-align: left;
  transition: background 0.15s, color 0.15s;
}

.toggle-btn:hover {
  background: #f0f1f3;
  color: #2563eb;
}

/* ===== 会话区域 ===== */
.session-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

/* 搜索框 */
.search-box {
  position: relative;
  display: flex;
  align-items: center;
  padding: 4px 12px;
}

.search-icon {
  position: absolute;
  left: 20px;
  color: #9ca3af;
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 5px 8px 5px 26px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #f4f5f7;
  font-size: 12px;
  color: #1f1f1f;
  outline: none;
  transition: background 0.15s, border-color 0.15s;
}

.search-input::placeholder {
  color: #9ca3af;
}

.search-input:focus {
  background: #ffffff;
  border-color: #2563eb;
}

/* 会话列表 */
.session-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 2px 8px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.session-item:hover {
  background: #f4f5f7;
}

.session-icon {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f1f3;
  border-radius: 6px;
  color: #6b7280;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 12px;
  font-weight: 500;
  color: #1f1f1f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 1px;
}

.session-delete {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d1d5db;
  opacity: 0;
  transition: opacity 0.15s, color 0.15s, background 0.15s;
}

.session-item:hover .session-delete {
  opacity: 1;
}

.session-delete:hover {
  color: #ef4444;
  background: #fef2f2;
}

/* 新对话按钮 */
.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 6px 12px 10px;
  padding: 7px;
  border: 1px solid #e6e8ec;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #1f1f1f;
  transition: background 0.15s, border-color 0.15s;
  flex-shrink: 0;
}

.new-chat-btn:hover {
  background: #f4f5f7;
  border-color: #d0d4da;
}
</style>

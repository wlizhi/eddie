<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {ChevronDown, GripVertical, MessageSquare, Pin, Plus, Search, Settings, Trash2} from '@lucide/vue'
import {useAssistantStore} from '@/stores/assistant'
import {useChatStore} from '@/stores/chat'
import {batchSortAssistant} from '@/api/assistant'
import {deleteSession, fetchSessionList, pinSession, unpinSession} from '@/api/session'
import type {SessionVO} from '@/types/session'
import AssistantAvatar from '../common/AssistantAvatar.vue'
import AssistantDialog from '../assistant/AssistantDialog.vue'

const assistantStore = useAssistantStore()
const chatStore = useChatStore()

const DEFAULT_SHOWN = 3
const assistantListCollapsed = ref(false)
const showAllAssistants = ref(true)
const editAssistantId = ref<number | null>(null)
const showCreateAssistant = ref(false)

// ========== 会话列表 ==========
const sessions = ref<SessionVO[]>([])
const sessionsLoading = ref(false)

/** 加载会话列表 */
async function loadSessions() {
  if (!assistantStore.activeId) {
    sessions.value = []
    return
  }
  sessionsLoading.value = true
  try {
    sessions.value = await fetchSessionList(assistantStore.activeId)
  } catch (err) {
    console.error('加载会话列表失败:', err)
  } finally {
    sessionsLoading.value = false
  }
}

// 切换助手时重新加载会话列表
watch(() => assistantStore.activeId, () => {
  // 不自动切换到上次会话，让用户自行选择
  loadSessions()
}, {immediate: true})

// 首轮对话完成后需要刷新列表（标题已更新）
watch(() => chatStore.currentConversationId, (newId) => {
  if (newId && sessions.value.length > 0) {
    loadSessions()
  }
})

/** 格式化时间 */
function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

// ========== 拖拽排序 ==========
const dragIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)

function onDragStart(index: number) {
  dragIndex.value = index
}

function onDragOver(e: DragEvent, index: number) {
  e.preventDefault()
  dragOverIndex.value = index
}

function onDragLeave() {
  dragOverIndex.value = null
}

async function onDrop() {
  if (dragIndex.value === null || dragOverIndex.value === null) return
  if (dragIndex.value === dragOverIndex.value) {
    dragIndex.value = null
    dragOverIndex.value = null
    return
  }

  const list = [...assistantStore.list]
  const [moved] = list.splice(dragIndex.value, 1)
  list.splice(dragOverIndex.value, 0, moved)
  assistantStore.list.splice(0, assistantStore.list.length, ...list)

  dragIndex.value = null
  dragOverIndex.value = null

  try {
    await batchSortAssistant(list.map(a => a.id))
  } catch (err) {
    console.error('排序保存失败:', err)
    assistantStore.loadList(true, true)
  }
}

const displayedAssistants = computed(() => {
  const items = assistantStore.list
  return showAllAssistants.value ? items : items.slice(0, DEFAULT_SHOWN)
})

/** 点击会话：加载历史消息 */
function selectSession(session: SessionVO) {
  chatStore.loadConversation(session.id)
}

/** 删除会话 */
async function removeSession(sessionId: number) {
  try {
    await deleteSession(sessionId)
    sessions.value = sessions.value.filter(s => s.id !== sessionId)
    // 如果删除的是当前会话，新建
    if (chatStore.currentConversationId === String(sessionId)) {
      chatStore.newConversation()
    }
  } catch (err) {
    console.error('删除会话失败:', err)
  }
}

/** 置顶/取消置顶 */
async function togglePin(session: SessionVO) {
  try {
    if (session.pinned) {
      await unpinSession(session.id)
    } else {
      await pinSession(session.id)
    }
    loadSessions()
  } catch (err) {
    console.error('置顶操作失败:', err)
  }
}
</script>

<template>
  <div class="sidebar">
    <!-- 助手列表折叠/展开按钮 -->
    <button class="collapse-assistant-btn" @click="assistantListCollapsed = !assistantListCollapsed">
      <span class="create-trigger" title="创建助手" @click.stop="showCreateAssistant = true">
        <Plus :size="15" :stroke-width="2.5"/>
      </span>
      <ChevronDown :size="14" :stroke-width="2" class="collapse-icon" :class="{ rotated: !assistantListCollapsed }"/>
      <span>{{ assistantListCollapsed ? '展开助手列表' : '收起助手列表' }}</span>
    </button>

    <!-- 助手列表区域 -->
    <div class="assistant-section" v-show="!assistantListCollapsed">
      <div class="assistant-list">
        <div
            v-for="(assistant, index) in displayedAssistants"
            :key="assistant.id"
            class="assistant-item"
            :class="{
              active: assistantStore.activeId === assistant.id,
              disabled: assistant.enabled !== 1,
              'drag-over': dragOverIndex === index,
            }"
            draggable="true"
            @dragstart="onDragStart(index)"
            @dragover="onDragOver($event, index)"
            @dragleave="onDragLeave"
            @drop.prevent="onDrop"
            @dragend="dragIndex = null; dragOverIndex = null"
            @click="assistantStore.select(assistant.id)"
        >
          <span class="drag-handle"><GripVertical :size="12" :stroke-width="1.5"/></span>
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
            v-if="assistantStore.list.length > DEFAULT_SHOWN"
            class="toggle-btn"
            @click="showAllAssistants = !showAllAssistants"
        >
          {{ showAllAssistants ? '▲ 收起' : `>>> 展示更多 (${assistantStore.list.length - DEFAULT_SHOWN})` }}
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
            :class="{ active: chatStore.currentConversationId === String(session.id) }"
            @click="selectSession(session)"
        >
          <div class="session-icon">
            <MessageSquare :size="14" :stroke-width="1.8"/>
          </div>
          <div class="session-info">
            <div class="session-title">{{ session.title || '新对话' }}</div>
            <div class="session-time">{{ formatTime(session.updatedAt) }}</div>
          </div>
          <button class="session-pin" title="置顶" @click.stop="togglePin(session)">
            <Pin :size="11" :stroke-width="2" :class="{ pinned: session.pinned }"/>
          </button>
          <button class="session-delete" title="删除会话" @click.stop="removeSession(session.id)">
            <Trash2 :size="12" :stroke-width="2"/>
          </button>
        </div>
      </div>

      <!-- 新对话按钮 -->
      <button class="new-chat-btn" @click="chatStore.newConversation()">
        <Plus :size="15" :stroke-width="2"/>
        <span>新对话</span>
      </button>
    </div>
  </div>

  <!-- 助手设置/创建弹窗 -->
  <AssistantDialog v-model:assistant-id="editAssistantId" v-model:create-visible="showCreateAssistant"/>
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

/* ===== 启用/全部过滤器 ===== */
.filter-bar {
  display: flex;
  gap: 2px;
  padding: 4px 10px 2px;
  flex-shrink: 0;
}

.filter-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 3px 0;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  font-size: 11px;
  color: #9ca3af;
  transition: background 0.15s, color 0.15s;
}

.filter-btn:hover {
  background: #f0f1f3;
  color: #6b7280;
}

.filter-btn.active {
  background: #e8f0fe;
  color: #2563eb;
  font-weight: 500;
}

/* ===== 折叠助手列表按钮 ===== */
.collapse-assistant-btn {
  position: relative;
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

/* ===== 新建按钮（绝对定位在折叠按钮最左侧） ===== */
.create-trigger {
  position: absolute;
  left: 5px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 5px;
  background: transparent;
  cursor: pointer;
  color: #9ca3af;
  transition: background 0.1s, color 0.1s;
}

.create-trigger:hover {
  background: #e8f0fe;
  color: #2563eb;
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

/* 禁用的助手：降低透明度，灰色调 */
.assistant-item.disabled {
  opacity: 0.55;
}

.assistant-item.disabled:hover {
  background: #f9fafb;
}

/* 拖拽排序 */
.drag-handle {
  display: flex;
  align-items: center;
  color: #d1d5db;
  cursor: grab;
  flex-shrink: 0;
  transition: color 0.15s;
  margin-right: -2px;
}

.assistant-item:hover .drag-handle {
  color: #9ca3af;
}

.assistant-item.drag-over {
  border-top: 2px solid #2563eb;
  border-radius: 0;
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

/* 置顶按钮 */
.session-pin {
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

.session-item:hover .session-pin {
  opacity: 1;
}

.session-pin:hover {
  color: #2563eb;
  background: #e8f0fe;
}

.session-pin .pinned {
  color: #2563eb;
  opacity: 1;
}

.session-item.active {
  background: #e8f0fe;
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

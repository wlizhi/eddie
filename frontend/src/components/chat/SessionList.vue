<!--
 * @author Eddie
 * @date 2026-06-22
-->

<script setup lang="ts">
import {computed, nextTick, ref} from 'vue'
import {Loader2, MessageSquare, Pencil, Pin, Plus, Search, Sparkles, Trash2} from '@lucide/vue'
import {useChatStore} from '@/stores/chat'
import type {SessionVO} from '@/types/session'
import {useSessionList} from '@/composables/useSessionList'
import {useVirtualList} from '@/composables/useVirtualList'
import {useRelativeTime} from '@/composables/useRelativeTime'
import {useAssistantStore} from '@/stores/assistant'

const chatStore = useChatStore()
const assistantStore = useAssistantStore()

/** 行内重命名状态 */
const editingSessionId = ref<number | null>(null)
const editTitle = ref('')

// 会话列表（分页 + 服务端搜索）
const {
  searchQuery, sessions, sessionsLoading, loadingMore, loadMore,
  removeSession, togglePin, renameSession, aiGenerateTitle,
} = useSessionList(
    computed(() => assistantStore.activeId),
    computed(() => chatStore.sessionRefreshCounter),
    computed(() => chatStore.currentConversationId),
)

/** 虚拟滚动容器 ref */
const listContainer = ref<HTMLElement | null>(null)

/** 单项固定高度：与 CSS 中 .session-item 实际渲染高度一致 */
const ITEM_HEIGHT = 52

const {
  visibleItems,
  offsetY,
  totalHeight,
  onScroll,
} = useVirtualList(listContainer, {
  items: sessions,
  itemHeight: ITEM_HEIGHT,
  buffer: 5,
  loadMoreThreshold: 200,
  onLoadMore: loadMore,
})

/** 进入行内编辑模式 */
function startRename(session: SessionVO) {
  editingSessionId.value = session.id
  editTitle.value = session.title || ''
  nextTick(() => {
    const input = document.querySelector<HTMLInputElement>('.rename-input')
    input?.focus()
    input?.select()
  })
}

/** 提交重命名 */
function submitRename() {
  if (editingSessionId.value != null && editTitle.value.trim()) {
    renameSession(editingSessionId.value, editTitle.value.trim())
  }
  editingSessionId.value = null
  editTitle.value = ''
}

/** 取消重命名 */
function cancelRename() {
  editingSessionId.value = null
  editTitle.value = ''
}

// 相对时间
const {formatTime} = useRelativeTime()

function selectSession(session: SessionVO) {
  chatStore.loadConversation(session.id)
}
</script>

<template>
  <div class="session-section">
    <!-- 搜索框 -->
    <div class="search-box">
      <Search :size="13" :stroke-width="2" class="search-icon"/>
      <input v-model="searchQuery" type="text" class="search-input" placeholder="搜索会话..."/>
    </div>

    <!-- 首次加载中 -->
    <div v-if="sessionsLoading" class="loading-state">
      <Loader2 :size="16" :stroke-width="2" class="spin"/>
      <span>加载中...</span>
    </div>

    <!-- 虚拟滚动会话列表 -->
    <div
        v-else
        ref="listContainer"
        class="session-list"
        @scroll="onScroll"
    >
      <!-- 撑开滚动条高度的占位元素 -->
      <div :style="{ height: totalHeight + 'px', position: 'relative' }">
        <!-- 可见项容器，使用 transform 偏移到正确位置 -->
        <div :style="{ transform: `translateY(${offsetY}px)` }">
          <div
              v-for="session in visibleItems"
              :key="session.id"
              class="session-item"
              :class="{ active: chatStore.currentConversationId === String(session.id) }"
              @click="editingSessionId !== session.id && selectSession(session)"
          >
            <div class="session-icon">
              <MessageSquare :size="14" :stroke-width="1.8"/>
            </div>
            <div class="session-info">
              <div class="session-title" v-if="editingSessionId !== session.id">
                <span class="title-text">{{ session.title || '新对话' }}</span>
                <span v-if="session.messageCount > 0" class="msg-count">{{ session.messageCount }}</span>
              </div>
              <div class="session-edit" v-else @click.stop>
                <input
                    v-model="editTitle"
                    class="rename-input"
                    maxlength="50"
                    @keyup.enter="submitRename()"
                    @keyup.escape="cancelRename()"
                    @blur="submitRename()"
                />
              </div>
              <div class="session-time">{{ formatTime(session.updatedAt) }}</div>
            </div>
            <div class="session-actions">
              <button class="session-rename" title="重命名" @click.stop="startRename(session)">
                <Pencil :size="11" :stroke-width="2"/>
              </button>
              <button class="session-ai-title" title="AI 生成标题" @click.stop="aiGenerateTitle(session.id)">
                <Sparkles :size="11" :stroke-width="2"/>
              </button>
              <button class="session-pin" title="置顶" @click.stop="togglePin(session)">
                <Pin :size="11" :stroke-width="2" :class="{ pinned: session.pinned }"/>
              </button>
              <button class="session-delete" title="删除会话" @click.stop="removeSession(session.id)">
                <Trash2 :size="12" :stroke-width="2"/>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多指示器 -->
      <div v-if="loadingMore" class="loading-more">
        <Loader2 :size="12" :stroke-width="2" class="spin"/>
        <span>加载更多...</span>
      </div>

      <!-- 无数据 -->
      <div v-if="!sessionsLoading && sessions.length === 0" class="empty-state">
        <span>暂无会话</span>
      </div>
    </div>

    <!-- 新对话按钮 -->
    <button class="new-chat-btn" @click="chatStore.newConversation()">
      <Plus :size="15" :stroke-width="2"/>
      <span>新对话</span>
    </button>
  </div>
</template>

<style scoped>
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
  color: var(--text-tertiary);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 5px 8px 5px 26px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: var(--bg-tertiary);
  font-size: var(--font-size-small);
  color: var(--text-primary);
  outline: none;
  transition: background 0.15s, border-color 0.15s;
}

.search-input::placeholder {
  color: var(--text-tertiary);
}

.search-input:focus {
  background: var(--bg-primary);
  border-color: var(--accent-default);
}

/* 加载 / 空状态 */
.loading-state,
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: var(--font-size-small);
  color: var(--text-tertiary);
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 0;
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.spin {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 会话列表（虚拟滚动容器） */
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 2px 8px;
  position: relative;
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
  min-height: 44px;
  height: auto;
  box-sizing: border-box;
  touch-action: manipulation;
}

@media (hover: hover) {
  .session-item:hover {
    background: var(--bg-tertiary);
  }
}

/* 触屏设备：操作按钮常显，并给右侧预留按钮空间避免遮挡 */
@media (hover: none) {
  .session-item {
    padding-right: 96px;
  }
}

.session-item.active {
  background: var(--accent-light-bg);
}

.session-icon {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-hover);
  border-radius: 6px;
  color: var(--text-quaternary);
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-small);
  font-weight: 500;
  color: var(--text-primary);
  min-width: 0;
}

.title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-all;
}

.msg-count {
  flex-shrink: 0;
  min-width: 18px;
  height: 17px;
  padding: 0 5px;
  border-radius: 8px;
  background: var(--border-lighter);
  font-size: var(--font-size-xxs);
  font-weight: 600;
  color: var(--text-quaternary);
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.session-time {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  margin-top: 1px;
}

/* 行内编辑 */
.session-edit {
  flex: 1;
  min-width: 0;
}

.rename-input {
  width: 100%;
  padding: 2px 6px;
  border: 1px solid var(--accent-default);
  border-radius: 4px;
  background: var(--bg-primary);
  font-size: var(--font-size-small);
  font-weight: 500;
  color: var(--text-primary);
  outline: none;
}

/* 操作按钮容器 —— 绝对定位悬浮，不占空间 */
.session-actions {
  position: absolute;
  right: 4px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 1px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s;
  background: inherit;
  padding: 2px 0;
}

@media (hover: hover) {
  .session-item:hover .session-actions {
    opacity: 1;
    pointer-events: auto;
  }
}

/* 触屏设备：操作按钮始终可见 */
@media (hover: none) {
  .session-actions {
    opacity: 1;
    pointer-events: auto;
  }
}

.session-rename,
.session-ai-title,
.session-pin,
.session-delete {
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-quaternary);
  transition: color 0.15s, background 0.15s;
}

.session-rename:hover {
  color: var(--text-secondary);
  background: var(--border-light);
}

.session-ai-title:hover {
  color: var(--tag-vision-text);
  background: #f3f0ff;
}

.session-pin:hover {
  color: var(--accent-default);
  background: var(--accent-light-bg);
}

.session-delete:hover {
  color: var(--danger-default);
  background: var(--danger-light-bg);
}

.session-pin .pinned {
  color: var(--accent-default);
}

/* 新对话按钮 */
.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 6px 12px 10px;
  padding: 7px;
  border: 1px solid var(--border-default);
  border-radius: 8px;
  background: var(--bg-primary);
  cursor: pointer;
  font-size: var(--font-size-small);
  font-weight: 500;
  color: var(--text-primary);
  transition: background 0.15s, border-color 0.15s;
  flex-shrink: 0;
}

.new-chat-btn:hover {
  background: var(--bg-tertiary);
  border-color: #d0d4da;
}
</style>

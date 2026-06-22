<script setup lang="ts">
import AssistantList from './AssistantList.vue'
import SessionList from './SessionList.vue'
</script>

<template>
  <div class="sidebar">
    <AssistantList/>
    <div class="divider"/>
    <SessionList/>
  </div>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.divider {
  height: 1px;
  background: #e6e8ec;
  margin: 4px 12px;
  flex-shrink: 0;
}
</style>

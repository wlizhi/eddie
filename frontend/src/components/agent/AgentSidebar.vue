<!--
 * @author Eddie
 * @date 2026-07-04
 -->

<script setup lang="ts">
import AgentList from './AgentList.vue'
import AgentSessionList from './AgentSessionList.vue'
</script>

<template>
  <div class="sidebar">
    <AgentList/>
    <div class="session-wrapper">
      <AgentSessionList/>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.session-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
</style>

<script setup lang="ts">
import { RouterView } from 'vue-router';
import AppHeader from '@/components/AppHeader.vue';
</script>

<template>
  <div class="shell">
    <AppHeader />
    <RouterView />
    <footer class="foot">
      <span class="mono">© Boplats Tracker · sourced from Vidingehem</span>
    </footer>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.foot {
  padding: 28px var(--page-pad);
  border-top: 1px solid var(--line);
  margin-top: auto;
  text-align: center;
}
</style>

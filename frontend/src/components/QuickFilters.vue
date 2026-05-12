<script setup lang="ts">
import type { QuickPreset } from '@/composables/useFilters';

defineProps<{ value: QuickPreset }>();
const emit = defineEmits<{ (e: 'update', value: QuickPreset): void }>();

const presets: Array<{ key: QuickPreset; label: string; icon: string }> = [
  { key: 'all', label: 'All', icon: '✦' },
  { key: 'spacious', label: 'Spacious', icon: 'm²' },
  { key: 'lowqueue', label: 'Low queue', icon: 'q' },
  { key: 'deadline', label: 'Deadline soon', icon: '7' },
  { key: 'floorplan', label: 'Floor plan', icon: 'p' },
];
</script>

<template>
  <nav class="quick" aria-label="Quick filters">
    <button
      v-for="preset in presets"
      :key="preset.key"
      type="button"
      :class="['quick-pill', { active: value === preset.key }]"
      @click="emit('update', preset.key)"
    >
      <span class="badge">{{ preset.icon }}</span>
      {{ preset.label }}
    </button>
  </nav>
</template>

<style scoped>
.quick {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.quick-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px 10px 10px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r-pill);
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease, transform 0.15s ease;
}

.quick-pill:hover {
  background: var(--bg-soft);
  transform: translateY(-1px);
}

.quick-pill.active {
  background: var(--ink);
  color: var(--white);
  border-color: var(--ink);
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--bg-soft);
  border: 1px solid var(--line);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--ink);
}

.quick-pill.active .badge {
  background: var(--accent);
  color: var(--white);
  border-color: var(--accent);
}
</style>

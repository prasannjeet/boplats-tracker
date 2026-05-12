<script setup lang="ts">
import { computed } from 'vue';
import { useSaved } from '@/composables/useSaved';

const props = defineProps<{ internalId: number; label?: string }>();
const { isSaved, toggle } = useSaved();

const saved = computed(() => isSaved(props.internalId));
const ariaLabel = computed(() => (saved.value ? `Unsave ${props.label ?? 'listing'}` : `Save ${props.label ?? 'listing'}`));

function onClick(e: MouseEvent) {
  e.preventDefault();
  e.stopPropagation();
  toggle(props.internalId);
}
</script>

<template>
  <button class="save-btn" :class="{ saved }" :aria-pressed="saved" :aria-label="ariaLabel" @click="onClick" type="button">
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
    </svg>
  </button>
</template>

<style scoped>
.save-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--surface-2);
  border: 1px solid var(--line);
  color: var(--ink);
  transition: background 0.15s ease, color 0.15s ease, transform 0.15s ease;
}

.save-btn:hover {
  background: var(--bg-soft);
  transform: scale(1.04);
}

.save-btn svg {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  stroke-width: 1.8;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.save-btn.saved {
  background: var(--accent);
  border-color: var(--accent);
  color: var(--white);
}

.save-btn.saved svg {
  fill: currentColor;
}
</style>

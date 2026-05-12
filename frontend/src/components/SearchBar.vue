<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{ modelValue: string; placeholder?: string }>();
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'open-filters'): void;
}>();

const local = ref(props.modelValue);
let t: ReturnType<typeof setTimeout> | null = null;

watch(
  () => props.modelValue,
  (v) => {
    if (v !== local.value) local.value = v;
  },
);

function onInput(e: Event) {
  local.value = (e.target as HTMLInputElement).value;
  if (t) clearTimeout(t);
  t = setTimeout(() => emit('update:modelValue', local.value), 200);
}

function clear() {
  local.value = '';
  emit('update:modelValue', '');
}
</script>

<template>
  <label class="search-bar">
    <svg class="icon-lead" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </svg>
    <input
      type="search"
      :value="local"
      :placeholder="placeholder ?? 'Search Växjö, Lammhult, postcode, listing ID…'"
      @input="onInput"
      aria-label="Search listings"
    />
    <button v-if="local" class="clear" type="button" aria-label="Clear search" @click="clear">
      <svg viewBox="0 0 24 24"><path d="M18 6 6 18M6 6l12 12" /></svg>
    </button>
    <button class="filter-cta" type="button" @click="emit('open-filters')">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16" /><path d="M7 12h10" /><path d="M10 17h4" /></svg>
      <span>Filters</span>
    </button>
  </label>
</template>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px 10px 18px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-pill);
  width: 100%;
}

.icon-lead {
  width: 18px;
  height: 18px;
  stroke: var(--muted);
  fill: none;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
  flex-shrink: 0;
}

input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  padding: 4px 0;
  min-width: 0;
}

input::placeholder {
  color: var(--muted);
}

.clear,
.filter-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--ink);
  flex-shrink: 0;
}

.clear svg,
.filter-cta svg {
  width: 16px;
  height: 16px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.clear:hover {
  background: var(--bg-soft);
}

.filter-cta {
  width: auto;
  padding: 0 18px 0 14px;
  gap: 8px;
  background: var(--ink);
  color: var(--white);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.01em;
  border-radius: var(--r-pill);
  height: 40px;
}

.filter-cta:hover {
  background: var(--accent);
}

.filter-cta span {
  display: inline-block;
}

@media (max-width: 520px) {
  .filter-cta span {
    display: none;
  }
  .filter-cta {
    width: 40px;
    padding: 0;
  }
}
</style>

<!-- frontend/src/components/TypeSelectorRow.vue -->
<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import { useObjectTypes } from '@/composables/useObjectTypes';
import { useFilters } from '@/composables/useFilters';
import { typeIcon } from '@/lib/derived';

const { objectTypes, load } = useObjectTypes();
const { filters, update } = useFilters();

onMounted(() => load());

const activeTypes = computed(() => filters.value.types);
const isAllActive = computed(() => activeTypes.value.length === 0);

function toggleType(typeId: string) {
  const current = activeTypes.value;
  const next = current.includes(typeId)
    ? current.filter((t) => t !== typeId)
    : [...current, typeId];
  update({ types: next });
}

function selectAll() {
  update({ types: [] });
}
</script>

<template>
  <div class="type-row" role="toolbar" aria-label="Filter by property type">
    <button type="button" :class="['type-chip', { active: isAllActive }]" @click="selectAll">
      Alla typer
    </button>
    <span v-for="ot in objectTypes" :key="ot.typeId" class="type-chip-group">
      <button
        type="button"
        :class="['type-chip', { active: !isAllActive && activeTypes.includes(ot.typeId) }]"
        @click="toggleType(ot.typeId)"
      >
        <span class="type-icon" aria-hidden="true">{{ typeIcon(ot.typeId) }}</span>
        {{ ot.displayName ?? ot.typeId }}
        <span v-if="ot.numberOfMarketObjects != null" class="type-count">{{ ot.numberOfMarketObjects }}</span>
      </button>
      <RouterLink
        :to="`/types/${ot.typeId}`"
        class="type-info-link"
        :title="`Om ${ot.displayName ?? ot.typeId}`"
        aria-label="Mer info"
      >ℹ</RouterLink>
    </span>
  </div>
</template>

<style scoped>
.type-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.type-chip-group {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.type-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: var(--r-pill);
  background: var(--surface-2);
  border: 1px solid var(--line);
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-soft);
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.type-chip:hover {
  background: var(--bg-soft);
  color: var(--ink);
}

.type-chip.active {
  background: var(--ink);
  color: var(--white);
  border-color: var(--ink);
}

.type-icon {
  font-size: 12px;
}

.type-count {
  font-size: 11px;
  font-family: var(--font-mono);
  opacity: 0.7;
}

.type-info-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-size: 11px;
  color: var(--muted);
  text-decoration: none;
  transition: background 0.12s, color 0.12s;
}

.type-info-link:hover {
  color: var(--ink);
  background: var(--surface-2);
}
</style>

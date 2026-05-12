<script setup lang="ts">
import { computed, watch, onBeforeUnmount } from 'vue';
import { defaultFilters, useFilters, type DeadlinePreset, type SortKey } from '@/composables/useFilters';
import { useHouses } from '@/composables/useHouses';

const props = defineProps<{ open: boolean; matchCount: number }>();
const emit = defineEmits<{ (e: 'close'): void }>();

const { filters, update, reset, toggleArray, activeCount } = useFilters();
const { cities } = useHouses();

const roomChoices = [1, 2, 3, 4, 5];
const deadlinePresets: Array<{ key: DeadlinePreset; label: string }> = [
  { key: 'all', label: 'Any deadline' },
  { key: 'today', label: 'Today' },
  { key: 'next3', label: 'Next 3 days' },
  { key: 'next7', label: 'Next 7 days' },
  { key: 'month', label: 'This month' },
];

const sortChoices: Array<{ key: SortKey; label: string }> = [
  { key: 'deadline', label: 'Deadline soonest' },
  { key: 'newest', label: 'Newest first' },
  { key: 'rentLow', label: 'Rent: low → high' },
  { key: 'rentHigh', label: 'Rent: high → low' },
  { key: 'areaLarge', label: 'Area: large → small' },
  { key: 'areaSmall', label: 'Area: small → large' },
  { key: 'cppLow', label: 'Cost per m²: low → high' },
  { key: 'queueLow', label: 'Queue points: low → high' },
  { key: 'queueHigh', label: 'Queue points: high → low' },
  { key: 'roomsMany', label: 'Most rooms first' },
];

function numericInputHandler(key: keyof typeof defaultFilters) {
  return (e: Event) => {
    const raw = (e.target as HTMLInputElement).value;
    const num = raw === '' ? null : Number(raw);
    update({ [key]: num == null || !Number.isFinite(num) ? null : num } as never);
  };
}

const titleId = computed(() => `filter-sheet-title-${Math.random().toString(36).slice(2, 8)}`);

function handleEscape(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.open) emit('close');
}

watch(
  () => props.open,
  (open) => {
    if (typeof document === 'undefined') return;
    if (open) {
      document.body.style.overflow = 'hidden';
      document.addEventListener('keydown', handleEscape);
    } else {
      document.body.style.overflow = '';
      document.removeEventListener('keydown', handleEscape);
    }
  },
);

onBeforeUnmount(() => {
  if (typeof document !== 'undefined') {
    document.body.style.overflow = '';
    document.removeEventListener('keydown', handleEscape);
  }
});
</script>

<template>
  <div v-if="open" class="filter-overlay" role="dialog" aria-modal="true" :aria-labelledby="titleId" @click.self="emit('close')">
    <aside class="filter-sheet">
      <header class="head">
        <div>
          <span class="kicker">Full filters {{ activeCount ? `· ${activeCount} active` : '' }}</span>
          <h2 :id="titleId">Refine search</h2>
        </div>
        <button class="icon-btn" type="button" aria-label="Close filters" @click="emit('close')">
          <svg viewBox="0 0 24 24"><path d="M18 6 6 18M6 6l12 12" /></svg>
        </button>
      </header>

      <div class="scroll">
        <section class="group">
          <h3>Location</h3>
          <div class="pill-row">
            <button
              v-for="city in cities"
              :key="city"
              type="button"
              :class="['pill', { active: filters.cities.includes(city) }]"
              @click="toggleArray('cities', city)"
            >
              {{ city }}
            </button>
          </div>
        </section>

        <section class="group">
          <h3>Property</h3>
          <div class="pill-row">
            <button
              v-for="r in roomChoices"
              :key="r"
              type="button"
              :class="['pill', { active: filters.rooms.includes(r) }]"
              @click="toggleArray('rooms', r)"
            >
              {{ r }} room{{ r === 1 ? '' : 's' }}
            </button>
          </div>
          <div class="range-row">
            <label class="field">
              <span>Area min (m²)</span>
              <input type="number" min="0" :value="filters.areaMin ?? ''" @input="numericInputHandler('areaMin')" />
            </label>
            <label class="field">
              <span>Area max (m²)</span>
              <input type="number" min="0" :value="filters.areaMax ?? ''" @input="numericInputHandler('areaMax')" />
            </label>
          </div>
        </section>

        <section class="group">
          <h3>Cost</h3>
          <div class="range-row">
            <label class="field">
              <span>Rent min (kr)</span>
              <input type="number" min="0" step="100" :value="filters.rentMin ?? ''" @input="numericInputHandler('rentMin')" />
            </label>
            <label class="field">
              <span>Rent max (kr)</span>
              <input type="number" min="0" step="100" :value="filters.rentMax ?? ''" @input="numericInputHandler('rentMax')" />
            </label>
          </div>
        </section>

        <section class="group">
          <h3>Queue pressure</h3>
          <div class="range-row">
            <label class="field">
              <span>Queue min</span>
              <input type="number" min="0" :value="filters.queueMin ?? ''" @input="numericInputHandler('queueMin')" />
            </label>
            <label class="field">
              <span>Queue max</span>
              <input type="number" min="0" :value="filters.queueMax ?? ''" @input="numericInputHandler('queueMax')" />
            </label>
          </div>
          <label class="toggle">
            <input type="checkbox" :checked="filters.includeUnknownQueue" @change="update({ includeUnknownQueue: ($event.target as HTMLInputElement).checked })" />
            <span>Include listings with no queue data</span>
          </label>
        </section>

        <section class="group">
          <h3>Deadline</h3>
          <div class="pill-row">
            <button
              v-for="d in deadlinePresets"
              :key="d.key"
              type="button"
              :class="['pill', { active: filters.deadline === d.key }]"
              @click="update({ deadline: d.key })"
            >
              {{ d.label }}
            </button>
          </div>
          <label class="toggle">
            <input type="checkbox" :checked="filters.hidePassed" @change="update({ hidePassed: ($event.target as HTMLInputElement).checked })" />
            <span>Hide listings whose deadline has passed</span>
          </label>
        </section>

        <section class="group">
          <h3>Content quality</h3>
          <label class="toggle">
            <input type="checkbox" :checked="filters.hasFloorplan" @change="update({ hasFloorplan: ($event.target as HTMLInputElement).checked })" />
            <span>Has floor plan</span>
          </label>
          <label class="toggle">
            <input type="checkbox" :checked="(filters.minImages ?? 0) >= 3" @change="update({ minImages: ($event.target as HTMLInputElement).checked ? 3 : null })" />
            <span>At least 3 images</span>
          </label>
          <label class="toggle">
            <input type="checkbox" :checked="filters.hasDescription" @change="update({ hasDescription: ($event.target as HTMLInputElement).checked })" />
            <span>Has description</span>
          </label>
        </section>

        <section class="group">
          <h3>Sort</h3>
          <label class="field full">
            <span>Order results by</span>
            <select :value="filters.sort" @change="update({ sort: ($event.target as HTMLSelectElement).value as SortKey })">
              <option v-for="opt in sortChoices" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
            </select>
          </label>
        </section>
      </div>

      <footer class="actions">
        <button type="button" class="btn ghost" @click="reset">Reset all</button>
        <button type="button" class="btn accent" @click="emit('close')">Show {{ matchCount }} listings</button>
      </footer>
    </aside>
  </div>
</template>

<style scoped>
.filter-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  background: rgba(20, 17, 13, 0.45);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: flex-end;
}

.filter-sheet {
  width: min(440px, 100%);
  background: var(--bg);
  display: flex;
  flex-direction: column;
  border-left: 1px solid var(--line);
  box-shadow: var(--shadow-lift);
  animation: slide-in 0.25s ease;
}

@keyframes slide-in {
  from {
    transform: translateX(20px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 22px 24px 14px;
  border-bottom: 1px solid var(--line);
}

.head h2 {
  margin: 6px 0 0;
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 900;
  letter-spacing: -0.02em;
}

.scroll {
  flex: 1;
  overflow-y: auto;
  padding: 4px 24px 24px;
}

.group {
  padding: 18px 0;
  border-bottom: 1px solid var(--line);
}

.group:last-child {
  border-bottom: none;
}

.group h3 {
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 12px;
}

.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.range-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  padding: 8px 12px;
}

.field.full {
  grid-column: 1 / -1;
}

.field > span {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--muted);
}

.field input,
.field select {
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  font-weight: 600;
  padding: 2px 0;
}

.toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  font-size: 14px;
}

.toggle input {
  width: 18px;
  height: 18px;
  accent-color: var(--accent);
}

.actions {
  display: flex;
  gap: 10px;
  padding: 16px 24px 24px;
  border-top: 1px solid var(--line);
  background: var(--bg);
}

.actions .btn {
  flex: 1;
}

@media (max-width: 600px) {
  .filter-sheet {
    width: 100%;
  }
  .head h2 {
    font-size: 24px;
  }
}
</style>

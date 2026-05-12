<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useHouses } from '@/composables/useHouses';
import { useFilters, filterHouses, sortHouses, type ViewMode } from '@/composables/useFilters';
import SearchBar from '@/components/SearchBar.vue';
import QuickFilters from '@/components/QuickFilters.vue';
import HouseCard from '@/components/HouseCard.vue';
import HouseGridCard from '@/components/HouseGridCard.vue';
import HouseMiniCard from '@/components/HouseMiniCard.vue';
import HouseMap from '@/components/HouseMap.vue';
import FilterSheet from '@/components/FilterSheet.vue';
import { formatRelative } from '@/lib/format';

const { houses, freshness, load, loading, error } = useHouses();
const { filters, update, activeCount } = useFilters();

const sheetOpen = ref(false);
const hoverId = ref<number | null>(null);

onMounted(() => load());

const now = computed(() => Date.now());

const filtered = computed(() => {
  const base = houses.value.filter((h) => h.endDate == null);
  const matched = filterHouses(base, filters.value, now.value);
  return sortHouses(matched, filters.value.sort, now.value);
});

const summaryParts = computed(() => {
  const parts: string[] = [];
  const f = filters.value;
  if (f.cities.length) parts.push(f.cities.join(', '));
  if (f.rooms.length) parts.push(`${f.rooms.join(', ')} rooms`);
  if (f.rentMin != null || f.rentMax != null) parts.push(`rent ${f.rentMin ?? '–'}–${f.rentMax ?? '–'} kr`);
  if (f.deadline !== 'all') parts.push(`deadline ${f.deadline.replace('next', 'in ')}`);
  if (f.hasFloorplan) parts.push('floor plan');
  if (parts.length === 0) parts.push('all active listings');
  return parts.join(' · ');
});

const viewModes: Array<{ key: ViewMode; label: string; icon: string }> = [
  { key: 'list', label: 'List', icon: 'list' },
  { key: 'grid', label: 'Grid', icon: 'grid' },
  { key: 'split', label: 'Split', icon: 'split' },
];

const view = computed<ViewMode>(() => filters.value.view);
const lastFetched = computed(() => formatRelative(freshness.value?.toISOString() ?? null));

function setView(mode: ViewMode) {
  update({ view: mode });
}

function setQuery(q: string) {
  update({ q });
}
</script>

<template>
  <div class="listings page">
    <header class="head">
      <div>
        <span class="kicker">Search the city</span>
        <h1 class="display headline">
          Find a home<span class="dot"></span>
        </h1>
      </div>
      <span class="mono updated">Updated {{ lastFetched }}</span>
    </header>

    <SearchBar :model-value="filters.q" @update:model-value="setQuery" @open-filters="sheetOpen = true" />

    <QuickFilters :value="filters.preset" @update="(v) => update({ preset: v })" />

    <div class="bar">
      <div class="summary">
        <strong>{{ filtered.length }} listings match</strong>
        <span class="meta">{{ summaryParts }}</span>
      </div>
      <div class="bar-right">
        <button
          type="button"
          class="filter-btn"
          :class="{ 'has-active': activeCount > 0 }"
          @click="sheetOpen = true"
          aria-label="Open filters"
        >
          <svg viewBox="0 0 24 24"><path d="M4 7h16" /><path d="M7 12h10" /><path d="M10 17h4" /></svg>
          Filters
          <span v-if="activeCount" class="badge">{{ activeCount }}</span>
        </button>
        <div class="view-switch" role="tablist" aria-label="View mode">
          <button
            v-for="mode in viewModes"
            :key="mode.key"
            type="button"
            role="tab"
            :aria-selected="view === mode.key"
            :class="{ active: view === mode.key }"
            @click="setView(mode.key)"
          >
            {{ mode.label }}
          </button>
        </div>
      </div>
    </div>

    <section v-if="filtered.length === 0 && !loading" class="empty">
      <h3>No listings match these filters.</h3>
      <p>Try widening your range or clearing the active filters.</p>
      <button type="button" class="btn ghost" @click="update({ ...{ q: '', cities: [], rooms: [], areaMin: null, areaMax: null, rentMin: null, rentMax: null, queueMin: null, queueMax: null, deadline: 'all', hasFloorplan: false, minImages: null, hasDescription: false, preset: 'all' } })">
        Clear filters
      </button>
    </section>

    <section v-else-if="view === 'list'" class="list-stack">
      <HouseCard
        v-for="h in filtered"
        :key="h.internalId"
        :house="h"
        :highlighted="hoverId === h.internalId"
        @mouseenter="hoverId = h.internalId"
        @mouseleave="hoverId = null"
      />
    </section>

    <section v-else-if="view === 'grid'" class="grid-stack">
      <HouseGridCard v-for="h in filtered" :key="h.internalId" :house="h" />
    </section>

    <section v-else class="split">
      <div class="split-list">
        <HouseMiniCard
          v-for="h in filtered"
          :key="h.internalId"
          :house="h"
          @mouseenter="hoverId = h.internalId"
          @mouseleave="hoverId = null"
        />
      </div>
      <div class="split-map">
        <HouseMap :houses="filtered" :highlight-id="hoverId" height="calc(100vh - 240px)" @hover="(id) => (hoverId = id)" />
      </div>
    </section>

    <div v-if="loading && houses.length === 0" class="state-pad">Loading listings…</div>
    <div v-if="error" class="state-pad error">Couldn't load listings: {{ error }}</div>

    <FilterSheet :open="sheetOpen" :match-count="filtered.length" @close="sheetOpen = false" />
  </div>
</template>

<style scoped>
.listings {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
}

.headline {
  font-size: clamp(56px, 8vw, 110px);
  line-height: 0.92;
}

.updated {
  white-space: nowrap;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 11px;
}

.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.summary strong {
  font-size: 18px;
  font-weight: 700;
}

.summary .meta {
  font-size: 12px;
  color: var(--muted);
  text-transform: lowercase;
  letter-spacing: 0.02em;
}

.bar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.filter-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px 10px 14px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r-pill);
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.filter-btn svg {
  width: 16px;
  height: 16px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.filter-btn:hover {
  background: var(--bg-soft);
}

.filter-btn.has-active {
  background: var(--ink);
  color: var(--white);
  border-color: var(--ink);
}

.filter-btn .badge {
  margin-left: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  background: var(--accent);
  color: var(--white);
  font-size: 11px;
  font-weight: 700;
}

.view-switch {
  display: inline-flex;
  padding: 4px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-pill);
}

.view-switch button {
  padding: 8px 18px;
  border-radius: var(--r-pill);
  background: transparent;
  border: none;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-soft);
}

.view-switch button.active {
  background: var(--ink);
  color: var(--white);
}

.list-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.grid-stack {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 22px;
}

.split {
  display: grid;
  grid-template-columns: minmax(360px, 0.9fr) 1.4fr;
  gap: 22px;
  align-items: start;
}

.split-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: calc(100vh - 240px);
  overflow-y: auto;
  padding-right: 6px;
}

.split-list::-webkit-scrollbar {
  width: 8px;
}

.split-list::-webkit-scrollbar-thumb {
  background: var(--line-strong);
  border-radius: 999px;
}

.split-map {
  position: sticky;
  top: 100px;
}

.empty {
  padding: 56px;
  text-align: center;
  background: var(--surface);
  border: 1px dashed var(--line-strong);
  border-radius: var(--r-lg);
}

.empty h3 {
  font-family: var(--font-display);
  font-size: 28px;
  margin: 0;
  font-weight: 900;
}

.empty p {
  color: var(--muted);
  margin: 8px 0 18px;
}

.state-pad {
  padding: 32px 0;
  color: var(--muted);
  text-align: center;
}

.state-pad.error {
  color: var(--accent);
}

@media (max-width: 980px) {
  .split {
    grid-template-columns: 1fr;
  }
  .split-map {
    position: relative;
    top: 0;
  }
}

@media (max-width: 720px) {
  .head {
    flex-direction: column;
    align-items: flex-start;
  }
  .bar {
    flex-direction: column;
    align-items: stretch;
  }
  .bar-right {
    justify-content: space-between;
  }
}
</style>

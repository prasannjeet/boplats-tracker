# Airbnb-Style Split View Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the listings split view to look like Airbnb — full-viewport layout with a 2-column big-thumbnail card grid on the left and a Google Maps-style sticky map on the right.

**Architecture:** Create two new components (`HouseAirbnbCard`, `MapPopupCard`), update `HouseMap` for CartoDB tiles + home-icon individual pins + Vue-mounted rich popups, restructure `ListingsView` into a full-width split layout with windowed rendering, and change the default view to `split`.

**Tech Stack:** Vue 3 (Composition API, `createApp` for popup mounting), Leaflet + leaflet.markercluster, CartoDB Positron tile layer, IntersectionObserver for windowed rendering.

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `frontend/src/components/HouseAirbnbCard.vue` | Big-thumbnail Airbnb-style card for split-view left panel |
| Create | `frontend/src/components/MapPopupCard.vue` | Vue component mounted inside Leaflet popups; image carousel + details |
| Modify | `frontend/src/components/HouseMap.vue` | CartoDB tiles, home-icon pins, Vue-mounted popup lifecycle |
| Modify | `frontend/src/views/ListingsView.vue` | Full-width split layout, windowed rendering, default view = split |
| Modify | `frontend/src/composables/useFilters.ts` | Change `defaultFilters.view` from `'list'` to `'split'` |
| Modify | `frontend/src/styles/global.css` | Remove tile filter, add popup zero-margin rule |

---

## Task 1: Create `HouseAirbnbCard.vue`

**Files:**
- Create: `frontend/src/components/HouseAirbnbCard.vue`

- [ ] **Step 1: Create the component file**

```vue
<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import type { House } from '@/types/house';
import Carousel from './Carousel.vue';
import SaveButton from './SaveButton.vue';
import { competitionTier, galleryImages, locationLabel, shortHeadline } from '@/lib/derived';
import { formatArea, formatRent, formatRooms } from '@/lib/format';

const props = defineProps<{
  house: House;
  highlighted?: boolean;
}>();

const images = computed(() => galleryImages(props.house));
const headline = computed(() => shortHeadline(props.house));
const location = computed(() => locationLabel(props.house));
const tier = computed(() => competitionTier(props.house.queuePoints));
const detailRoute = computed(() => ({
  name: 'detail' as const,
  params: { internalId: String(props.house.internalId) },
}));
const queueLabel = computed(() =>
  props.house.queuePoints != null
    ? `${Math.round(props.house.queuePoints).toLocaleString('sv-SE')} pts`
    : null,
);
</script>

<template>
  <RouterLink :to="detailRoute" class="airbnb-card" :class="{ highlighted }">
    <div class="media">
      <Carousel :images="images" :alt="headline" cover />
      <span v-if="queueLabel" :class="['queue-chip', tier]">{{ queueLabel }}</span>
      <div class="save-wrap">
        <SaveButton :internal-id="house.internalId" :label="headline" />
      </div>
    </div>
    <div class="body">
      <h3 class="title">{{ headline }}</h3>
      <p class="loc">{{ location }}</p>
      <p class="meta">{{ formatRent(house.rent) }} · {{ formatRooms(house.rooms) }} · {{ formatArea(house.area) }}</p>
    </div>
  </RouterLink>
</template>

<style scoped>
.airbnb-card {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.airbnb-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lift);
  border-color: var(--line-strong);
}

.airbnb-card.highlighted {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 22%, transparent);
}

.media {
  position: relative;
}

/* Force Carousel aspect ratio to 16/10 (cover prop not enough on its own) */
.media :deep(.carousel) {
  aspect-ratio: 16 / 10;
  border-radius: 0;
}

.queue-chip {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 5px 11px;
  border-radius: var(--r-pill);
  background: var(--ink);
  color: var(--white);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  pointer-events: none;
}

.queue-chip.low {
  background: var(--surface-2);
  color: var(--ink);
  border: 1px solid var(--line-strong);
}

.queue-chip.medium {
  background: var(--ink-soft);
  color: var(--white);
}

.queue-chip.high,
.queue-chip.very-high {
  background: var(--accent);
  color: var(--white);
}

.queue-chip.unknown {
  display: none;
}

.save-wrap {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
}

.body {
  padding: 14px 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title {
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 900;
  letter-spacing: -0.01em;
  margin: 0;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.loc {
  margin: 0;
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  margin: 2px 0 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-soft);
}
</style>
```

- [ ] **Step 2: Verify TypeScript compiles (no new dependencies needed)**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend
pnpm tsc --noEmit
```

Expected: no errors related to HouseAirbnbCard.vue

---

## Task 2: Create `MapPopupCard.vue`

**Files:**
- Create: `frontend/src/components/MapPopupCard.vue`

This component is mounted via Vue's `createApp` into a Leaflet popup DOM element. It uses `useRouter` for navigation (the parent mounts it with `app.use(router)`).

- [ ] **Step 1: Create the component file**

```vue
<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import type { House } from '@/types/house';
import { competitionTier, galleryImages, locationLabel, shortHeadline } from '@/lib/derived';
import { formatArea, formatRent, formatRooms } from '@/lib/format';

const props = defineProps<{ house: House }>();

const router = useRouter();
const images = computed(() => galleryImages(props.house));
const headline = computed(() => shortHeadline(props.house));
const location = computed(() => locationLabel(props.house));
const tier = computed(() => competitionTier(props.house.queuePoints));
const queueLabel = computed(() =>
  props.house.queuePoints != null
    ? `${Math.round(props.house.queuePoints).toLocaleString('sv-SE')} pts`
    : null,
);

const imgIdx = ref(0);

function prev(e: MouseEvent) {
  e.stopPropagation();
  imgIdx.value = (imgIdx.value - 1 + images.value.length) % images.value.length;
}
function next(e: MouseEvent) {
  e.stopPropagation();
  imgIdx.value = (imgIdx.value + 1) % images.value.length;
}
function openDetail() {
  router.push({ name: 'detail', params: { internalId: String(props.house.internalId) } });
}
</script>

<template>
  <div class="popup-card" @click="openDetail" role="button" tabindex="0" @keydown.enter="openDetail">
    <div class="popup-media">
      <img v-if="images.length" :src="images[imgIdx]" :alt="headline" class="popup-img" loading="lazy" />
      <div v-else class="popup-img-placeholder" />
      <button v-if="images.length > 1" class="img-nav prev" type="button" @click="prev" aria-label="Previous image">‹</button>
      <button v-if="images.length > 1" class="img-nav next" type="button" @click="next" aria-label="Next image">›</button>
      <div v-if="images.length > 1" class="img-dots" aria-hidden="true">
        <span v-for="(_, i) in images" :key="i" :class="['dot', { on: i === imgIdx }]" />
      </div>
      <span v-if="queueLabel" :class="['popup-queue', tier]">{{ queueLabel }}</span>
    </div>
    <div class="popup-body">
      <h4 class="popup-title">{{ headline }}</h4>
      <p class="popup-loc">{{ location }}</p>
      <p class="popup-meta">{{ formatRent(house.rent) }} · {{ formatRooms(house.rooms) }} · {{ formatArea(house.area) }}</p>
      <span class="popup-cta">View details →</span>
    </div>
  </div>
</template>

<style scoped>
.popup-card {
  width: 280px;
  cursor: pointer;
  user-select: none;
}

.popup-media {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  background: var(--bg-soft);
  overflow: hidden;
}

.popup-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.popup-img-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--bg-soft), var(--surface-2));
}

.img-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(20, 17, 13, 0.6);
  color: var(--white);
  border: none;
  font-size: 18px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.15s ease;
  cursor: pointer;
}

.popup-media:hover .img-nav {
  opacity: 1;
}

.img-nav.prev { left: 8px; }
.img-nav.next { right: 8px; }

.img-dots {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 5px;
}

.img-dots .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
}

.img-dots .dot.on {
  background: var(--white);
  transform: scale(1.3);
}

.popup-queue {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 9px;
  border-radius: var(--r-pill);
  background: var(--ink);
  color: var(--white);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.popup-queue.low {
  background: var(--surface-2);
  color: var(--ink);
  border: 1px solid var(--line-strong);
}

.popup-queue.medium { background: var(--ink-soft); }
.popup-queue.high,
.popup-queue.very-high { background: var(--accent); }
.popup-queue.unknown { display: none; }

.popup-body {
  padding: 12px 14px 14px;
  background: var(--surface-2);
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.popup-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.popup-loc {
  margin: 0;
  font-size: 11px;
  color: var(--muted);
}

.popup-meta {
  margin: 2px 0 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--ink-soft);
}

.popup-cta {
  margin-top: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
}
</style>
```

- [ ] **Step 2: Verify TypeScript compiles**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend
pnpm tsc --noEmit
```

Expected: no errors related to MapPopupCard.vue

---

## Task 3: Update `HouseMap.vue`

**Files:**
- Modify: `frontend/src/components/HouseMap.vue`

Changes:
1. Switch tile layer to CartoDB Positron
2. Individual pin icon uses SVG home icon (no rent text)
3. Popup: create a `<div>`, mount `MapPopupCard` via `createApp`, clean up on close + unmount
4. Track mounted popup Vue apps in a `Map<number, VueApp>` for cleanup

- [ ] **Step 1: Replace the entire `HouseMap.vue` with the updated version**

```vue
<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch, computed } from 'vue';
import { createApp, type App as VueApp } from 'vue';
import L, { type Map as LMap, type Marker, type DivIcon, type MarkerClusterGroup } from 'leaflet';
import 'leaflet.markercluster';
import 'leaflet.markercluster/dist/MarkerCluster.css';
import router from '@/router';
import type { House } from '@/types/house';
import { competitionTier } from '@/lib/derived';
import MapPopupCard from './MapPopupCard.vue';

const props = defineProps<{ houses: House[]; highlightId?: number | null; height?: string }>();
const emit = defineEmits<{ (e: 'hover', id: number | null): void }>();

const mapEl = ref<HTMLDivElement | null>(null);
let mapInstance: LMap | null = null;
let clusterGroup: MarkerClusterGroup | null = null;
const markerByInternalId = new Map<number, Marker>();
const popupApps = new Map<number, VueApp>();

const containerStyle = computed(() => ({ height: props.height ?? '600px' }));

function clusterSizeClass(count: number): string {
  if (count >= 25) return 'cluster-lg';
  if (count >= 10) return 'cluster-md';
  return 'cluster-sm';
}

const HOME_SVG = `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>`;

function pinIcon(house: House, highlighted: boolean): DivIcon {
  const tier = competitionTier(house.queuePoints);
  const classes = ['pin', tier, highlighted ? 'highlight' : ''].filter(Boolean).join(' ');
  return L.divIcon({
    className: classes,
    html: HOME_SVG,
    iconSize: [40, 40],
    iconAnchor: [20, 20],
  });
}

function clusterIcon(count: number): DivIcon {
  const size = count >= 25 ? 52 : count >= 10 ? 46 : 40;
  return L.divIcon({
    className: `pin cluster ${clusterSizeClass(count)}`,
    html: `<span>${count}</span>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  });
}

function buildPopupEl(house: House): HTMLElement {
  const div = document.createElement('div');
  const app = createApp(MapPopupCard, { house });
  app.use(router);
  app.mount(div);
  popupApps.set(house.internalId, app);
  return div;
}

function unmountPopupApp(internalId: number) {
  const app = popupApps.get(internalId);
  if (app) {
    app.unmount();
    popupApps.delete(internalId);
  }
}

function rebuildMarkers() {
  for (const app of popupApps.values()) app.unmount();
  popupApps.clear();

  if (!mapInstance || !clusterGroup) return;
  clusterGroup.clearLayers();
  markerByInternalId.clear();

  const located = props.houses.filter((h) => h.latitude != null && h.longitude != null);
  const markers: Marker[] = [];

  for (const h of located) {
    const isHighlighted = props.highlightId === h.internalId;
    const m = L.marker([h.latitude as number, h.longitude as number], {
      icon: pinIcon(h, isHighlighted),
    });
    m.bindPopup(() => buildPopupEl(h), {
      maxWidth: 300,
      minWidth: 280,
      className: 'house-popup-wrapper',
    });
    m.on('popupclose', () => unmountPopupApp(h.internalId));
    m.on('mouseover', () => emit('hover', h.internalId));
    m.on('mouseout', () => emit('hover', null));
    markerByInternalId.set(h.internalId, m);
    markers.push(m);
  }

  clusterGroup.addLayers(markers);
  fitToMarkers();
}

function fitToMarkers() {
  if (!mapInstance) return;
  const located = props.houses.filter((h) => h.latitude != null && h.longitude != null);
  if (located.length === 0) {
    mapInstance.setView([56.8796, 14.8059], 11);
    return;
  }
  if (located.length === 1) {
    mapInstance.setView([located[0].latitude as number, located[0].longitude as number], 13);
    return;
  }
  const bounds = L.latLngBounds(located.map((h) => [h.latitude as number, h.longitude as number]));
  mapInstance.fitBounds(bounds, { padding: [40, 40], maxZoom: 14 });
}

onMounted(() => {
  if (!mapEl.value) return;
  mapInstance = L.map(mapEl.value, { zoomControl: true, attributionControl: true });
  L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors © <a href="https://carto.com/attributions">CARTO</a>',
    subdomains: ['a', 'b', 'c', 'd'],
    maxZoom: 19,
  }).addTo(mapInstance);

  clusterGroup = L.markerClusterGroup({
    showCoverageOnHover: false,
    spiderfyOnMaxZoom: true,
    zoomToBoundsOnClick: true,
    maxClusterRadius: 60,
    iconCreateFunction: (cluster) => clusterIcon(cluster.getChildCount()),
  });
  clusterGroup.addTo(mapInstance);
  rebuildMarkers();
});

onBeforeUnmount(() => {
  for (const app of popupApps.values()) app.unmount();
  popupApps.clear();
  if (mapInstance) {
    mapInstance.remove();
    mapInstance = null;
  }
  clusterGroup = null;
  markerByInternalId.clear();
});

watch(
  () => props.houses,
  () => rebuildMarkers(),
);

watch(
  () => props.highlightId,
  (next, prev) => {
    if (prev != null) {
      const m = markerByInternalId.get(prev);
      if (m) {
        const h = props.houses.find((x) => x.internalId === prev);
        if (h) m.setIcon(pinIcon(h, false));
      }
    }
    if (next != null) {
      const m = markerByInternalId.get(next);
      if (m) {
        const h = props.houses.find((x) => x.internalId === next);
        if (h) {
          m.setIcon(pinIcon(h, true));
          mapInstance?.panTo(m.getLatLng(), { animate: true });
        }
      }
    }
  },
);
</script>

<template>
  <div ref="mapEl" class="house-map" :style="containerStyle"></div>
</template>

<style>
.house-map {
  width: 100%;
  border-radius: var(--r-lg);
  overflow: hidden;
  border: 1px solid var(--line);
  background: var(--bg-soft);
  position: relative;
  isolation: isolate;
  z-index: 0;
}

.leaflet-marker-icon.pin {
  background: var(--ink);
  color: var(--white);
  border-radius: 999px;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 13px;
  border: 2px solid var(--surface-2);
  box-shadow: 0 4px 12px rgba(20, 17, 13, 0.35);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  cursor: pointer;
}

.leaflet-marker-icon.pin span {
  pointer-events: none;
  line-height: 1;
}

.leaflet-marker-icon.pin svg {
  pointer-events: none;
}

.leaflet-marker-icon.pin:hover {
  transform: scale(1.08);
}

.leaflet-marker-icon.pin.low {
  background: var(--surface-2);
  color: var(--ink);
  border-color: var(--ink);
}

.leaflet-marker-icon.pin.medium {
  background: var(--ink-soft);
  color: var(--white);
}

.leaflet-marker-icon.pin.high,
.leaflet-marker-icon.pin.very-high {
  background: var(--accent);
  color: var(--white);
  border-color: var(--white);
}

.leaflet-marker-icon.pin.unknown {
  background: rgba(20, 17, 13, 0.65);
  color: var(--white);
  border-color: var(--surface-2);
}

.leaflet-marker-icon.pin.highlight {
  transform: scale(1.25);
  box-shadow: 0 8px 24px rgba(231, 92, 77, 0.55);
  z-index: 1000 !important;
  border-color: var(--accent);
}

.leaflet-marker-icon.pin.cluster {
  background: var(--ink);
  color: var(--white);
  font-size: 14px;
  font-weight: 700;
  border: 3px solid var(--surface-2);
  box-shadow: 0 6px 18px rgba(20, 17, 13, 0.4);
}

.leaflet-marker-icon.pin.cluster.cluster-md {
  font-size: 15px;
  border-color: var(--accent);
}

.leaflet-marker-icon.pin.cluster.cluster-lg {
  font-size: 16px;
  background: var(--accent);
  border-color: var(--surface-2);
  box-shadow: 0 10px 26px rgba(231, 92, 77, 0.55);
}

.leaflet-cluster-anim .leaflet-marker-icon,
.leaflet-cluster-anim .leaflet-marker-shadow {
  transition: transform 0.3s ease-out, opacity 0.3s ease-in;
}

/* Rich popup — zero inner margin so MapPopupCard controls its own layout */
.house-popup-wrapper .leaflet-popup-content {
  margin: 0;
  padding: 0;
  width: auto !important;
}

.house-popup-wrapper .leaflet-popup-content-wrapper {
  padding: 0;
  border-radius: var(--r-md);
  overflow: hidden;
}
</style>
```

- [ ] **Step 2: Verify TypeScript compiles**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend
pnpm tsc --noEmit
```

Expected: no errors in HouseMap.vue

---

## Task 4: Update `global.css` — remove tile filter, add popup rules

**Files:**
- Modify: `frontend/src/styles/global.css`

- [ ] **Step 1: Remove the `.leaflet-tile` filter rule**

Find and delete these lines in `frontend/src/styles/global.css`:
```css
.leaflet-tile {
  filter: grayscale(0.45) sepia(0.18) hue-rotate(-12deg) saturate(0.7);
}
```

CartoDB Positron already looks clean and neutral — the filter is not needed.

- [ ] **Step 2: Update `.leaflet-popup-content` default margin**

The current rule:
```css
.leaflet-popup-content {
  margin: 12px 14px;
  font-size: 13px;
}
```

Keep this rule as-is. The `.house-popup-wrapper` override (added in Task 3's `<style>` block) overrides it specifically for house popups. The default margin stays for any other popups.

---

## Task 5: Update `ListingsView.vue` — Airbnb split layout with windowed rendering

**Files:**
- Modify: `frontend/src/views/ListingsView.vue`

This is the largest change. The existing split section is replaced with a full-width Airbnb-style layout. List and grid views are unaffected.

- [ ] **Step 1: Replace the entire `ListingsView.vue` with the updated version**

```vue
<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue';
import { useHouses } from '@/composables/useHouses';
import { useFilters, filterHouses, sortHouses, type ViewMode } from '@/composables/useFilters';
import SearchBar from '@/components/SearchBar.vue';
import QuickFilters from '@/components/QuickFilters.vue';
import HouseCard from '@/components/HouseCard.vue';
import HouseGridCard from '@/components/HouseGridCard.vue';
import HouseAirbnbCard from '@/components/HouseAirbnbCard.vue';
import HouseMap from '@/components/HouseMap.vue';
import FilterSheet from '@/components/FilterSheet.vue';
import { formatRelative } from '@/lib/format';

const { houses, freshness, load, loading, error } = useHouses();
const { filters, update, activeCount } = useFilters();

const sheetOpen = ref(false);
const hoverId = ref<number | null>(null);
const sentinel = ref<HTMLDivElement | null>(null);
const visibleCount = ref(20);

onMounted(() => {
  load();
  setupSentinel();
});

onBeforeUnmount(() => {
  sentinelObserver?.disconnect();
});

let sentinelObserver: IntersectionObserver | null = null;

function setupSentinel() {
  sentinelObserver = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting) {
        visibleCount.value += 20;
      }
    },
    { rootMargin: '200px' },
  );
  // Observe after DOM is ready; sentinel ref may not exist yet if view=split isn't active
  // The watcher on sentinel ref handles late attachment
}

watch(sentinel, (el) => {
  if (el && sentinelObserver) sentinelObserver.observe(el);
});

const now = computed(() => Date.now());

const filtered = computed(() => {
  const base = houses.value.filter((h) => h.endDate == null);
  const matched = filterHouses(base, filters.value, now.value);
  return sortHouses(matched, filters.value.sort, now.value);
});

const visibleHouses = computed(() => filtered.value.slice(0, visibleCount.value));

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

const viewModes: Array<{ key: ViewMode; label: string }> = [
  { key: 'list', label: 'List' },
  { key: 'grid', label: 'Grid' },
  { key: 'split', label: 'Split' },
];

const view = computed<ViewMode>(() => filters.value.view);
const lastFetched = computed(() => formatRelative(freshness.value?.toISOString() ?? null));
const isSplit = computed(() => view.value === 'split');

function setView(mode: ViewMode) {
  visibleCount.value = 20;
  update({ view: mode });
}

function setQuery(q: string) {
  update({ q });
}

// Re-import watch (it's used above so must be imported)
import { watch } from 'vue';
</script>

<template>
  <div class="listings" :class="{ 'is-split': isSplit }">
    <div class="page-inner">
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

      <!-- List and grid views stay inside page-inner -->
      <section v-if="filtered.length === 0 && !loading" class="empty">
        <h3>No listings match these filters.</h3>
        <p>Try widening your range or clearing the active filters.</p>
        <button type="button" class="btn ghost" @click="update({ q: '', cities: [], rooms: [], areaMin: null, areaMax: null, rentMin: null, rentMax: null, queueMin: null, queueMax: null, deadline: 'all', hasFloorplan: false, minImages: null, hasDescription: false, preset: 'all' })">
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

      <div v-if="loading && houses.length === 0" class="state-pad">Loading listings…</div>
      <div v-if="error" class="state-pad error">Couldn't load listings: {{ error }}</div>
    </div>

    <!-- Split view — full-width, outside page-inner -->
    <section v-if="view === 'split' && filtered.length > 0" class="split-airbnb">
      <div class="split-left">
        <HouseAirbnbCard
          v-for="h in visibleHouses"
          :key="h.internalId"
          :house="h"
          :highlighted="hoverId === h.internalId"
          @mouseenter="hoverId = h.internalId"
          @mouseleave="hoverId = null"
        />
        <div ref="sentinel" class="sentinel" aria-hidden="true" />
      </div>
      <div class="split-right">
        <HouseMap
          :houses="filtered"
          :highlight-id="hoverId"
          height="100%"
          @hover="(id) => (hoverId = id)"
        />
      </div>
    </section>

    <FilterSheet :open="sheetOpen" :match-count="filtered.length" @close="sheetOpen = false" />
  </div>
</template>

<style scoped>
/* Override .page global styles when in split mode */
.listings {
  max-width: var(--page-max);
  margin: 0 auto;
  padding: clamp(20px, 4vw, 48px) var(--page-pad) 96px;
}

.listings.is-split {
  max-width: 100%;
  padding: 0;
}

.page-inner {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* When in split mode, page-inner gets its own constrained padding */
.is-split .page-inner {
  max-width: var(--page-max);
  margin: 0 auto;
  padding: clamp(20px, 4vw, 48px) var(--page-pad) 24px;
}

/* --- Split layout --- */
.split-airbnb {
  display: grid;
  grid-template-columns: minmax(360px, 50%) 1fr;
  gap: 0;
  align-items: start;
}

.split-left {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  padding: 20px var(--page-pad) 48px;
  align-content: start;
}

.split-right {
  position: sticky;
  top: 66px; /* height of .app-header: 30px logo + 2×18px padding */
  height: calc(100vh - 66px);
}

.split-right :deep(.house-map) {
  height: 100%;
  border-radius: 0;
  border: none;
  border-left: 1px solid var(--line);
}

.sentinel {
  grid-column: 1 / -1;
  height: 1px;
}

/* --- Shared styles (same as before for list/grid) --- */
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
  .split-airbnb {
    grid-template-columns: 1fr;
  }
  .split-right {
    position: relative;
    top: 0;
    height: 60vw;
    min-height: 360px;
  }
  .split-right :deep(.house-map) {
    border: 1px solid var(--line);
    border-radius: var(--r-lg);
    height: 100%;
  }
  .split-left {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .split-left {
    grid-template-columns: 1fr;
  }
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
```

**Note:** The `import { watch } from 'vue'` at the bottom of `<script setup>` is invalid — Vue's `<script setup>` auto-imports from the same block. Remove that line; move `watch` into the initial `import { ..., watch }` at the top.

- [ ] **Step 2: Fix the imports — merge `watch` into the top import**

The `<script setup>` block should start with:
```ts
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue';
```

And remove the erroneous `import { watch } from 'vue'` at the bottom.

- [ ] **Step 3: Verify TypeScript compiles**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend
pnpm tsc --noEmit
```

Expected: no errors

---

## Task 6: Change default view to `split` in `useFilters.ts`

**Files:**
- Modify: `frontend/src/composables/useFilters.ts` line 59

- [ ] **Step 1: Change the default view**

In `frontend/src/composables/useFilters.ts`, change line 59:
```ts
// Before:
  view: 'list',
// After:
  view: 'split',
```

---

## Task 7: Smoke-test in browser

- [ ] **Step 1: Start the dev server**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend
pnpm dev
```

- [ ] **Step 2: Open the listings page and verify**

Check:
1. Default view is the split layout (two-column cards on left, map on right)
2. Cards show large thumbnails, queue-point chip on image (if available), rent/rooms/area below
3. Map uses CartoDB Positron tiles (light, clean, Google Maps-like)
4. Individual house pins show a home icon (no rent text); clusters show a number
5. Clicking a pin opens a popup with image carousel, details, "View details →"
6. Image prev/next works in popup without closing the popup
7. Clicking the popup card navigates to the detail view
8. Scrolling the left panel loads more cards (windowed rendering sentinel fires)
9. Hovering a card highlights the corresponding map pin (and vice versa)
10. List and Grid view modes still work correctly
11. Filter sheet still works

- [ ] **Step 3: Commit**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker
git add frontend/src/components/HouseAirbnbCard.vue \
        frontend/src/components/MapPopupCard.vue \
        frontend/src/components/HouseMap.vue \
        frontend/src/views/ListingsView.vue \
        frontend/src/composables/useFilters.ts \
        frontend/src/styles/global.css \
        docs/superpowers/plans/2026-05-13-airbnb-split-view.md
git commit -m "feat: Airbnb-style split view with big thumbnails, CartoDB map, and rich popups"
```

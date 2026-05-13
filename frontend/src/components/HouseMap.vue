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
    attribution:
      '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors © <a href="https://carto.com/attributions">CARTO</a>',
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

/* Rich popup — zero inner margin so MapPopupCard controls layout */
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

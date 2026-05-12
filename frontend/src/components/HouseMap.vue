<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch, computed } from 'vue';
import L, { type Map as LMap, type Marker, type DivIcon, type MarkerClusterGroup } from 'leaflet';
import 'leaflet.markercluster';
import 'leaflet.markercluster/dist/MarkerCluster.css';
import { useRouter } from 'vue-router';
import type { House } from '@/types/house';
import { competitionTier, shortHeadline } from '@/lib/derived';
import { formatRent } from '@/lib/format';

const props = defineProps<{ houses: House[]; highlightId?: number | null; height?: string }>();
const emit = defineEmits<{ (e: 'hover', id: number | null): void }>();

const router = useRouter();
const mapEl = ref<HTMLDivElement | null>(null);
let mapInstance: LMap | null = null;
let clusterGroup: MarkerClusterGroup | null = null;
const markerByInternalId = new Map<number, Marker>();

const containerStyle = computed(() => ({ height: props.height ?? '600px' }));

function clusterSizeClass(count: number): string {
  if (count >= 25) return 'cluster-lg';
  if (count >= 10) return 'cluster-md';
  return 'cluster-sm';
}

function pinIcon(house: House, highlighted: boolean): DivIcon {
  const tier = competitionTier(house.queuePoints);
  const label = house.rent ? Math.round(house.rent / 1000).toString() : '·';
  const classes = ['pin', tier, highlighted ? 'highlight' : ''].filter(Boolean).join(' ');
  return L.divIcon({
    className: classes,
    html: `<span>${label}</span>`,
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

function popupHtml(house: House): string {
  const headline = escapeHtml(shortHeadline(house));
  const rent = escapeHtml(formatRent(house.rent));
  const city = escapeHtml(house.city ?? '');
  return `
    <div class="pop">
      <strong>${headline}</strong>
      <div class="pop-sub">${rent}${city ? ` · ${city}` : ''}</div>
      <a data-internal-id="${house.internalId}" class="pop-link">Open detail →</a>
    </div>`;
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] ?? c));
}

function rebuildMarkers() {
  if (!mapInstance || !clusterGroup) return;
  clusterGroup.clearLayers();
  markerByInternalId.clear();
  const located = props.houses.filter((h) => h.latitude != null && h.longitude != null);
  const markers: Marker[] = [];
  for (const h of located) {
    const isHighlighted = props.highlightId === h.internalId;
    const m = L.marker([h.latitude as number, h.longitude as number], { icon: pinIcon(h, isHighlighted) });
    m.bindPopup(popupHtml(h));
    m.on('mouseover', () => emit('hover', h.internalId));
    m.on('mouseout', () => emit('hover', null));
    m.on('popupopen', (e) => {
      const link = (e.popup as L.Popup).getElement()?.querySelector('.pop-link') as HTMLAnchorElement | null;
      if (link) {
        link.addEventListener('click', (ev) => {
          ev.preventDefault();
          router.push({ name: 'detail', params: { internalId: String(h.internalId) } });
        });
      }
    });
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
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap',
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

/* Leaflet DivIcon assigns class `leaflet-marker-icon` + whatever we pass.
   Target `.pin` directly — the default `leaflet-div-icon` background is
   NOT applied because our className overrides the DivIcon default. */
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
  letter-spacing: -0.01em;
}

.leaflet-marker-icon.pin span {
  pointer-events: none;
  line-height: 1;
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

/* Cluster pins — larger circle, white outline, count inside */
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
  background: var(--ink);
  border-color: var(--accent);
}

.leaflet-marker-icon.pin.cluster.cluster-lg {
  font-size: 16px;
  background: var(--accent);
  border-color: var(--surface-2);
  box-shadow: 0 10px 26px rgba(231, 92, 77, 0.55);
}

/* MarkerCluster animations rely on this default class; keep transitions smooth. */
.leaflet-cluster-anim .leaflet-marker-icon,
.leaflet-cluster-anim .leaflet-marker-shadow {
  transition: transform 0.3s ease-out, opacity 0.3s ease-in;
}

.leaflet-popup-content .pop strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
  font-weight: 700;
}

.leaflet-popup-content .pop-sub {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 8px;
}

.leaflet-popup-content .pop-link {
  display: inline-block;
  font-weight: 600;
  font-size: 12px;
  color: var(--accent);
  cursor: pointer;
}
</style>

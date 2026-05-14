<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useHouses } from '@/composables/useHouses';
import { useSaved } from '@/composables/useSaved';
import HouseMap from '@/components/HouseMap.vue';
import HouseMiniCard from '@/components/HouseMiniCard.vue';
import {
  competitionLabel,
  competitionTier,
  galleryImages,
  hasFloorplan,
  locationLabel,
  parseAmenities,
  shortHeadline,
} from '@/lib/derived';
import {
  formatArea,
  formatCostPerM2,
  formatCountdown,
  formatDeadlineTimestamp,
  formatLongDate,
  formatRelative,
  formatRent,
  formatRooms,
  formatNumber,
  daysUntil,
} from '@/lib/format';

const props = defineProps<{ internalId: string }>();
const router = useRouter();
const { houses, load, loading } = useHouses();
const { isSaved, toggle } = useSaved();

const tab = ref<'photos' | 'plans'>('photos');
const photoIndex = ref(0);
const planIndex = ref(0);

onMounted(() => load());

const id = computed(() => Number(props.internalId));
const house = computed(() => houses.value.find((h) => h.internalId === id.value) ?? null);

interface HeroItem {
  src: string;
  label: string;
  isPdf: boolean;
  downloadName: string;
}

const photos = computed(() => (house.value ? galleryImages(house.value) : []));

const photoItems = computed<HeroItem[]>(() =>
  photos.value.map((src, i) => ({
    src,
    label: `${i + 1} / ${photos.value.length}`,
    isPdf: false,
    downloadName: `image-${i + 1}`,
  })),
);

const planItems = computed<HeroItem[]>(() => {
  const plans = house.value?.floorplans ?? [];
  return plans.map((p, i) => {
    const isPdf = (p.mimeType ?? '').toLowerCase().includes('pdf');
    const baseName = p.displayName?.replace(/[^\w.\- ]/g, '').trim() || `floorplan-${i + 1}`;
    return {
      src: p.address,
      label: p.displayName ?? `Floor plan ${i + 1}`,
      isPdf,
      downloadName: isPdf ? `${baseName}.pdf` : baseName,
    };
  });
});

const heroItems = computed<HeroItem[]>(() => (tab.value === 'plans' ? planItems.value : photoItems.value));

const heroIndex = computed({
  get: () => (tab.value === 'plans' ? planIndex.value : photoIndex.value),
  set: (v: number) => {
    if (tab.value === 'plans') planIndex.value = v;
    else photoIndex.value = v;
  },
});

const currentItem = computed<HeroItem | null>(() => heroItems.value[heroIndex.value] ?? null);

const headline = computed(() => (house.value ? shortHeadline(house.value) : ''));
const tier = computed(() => competitionTier(house.value?.queuePoints ?? null));
const days = computed(() => (house.value ? daysUntil(house.value.applicationDeadline) : null));
const countdownClass = computed(() => {
  if (days.value == null) return '';
  if (days.value < 0) return 'deadline-passed';
  if (days.value <= 3) return 'deadline-soon';
  return '';
});

const moveInDelta = computed(() => {
  if (!house.value?.availableFrom) return null;
  const d = daysUntil(house.value.availableFrom);
  if (d == null) return null;
  if (d < 0) return 'available now';
  if (d === 0) return 'today';
  if (d === 1) return '1 day';
  return `${d} days`;
});

const externalLink = computed(() => {
  if (!house.value?.id) return null;
  return `https://vidingehem.se/lediga-objekt/${house.value.id}`;
});

const googleMapsLink = computed(() => {
  if (!house.value?.completeAddress) return null;
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(house.value.completeAddress)}`;
});

const nearby = computed(() => {
  if (!house.value) return [];
  const city = house.value.city?.toLocaleLowerCase('sv-SE');
  return houses.value
    .filter((h) => h.internalId !== id.value && h.endDate == null && h.city?.toLocaleLowerCase('sv-SE') === city)
    .slice(0, 4);
});

const amenities = computed(() => parseAmenities(house.value?.includedJson));
const nrApplications = computed(() => house.value?.nrApplications ?? null);

const lastFetched = computed(() => formatRelative(house.value?.lastDetailFetchedAt));

const isEnded = computed(() => house.value?.endDate != null);

const breadcrumb = computed(() => {
  if (!house.value) return '';
  const parts = [house.value.city, house.value.areaName, house.value.floorDisplayName].filter(Boolean).join(' / ');
  return parts || locationLabel(house.value);
});

function setHeroIndex(i: number) {
  heroIndex.value = i;
}

function nextHero() {
  if (heroItems.value.length <= 1) return;
  heroIndex.value = (heroIndex.value + 1) % heroItems.value.length;
}

function prevHero() {
  if (heroItems.value.length <= 1) return;
  heroIndex.value = (heroIndex.value - 1 + heroItems.value.length) % heroItems.value.length;
}

function copyShare() {
  if (typeof navigator !== 'undefined' && navigator.clipboard) {
    navigator.clipboard.writeText(window.location.href).catch(() => {});
  }
}

function goBack() {
  if (window.history.length > 1) router.back();
  else router.push('/listings');
}
</script>

<template>
  <div class="detail page" v-if="house">
    <header class="detail-head">
      <button type="button" class="icon-btn" aria-label="Back" @click="goBack">
        <svg viewBox="0 0 24 24"><path d="M19 12H5" /><path d="m11 18-6-6 6-6" /></svg>
      </button>
      <div class="actions">
        <button type="button" class="icon-btn" aria-label="Share listing" @click="copyShare">
          <svg viewBox="0 0 24 24"><path d="M4 12v7a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-7" /><path d="M12 16V3" /><path d="m7 8 5-5 5 5" /></svg>
        </button>
        <button
          type="button"
          class="icon-btn"
          :class="{ active: isSaved(house.internalId) }"
          :aria-label="isSaved(house.internalId) ? 'Unsave listing' : 'Save listing'"
          @click="toggle(house.internalId)"
        >
          <svg viewBox="0 0 24 24"><path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" /></svg>
        </button>
      </div>
    </header>

    <section class="hero">
      <div class="hero-media">
        <div v-if="currentItem" class="hero-image" :class="{ 'is-pdf': currentItem.isPdf }">
          <object
            v-if="currentItem.isPdf"
            :data="`${currentItem.src}#toolbar=0&navpanes=0&view=FitH`"
            type="application/pdf"
            class="pdf-frame"
            :aria-label="`${currentItem.label} (PDF)`"
          >
            <div class="pdf-fallback">
              <svg viewBox="0 0 24 24" class="pdf-fallback-icon" aria-hidden="true">
                <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
                <path d="M14 3v6h6" />
              </svg>
              <p>PDF preview not available in this browser.</p>
              <a :href="currentItem.src" :download="currentItem.downloadName" target="_blank" rel="noreferrer noopener" class="btn solid">
                Download {{ currentItem.label }}
              </a>
            </div>
          </object>
          <img v-else :src="currentItem.src" :alt="`${headline} — ${currentItem.label}`" />

          <span class="hero-label">{{ currentItem.label }}</span>

          <a
            v-if="currentItem.isPdf"
            :href="currentItem.src"
            :download="currentItem.downloadName"
            target="_blank"
            rel="noreferrer noopener"
            class="download-pdf"
            :aria-label="`Download ${currentItem.label}`"
            :title="`Download ${currentItem.label}`"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 3v12" />
              <path d="m7 10 5 5 5-5" />
              <path d="M5 21h14" />
            </svg>
          </a>

          <button v-if="heroItems.length > 1" class="hero-nav prev" type="button" aria-label="Previous" @click="prevHero">
            <svg viewBox="0 0 24 24"><path d="m15 6-6 6 6 6" /></svg>
          </button>
          <button v-if="heroItems.length > 1" class="hero-nav next" type="button" aria-label="Next" @click="nextHero">
            <svg viewBox="0 0 24 24"><path d="m9 6 6 6-6 6" /></svg>
          </button>
        </div>
        <div v-else class="hero-image placeholder">No imagery available.</div>

        <div v-if="planItems.length > 0" class="hero-tabs">
          <button type="button" :class="{ active: tab === 'photos' }" @click="tab = 'photos'; setHeroIndex(0)">
            {{ photos.length }} photos
          </button>
          <button type="button" :class="{ active: tab === 'plans' }" @click="tab = 'plans'; setHeroIndex(0)">
            {{ planItems.length }} floor plan{{ planItems.length === 1 ? '' : 's' }}
          </button>
        </div>

        <div v-if="heroItems.length > 1" class="thumbs">
          <button
            v-for="(item, i) in heroItems"
            :key="item.src + i"
            type="button"
            :class="['thumb', { on: i === heroIndex, 'thumb-pdf': item.isPdf }]"
            @click="setHeroIndex(i)"
          >
            <img v-if="!item.isPdf" :src="item.src" alt="" loading="lazy" />
            <span v-else class="thumb-pdf-content" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
                <path d="M14 3v6h6" />
              </svg>
              <span class="thumb-pdf-label">PDF</span>
            </span>
          </button>
        </div>
      </div>

      <div class="title-block">
        <span class="kicker">{{ house.type ?? 'Residential' }} · {{ house.number ?? house.id }}</span>
        <h1 class="display title">
          {{ headline }}<span class="dot"></span>
        </h1>
        <p class="breadcrumb">{{ breadcrumb }}<span v-if="house.localId"> · Local id {{ house.localId }}</span></p>

        <div v-if="isEnded" class="ended-banner">
          This listing ended on {{ formatLongDate(house.endDate) }}. Historical data only.
        </div>

        <a v-if="externalLink" :href="externalLink" target="_blank" rel="noreferrer noopener" class="btn solid apply">
          How to apply
          <svg viewBox="0 0 24 24"><path d="M7 17 17 7" /><path d="M7 7h10v10" /></svg>
        </a>
      </div>
    </section>

    <section class="status-grid">
      <article class="status-card">
        <span class="round-num">I</span>
        <div>
          <strong>Application deadline</strong>
          <p class="sub">{{ formatDeadlineTimestamp(house.applicationDeadline) }}</p>
        </div>
        <span :class="['big', countdownClass]">{{ formatCountdown(house.applicationDeadline) }}</span>
      </article>

      <article class="status-card">
        <span class="round-num">II</span>
        <div>
          <strong>Move-in window</strong>
          <p class="sub">Available from {{ formatLongDate(house.availableFrom) }}</p>
        </div>
        <span class="big mono">{{ moveInDelta ?? '—' }}</span>
      </article>

      <article class="status-card">
        <span class="round-num">III</span>
        <div>
          <strong>Queue cut-off</strong>
          <p class="sub">{{ competitionLabel(tier) }}</p>
        </div>
        <span :class="['big', 'tier', tier]">{{ house.queuePoints != null ? `${formatNumber(house.queuePoints)} pts` : 'no cut-off yet' }}</span>
      </article>
    </section>

    <section class="panel">
      <h2 class="panel-title">Key facts</h2>
      <div class="facts">
        <div class="fact"><b>{{ formatRent(house.rent) }}</b><span>rent / month</span></div>
        <div class="fact"><b>{{ formatArea(house.area) }}</b><span>living area</span></div>
        <div class="fact"><b>{{ formatRooms(house.rooms) }}</b><span>layout</span></div>
        <div class="fact"><b>{{ formatCostPerM2(house.rent, house.area) }}</b><span>per m²</span></div>
        <div class="fact"><b>{{ house.floorDisplayName ?? '—' }}</b><span>floor</span></div>
        <div class="fact"><b>{{ house.areaName ?? '—' }}</b><span>area name</span></div>
        <div v-if="hasFloorplan(house)" class="fact"><b>Yes</b><span>floor plan</span></div>
        <div class="fact"><b>{{ photos.length }}</b><span>images</span></div>
        <div v-if="nrApplications != null" class="fact">
          <b>{{ nrApplications.toLocaleString('sv-SE') }}</b>
          <span>sökande</span>
        </div>
      </div>
    </section>

    <section v-if="amenities.length > 0" class="panel">
      <h2 class="panel-title">Ingår i hyran</h2>
      <div class="amenities">
        <span v-for="a in amenities" :key="a" class="amenity-chip">{{ a }}</span>
      </div>
    </section>

    <section v-if="house.description" class="panel">
      <h2 class="panel-title">Description</h2>
      <p class="copy">{{ house.description }}</p>
    </section>

    <section v-if="house.latitude != null && house.longitude != null" class="panel">
      <header class="panel-head">
        <h2 class="panel-title">Location</h2>
        <a v-if="googleMapsLink" :href="googleMapsLink" target="_blank" rel="noreferrer noopener" class="panel-link">
          Open in Google Maps →
        </a>
      </header>
      <HouseMap :houses="[house]" :highlight-id="house.internalId" height="320px" />
      <p class="sub addr">
        {{ house.completeAddress ?? house.address }}
        <span v-if="house.latitude" class="mono">· {{ house.latitude.toFixed(4) }}°N {{ house.longitude?.toFixed(4) }}°E</span>
      </p>
    </section>

    <section v-if="nearby.length > 0" class="panel">
      <header class="panel-head">
        <h2 class="panel-title">Nearby in {{ house.city }}</h2>
        <span class="mono">same area</span>
      </header>
      <div class="nearby">
        <HouseMiniCard v-for="n in nearby" :key="n.internalId" :house="n" />
      </div>
    </section>

    <section class="panel provenance">
      <h2 class="panel-title">Data provenance</h2>
      <p class="copy">
        Source: Vidingehem · Momentum API. Last detail fetch: {{ lastFetched }} ({{ formatRelative(house.lastDetailFetchedAt) }}).
        Listing instance <span class="mono">internalId={{ house.internalId }}</span> · external id <span class="mono">{{ house.id }}</span>.
      </p>
    </section>
  </div>

  <div v-else-if="loading" class="detail page state">Loading listing…</div>
  <div v-else class="detail page state">
    <h2>Listing not found.</h2>
    <p>It may have ended or never existed under this id.</p>
    <button class="btn ghost" type="button" @click="router.push('/listings')">Back to listings</button>
  </div>
</template>

<style scoped>
.detail {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.state {
  text-align: center;
  padding: 96px 24px;
  color: var(--muted);
}

.detail-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: -8px;
}

.actions {
  display: flex;
  gap: 10px;
}

.icon-btn.active {
  background: var(--accent);
  color: var(--white);
  border-color: var(--accent);
}

.icon-btn svg {
  width: 16px;
  height: 16px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.hero {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 36px;
  align-items: start;
}

.hero-media {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero-image {
  position: relative;
  aspect-ratio: 4 / 3;
  border-radius: var(--r-lg);
  overflow: hidden;
  background: var(--bg-soft);
}

.hero-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-image.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted);
}

.hero-image.is-pdf {
  background: var(--surface-2);
  aspect-ratio: 3 / 4;
}

.pdf-frame {
  width: 100%;
  height: 100%;
  border: none;
  background: var(--surface-2);
  display: block;
}

.pdf-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 32px;
  gap: 14px;
  text-align: center;
  color: var(--ink-soft);
}

.pdf-fallback-icon {
  width: 56px;
  height: 56px;
  stroke: var(--accent);
  fill: none;
  stroke-width: 1.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.pdf-fallback p {
  margin: 0;
  font-size: 14px;
  max-width: 320px;
}

.download-pdf {
  position: absolute;
  top: 14px;
  right: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--ink);
  color: var(--white);
  box-shadow: 0 8px 22px rgba(20, 17, 13, 0.35);
  transition: transform 0.15s ease, background 0.15s ease;
  z-index: 2;
}

.download-pdf:hover {
  transform: translateY(-1px) scale(1.05);
  background: var(--accent);
}

.download-pdf svg {
  width: 16px;
  height: 16px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.thumb-pdf {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-2);
  border: 1px solid var(--line-strong);
}

.thumb-pdf-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--accent);
}

.thumb-pdf-content svg {
  width: 22px;
  height: 22px;
  stroke: currentColor;
  fill: none;
  stroke-width: 1.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.thumb-pdf-label {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--ink);
}

.hero-label {
  position: absolute;
  left: 14px;
  bottom: 14px;
  padding: 6px 12px;
  border-radius: var(--r-pill);
  background: rgba(20, 17, 13, 0.78);
  color: var(--white);
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.06em;
}

.hero-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.45);
  background: rgba(20, 17, 13, 0.62);
  color: var(--white);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.hero-nav svg {
  width: 16px;
  height: 16px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2;
}

.hero-nav.prev {
  left: 14px;
}

.hero-nav.next {
  right: 14px;
}

.hero-tabs {
  display: inline-flex;
  align-self: flex-start;
  padding: 4px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-pill);
}

.hero-tabs button {
  padding: 8px 16px;
  border: none;
  background: transparent;
  border-radius: var(--r-pill);
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-soft);
}

.hero-tabs button.active {
  background: var(--ink);
  color: var(--white);
}

.thumbs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.thumbs::-webkit-scrollbar {
  height: 6px;
}

.thumbs::-webkit-scrollbar-thumb {
  background: var(--line-strong);
  border-radius: 999px;
}

.thumb {
  width: 96px;
  height: 72px;
  border-radius: var(--r-sm);
  overflow: hidden;
  border: 2px solid transparent;
  padding: 0;
  cursor: pointer;
  background: var(--bg-soft);
  flex-shrink: 0;
}

.thumb.on {
  border-color: var(--accent);
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.title-block {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.title {
  font-size: clamp(40px, 5.5vw, 72px);
  line-height: 0.95;
  margin: 4px 0;
}

.breadcrumb {
  color: var(--muted);
  margin: 0;
  font-size: 14px;
}

.ended-banner {
  margin-top: 8px;
  padding: 14px 18px;
  background: var(--accent);
  color: var(--white);
  border-radius: var(--r-md);
  font-weight: 600;
}

.apply {
  align-self: flex-start;
  margin-top: 14px;
  padding: 16px 24px;
  font-size: 15px;
}

.apply svg {
  width: 16px;
  height: 16px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.status-card {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
}

.round-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1.5px solid var(--ink);
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 600;
}

.status-card strong {
  display: block;
  font-size: 15px;
}

.status-card .sub {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--muted);
}

.big {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 900;
  letter-spacing: -0.01em;
}

.big.tier.low {
  color: var(--ink-soft);
}

.big.tier.medium {
  color: var(--ink);
}

.big.tier.high,
.big.tier.very-high {
  color: var(--accent);
}

.big.tier.unknown {
  color: var(--muted);
  font-size: 16px;
}

.panel {
  padding: 28px 32px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.panel-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 900;
  letter-spacing: -0.015em;
  margin: 0;
}

.panel-link {
  font-weight: 600;
  border-bottom: 2px solid var(--ink);
  padding-bottom: 2px;
  font-size: 13px;
}

.facts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
}

.facts .fact {
  background: var(--surface-2);
  border-radius: var(--r-md);
  padding: 14px 16px;
  border: 1px solid var(--line);
  flex-direction: column;
  align-items: flex-start;
}

.facts .fact b {
  font-size: 18px;
  font-weight: 700;
}

.facts .fact span {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--muted);
  margin-top: 4px;
}

.addr {
  margin: 0;
  font-size: 13px;
  color: var(--ink-soft);
}

.nearby {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.provenance .copy {
  color: var(--muted);
  font-size: 13px;
}

.amenities {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.amenity-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: var(--r-pill);
  background: var(--surface-2);
  border: 1px solid var(--line);
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}

@media (max-width: 960px) {
  .hero {
    grid-template-columns: 1fr;
  }
  .status-grid {
    grid-template-columns: 1fr;
  }
  .nearby {
    grid-template-columns: 1fr;
  }
  .panel {
    padding: 22px 18px;
  }
}
</style>

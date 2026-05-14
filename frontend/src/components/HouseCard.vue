<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import type { House } from '@/types/house';
import Carousel from './Carousel.vue';
import SaveButton from './SaveButton.vue';
import { competitionDisplay, competitionLabel, competitionTier, galleryImages, hasFloorplan, locationLabel, shortHeadline } from '@/lib/derived';
import { daysUntil, formatArea, formatCostPerM2, formatCountdown, formatRent, formatRooms, formatShortDate } from '@/lib/format';
import parkingPlaceholder from '@/assets/parking-placeholder.png';

const props = defineProps<{ house: House; highlighted?: boolean }>();

const headline = computed(() => shortHeadline(props.house));
const location = computed(() => locationLabel(props.house));
const tier = computed(() => competitionTier(props.house.queuePoints));
const tierLabel = computed(() => competitionLabel(tier.value));
const images = computed(() => {
  const imgs = galleryImages(props.house);
  if (imgs.length > 0) return imgs;
  if ((props.house.type ?? '').toLowerCase() === 'parking') return [parkingPlaceholder];
  return imgs;
});
const imageLabels = computed(() => images.value.map((_, i) => `${i + 1} / ${images.value.length}`));
const days = computed(() => daysUntil(props.house.applicationDeadline));
const countdownClass = computed(() => {
  if (days.value == null) return '';
  if (days.value < 0) return 'deadline-passed';
  if (days.value <= 3) return 'deadline-soon';
  return '';
});
const detailRoute = computed(() => ({ name: 'detail' as const, params: { internalId: String(props.house.internalId) } }));
const queueLabel = computed(() => competitionDisplay(props.house) ?? '—');
const typeLabel = computed(() => {
  const t = props.house.type ?? '';
  if (t.toLowerCase() === 'residential' || t === '') return null;
  return props.house.rentalObjectType ?? t;
});
const floorplanHint = computed(() => (hasFloorplan(props.house) ? 'floor plan included' : null));
const moveIn = computed(() => formatShortDate(props.house.availableFrom));
</script>

<template>
  <RouterLink :to="detailRoute" class="house-card" :class="{ highlighted }">
    <div class="card-media">
      <Carousel :images="images" :labels="imageLabels" :alt="headline" />
      <span v-if="typeLabel" class="card-type-badge">{{ typeLabel }}</span>
    </div>
    <div class="body">
      <header>
        <div class="title-block">
          <h3 class="title">{{ headline }}</h3>
          <p class="sub">{{ location }}</p>
        </div>
        <SaveButton :internal-id="house.internalId" :label="headline" />
      </header>

      <div class="fact-grid">
        <div class="fact"><b>{{ formatRent(house.rent) }}</b><span>monthly rent</span></div>
        <div class="fact"><b>{{ formatArea(house.area) }}</b><span>living area</span></div>
        <div class="fact"><b>{{ formatRooms(house.rooms) }}</b><span>residential</span></div>
        <div class="fact"><b>{{ formatCostPerM2(house.rent, house.area) }}</b><span>per m²</span></div>
      </div>

      <footer>
        <div>
          <span :class="['countdown', countdownClass]">{{ formatCountdown(house.applicationDeadline) }}</span>
          <p class="meta">Move-in {{ moveIn }} · {{ tierLabel }}<span v-if="floorplanHint"> · {{ floorplanHint }}</span></p>
        </div>
        <span :class="['queue-badge', tier]">{{ queueLabel }}</span>
      </footer>
    </div>
  </RouterLink>
</template>

<style scoped>
.house-card {
  display: grid;
  grid-template-columns: minmax(220px, 38%) 1fr;
  gap: 0;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  overflow: hidden;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.house-card:hover {
  transform: translateY(-2px);
  border-color: var(--line-strong);
  box-shadow: var(--shadow-card);
}

.house-card.highlighted {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 25%, transparent);
}

.house-card :deep(.carousel) {
  aspect-ratio: auto;
  height: 100%;
  min-height: 230px;
  border-radius: 0;
}

.body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px 24px;
}

header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.title-block {
  min-width: 0;
}

.title {
  font-family: var(--font-display);
  font-size: clamp(22px, 2.2vw, 30px);
  font-weight: 900;
  letter-spacing: -0.015em;
  line-height: 1;
  margin: 0 0 6px;
  color: var(--ink);
}

.sub {
  font-size: 13px;
  color: var(--muted);
  margin: 0;
}

footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-top: auto;
}

.countdown {
  font-weight: 600;
  font-size: 14px;
}

.meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--muted);
}

.queue-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--r-pill);
  background: var(--ink);
  color: var(--white);
  font-family: var(--font-mono);
  font-size: 12px;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.queue-badge.low {
  background: var(--surface-2);
  color: var(--ink);
  border: 1px solid var(--line-strong);
}

.queue-badge.medium {
  background: var(--ink-soft);
}

.queue-badge.high,
.queue-badge.very-high {
  background: var(--accent);
  border: 1px solid var(--accent);
}

.queue-badge.unknown {
  background: transparent;
  color: var(--muted);
  border: 1px dashed var(--line-strong);
}

.card-media {
  position: relative;
}

.card-type-badge {
  position: absolute;
  bottom: 10px;
  left: 12px;
  padding: 4px 10px;
  border-radius: var(--r-pill);
  background: rgba(0, 0, 0, 0.62);
  color: var(--white);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  pointer-events: none;
  z-index: 1;
}

@media (max-width: 720px) {
  .house-card {
    grid-template-columns: 1fr;
  }
  .house-card :deep(.carousel) {
    aspect-ratio: 16 / 10;
    min-height: 0;
  }
  .body {
    padding: 18px 18px 20px;
  }
}
</style>

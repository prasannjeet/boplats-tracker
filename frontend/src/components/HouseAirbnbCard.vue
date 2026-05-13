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

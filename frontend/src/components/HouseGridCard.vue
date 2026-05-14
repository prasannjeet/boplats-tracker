<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import type { House } from '@/types/house';
import SaveButton from './SaveButton.vue';
import Carousel from './Carousel.vue';
import { galleryImages, locationLabel, shortHeadline } from '@/lib/derived';
import parkingPlaceholder from '@/assets/parking-placeholder.png';
import { daysUntil, formatArea, formatCountdown, formatRent, formatRooms } from '@/lib/format';

const props = defineProps<{ house: House }>();

const headline = computed(() => shortHeadline(props.house));
const location = computed(() => locationLabel(props.house));
const detailRoute = computed(() => ({ name: 'detail' as const, params: { internalId: String(props.house.internalId) } }));
const images = computed(() => {
  const imgs = galleryImages(props.house);
  if (imgs.length > 0) return imgs;
  if ((props.house.type ?? '').toLowerCase() === 'parking') return [parkingPlaceholder];
  return imgs;
});
const typeLabel = computed(() => {
  const t = props.house.type ?? '';
  if (t.toLowerCase() === 'residential' || t === '') return null;
  return props.house.rentalObjectType ?? t;
});
const days = computed(() => daysUntil(props.house.applicationDeadline));
const countdownClass = computed(() => {
  if (days.value == null) return '';
  if (days.value < 0) return 'deadline-passed';
  if (days.value <= 3) return 'deadline-soon';
  return '';
});
</script>

<template>
  <RouterLink :to="detailRoute" class="grid-card">
    <div class="media">
      <Carousel :images="images" :alt="headline" :cover="false" />
      <span v-if="typeLabel" class="type-badge">{{ typeLabel }}</span>
      <div class="save-overlay">
        <SaveButton :internal-id="house.internalId" :label="headline" />
      </div>
    </div>
    <div class="body">
      <div class="row1">
        <h3 class="title">{{ headline }}</h3>
        <span :class="['countdown', countdownClass]">{{ formatCountdown(house.applicationDeadline) }}</span>
      </div>
      <p class="sub">{{ location }}</p>
      <div class="row2">
        <span class="chip">{{ formatRent(house.rent) }}</span>
        <span class="chip">{{ formatArea(house.area) }}</span>
        <span class="chip">{{ formatRooms(house.rooms) }}</span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.grid-card {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  overflow: hidden;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.grid-card:hover {
  transform: translateY(-3px);
  border-color: var(--line-strong);
  box-shadow: var(--shadow-card);
}

.media {
  position: relative;
}

.save-overlay {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
}

.body {
  padding: 16px 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.row1 {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.title {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -0.015em;
  margin: 0;
  line-height: 1;
}

.countdown {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  padding-top: 4px;
}

.sub {
  font-size: 13px;
  color: var(--muted);
  margin: 0;
}

.row2 {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.chip {
  padding: 4px 10px;
  border-radius: var(--r-pill);
  background: var(--bg-soft);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.type-badge {
  position: absolute;
  bottom: 8px;
  left: 10px;
  padding: 3px 9px;
  border-radius: var(--r-pill);
  background: rgba(0, 0, 0, 0.62);
  color: var(--white);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  pointer-events: none;
}
</style>

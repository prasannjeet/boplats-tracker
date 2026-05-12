<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import type { House } from '@/types/house';
import { hasFloorplan, locationLabel, primaryImage, shortHeadline } from '@/lib/derived';
import { formatArea, formatNumber, formatRooms } from '@/lib/format';

const props = defineProps<{ house: House; emphasis?: 'queue' | 'deadline' | 'rent' }>();

const headline = computed(() => shortHeadline(props.house));
const detailRoute = computed(() => ({ name: 'detail' as const, params: { internalId: String(props.house.internalId) } }));
const image = computed(() => primaryImage(props.house));
const headlineMetric = computed(() => {
  switch (props.emphasis) {
    case 'queue':
      return props.house.queuePoints != null ? `${formatNumber(props.house.queuePoints)} pts` : '— pts';
    default:
      return props.house.queuePoints != null ? `${formatNumber(props.house.queuePoints)} pts` : '— pts';
  }
});

const detailLine = computed(() => {
  const bits: string[] = [];
  bits.push(formatRooms(props.house.rooms));
  bits.push(formatArea(props.house.area));
  if (props.house.city) bits.push(props.house.city);
  if (hasFloorplan(props.house)) bits.push('floor plan included');
  return bits.join(' · ');
});
const loc = computed(() => locationLabel(props.house));
</script>

<template>
  <RouterLink :to="detailRoute" class="feature">
    <div class="media">
      <img v-if="image" :src="image" :alt="headline" loading="lazy" />
      <div v-else class="placeholder" aria-hidden="true" />
    </div>
    <div class="caption">
      <span class="metric">{{ headlineMetric }}</span>
      <h3 class="title">{{ headline }}</h3>
      <p class="meta">{{ detailLine }}</p>
      <p class="loc">{{ loc }}</p>
    </div>
  </RouterLink>
</template>

<style scoped>
.feature {
  display: flex;
  flex-direction: column;
  gap: 18px;
  border-radius: var(--r-lg);
  padding: 14px;
  background: var(--surface);
  border: 1px solid var(--line);
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.feature:hover {
  transform: translateY(-3px);
  border-color: var(--line-strong);
  box-shadow: var(--shadow-card);
}

.media {
  aspect-ratio: 4 / 3;
  border-radius: var(--r-md);
  overflow: hidden;
  background: var(--bg-soft);
}

.media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #2a261f, #14110d);
}

.caption {
  padding: 0 6px 10px;
}

.metric {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 600;
  color: var(--accent);
  letter-spacing: 0.04em;
  margin-bottom: 6px;
}

.title {
  font-family: var(--font-display);
  font-size: clamp(20px, 2vw, 28px);
  font-weight: 900;
  letter-spacing: -0.015em;
  margin: 0 0 6px;
  line-height: 1;
}

.meta {
  font-size: 13px;
  color: var(--ink-soft);
  margin: 0;
}

.loc {
  font-size: 12px;
  color: var(--muted);
  margin: 4px 0 0;
}
</style>

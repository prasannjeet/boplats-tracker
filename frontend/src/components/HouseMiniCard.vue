<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import type { House } from '@/types/house';
import { locationLabel, primaryImage, shortHeadline } from '@/lib/derived';
import { formatCountdown, formatRent, formatRooms } from '@/lib/format';

const props = defineProps<{ house: House; subtitle?: string }>();
const headline = computed(() => shortHeadline(props.house));
const image = computed(() => primaryImage(props.house));
const detailRoute = computed(() => ({ name: 'detail' as const, params: { internalId: String(props.house.internalId) } }));
const fallbackSubtitle = computed(
  () => `${formatRent(props.house.rent)} · ${formatRooms(props.house.rooms)} · ${formatCountdown(props.house.applicationDeadline)}`,
);
const location = computed(() => locationLabel(props.house));
</script>

<template>
  <RouterLink :to="detailRoute" class="mini">
    <div class="thumb">
      <img v-if="image" :src="image" :alt="headline" loading="lazy" />
      <div v-else class="placeholder" aria-hidden="true" />
    </div>
    <div class="text">
      <h4>{{ headline }}</h4>
      <p class="sub">{{ subtitle ?? fallbackSubtitle }}</p>
      <p class="loc">{{ location }}</p>
    </div>
    <span class="chev" aria-hidden="true">
      <svg viewBox="0 0 24 24"><path d="M5 12h14" /><path d="m13 6 6 6-6 6" /></svg>
    </span>
  </RouterLink>
</template>

<style scoped>
.mini {
  display: grid;
  grid-template-columns: 72px 1fr auto;
  align-items: center;
  gap: 14px;
  padding: 12px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  transition: background 0.15s ease, border-color 0.15s ease;
}

.mini:hover {
  background: var(--surface-2);
  border-color: var(--line-strong);
}

.thumb {
  width: 72px;
  height: 72px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--bg-soft);
  flex-shrink: 0;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--bg-soft), var(--surface-2));
}

.text {
  min-width: 0;
}

.text h4 {
  margin: 0 0 2px;
  font-size: 15px;
  font-weight: 700;
}

.sub {
  margin: 0;
  font-size: 12px;
  color: var(--ink-soft);
}

.loc {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--muted);
}

.chev {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--ink);
  color: var(--white);
}

.chev svg {
  width: 14px;
  height: 14px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>

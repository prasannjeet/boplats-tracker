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
  padding: 0;
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
  display: inline-block;
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

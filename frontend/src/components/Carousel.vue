<script setup lang="ts">
import { ref, computed } from 'vue';

const props = defineProps<{
  images: string[];
  labels?: string[];
  alt?: string;
  cover?: boolean;
}>();

const index = ref(0);

const safeImages = computed(() => (props.images && props.images.length > 0 ? props.images : []));
const currentLabel = computed(() => props.labels?.[index.value]);

function next() {
  if (safeImages.value.length === 0) return;
  index.value = (index.value + 1) % safeImages.value.length;
}
function prev() {
  if (safeImages.value.length === 0) return;
  index.value = (index.value - 1 + safeImages.value.length) % safeImages.value.length;
}
function go(i: number) {
  index.value = i;
}
function stop(e: Event) {
  e.preventDefault();
  e.stopPropagation();
}
</script>

<template>
  <div class="carousel" :class="{ cover }">
    <template v-if="safeImages.length > 0">
      <img :src="safeImages[index]" :alt="alt ?? 'Listing image'" loading="lazy" decoding="async" />
      <span v-if="currentLabel" class="label">{{ currentLabel }}</span>
      <button
        v-if="safeImages.length > 1"
        class="nav prev"
        type="button"
        aria-label="Previous image"
        @click="(e) => { stop(e); prev(); }"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m15 6-6 6 6 6" /></svg>
      </button>
      <button
        v-if="safeImages.length > 1"
        class="nav next"
        type="button"
        aria-label="Next image"
        @click="(e) => { stop(e); next(); }"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 6 6 6-6 6" /></svg>
      </button>
      <div v-if="safeImages.length > 1" class="dots" aria-hidden="true">
        <button
          v-for="(_, i) in safeImages"
          :key="i"
          type="button"
          :class="{ on: i === index }"
          @click="(e) => { stop(e); go(i); }"
          :aria-label="`Go to image ${i + 1}`"
        />
      </div>
    </template>
    <div v-else class="placeholder" aria-hidden="true">
      <svg viewBox="0 0 24 24"><path d="M4 6h16v12H4z" /><path d="m4 16 5-5 4 4 3-3 4 4" /><circle cx="9" cy="10" r="1.5" /></svg>
    </div>
  </div>
</template>

<style scoped>
.carousel {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: var(--r-md);
  overflow: hidden;
  background: var(--bg-soft);
  isolation: isolate;
}

.carousel.cover {
  aspect-ratio: 16 / 10;
}

.carousel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: opacity 0.2s ease;
}

.carousel .label {
  position: absolute;
  left: 12px;
  bottom: 12px;
  padding: 5px 10px;
  border-radius: var(--r-pill);
  background: rgba(20, 17, 13, 0.78);
  color: var(--white);
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.05em;
  text-transform: lowercase;
}

.carousel .nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(20, 17, 13, 0.55);
  color: var(--white);
  opacity: 0;
  transition: opacity 0.15s ease, background 0.15s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.carousel:hover .nav {
  opacity: 1;
}

.carousel .nav svg {
  width: 14px;
  height: 14px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.carousel .nav.prev {
  left: 10px;
}
.carousel .nav.next {
  right: 10px;
}

.carousel .dots {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
}

.carousel .dots button {
  width: 6px;
  height: 6px;
  padding: 0;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.55);
  cursor: pointer;
}

.carousel .dots button.on {
  background: var(--white);
  transform: scale(1.35);
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--line-strong);
}

.placeholder svg {
  width: 60px;
  height: 60px;
  stroke: currentColor;
  fill: none;
  stroke-width: 1.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>

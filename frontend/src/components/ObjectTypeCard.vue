<!-- frontend/src/components/ObjectTypeCard.vue -->
<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import type { ObjectType } from '@/types/objectType';
import { typeIcon } from '@/lib/derived';

const props = defineProps<{ objectType: ObjectType }>();

const priceRange = computed(() => {
  const { minPrice, maxPrice } = props.objectType;
  if (minPrice == null && maxPrice == null) return null;
  const fmt = (n: number) => Math.round(n).toLocaleString('sv-SE');
  if (minPrice != null && maxPrice != null) {
    if (minPrice === maxPrice) return `${fmt(minPrice)} kr/mån`;
    return `${fmt(minPrice)}–${fmt(maxPrice)} kr/mån`;
  }
  if (minPrice != null) return `från ${fmt(minPrice)} kr/mån`;
  if (maxPrice != null && maxPrice > 0) return `upp till ${fmt(maxPrice)} kr/mån`;
  return null;
});

const sizeRange = computed(() => {
  const { minRooms, maxRooms, minSize, maxSize } = props.objectType;
  const parts: string[] = [];
  if (minRooms != null && maxRooms != null && minRooms !== maxRooms) {
    parts.push(`${minRooms}–${maxRooms} rum`);
  }
  if (minSize != null && maxSize != null && maxSize > 0 && minSize !== maxSize) {
    parts.push(`${Math.round(minSize)}–${Math.round(maxSize)} m²`);
  }
  return parts.join(' · ') || null;
});

const listingsLink = computed(() => `/listings?types=${props.objectType.typeId}`);
const detailLink = computed(() => `/types/${props.objectType.typeId}`);
</script>

<template>
  <div class="ot-card">
    <div class="ot-icon" aria-hidden="true">{{ typeIcon(objectType.typeId) }}</div>
    <div class="ot-body">
      <h3 class="ot-name">{{ objectType.displayName ?? objectType.typeId }}</h3>
      <p v-if="objectType.numberOfMarketObjects != null" class="ot-count">
        {{ objectType.numberOfMarketObjects.toLocaleString('sv-SE') }} objekt
      </p>
      <p v-if="priceRange" class="ot-meta">{{ priceRange }}</p>
      <p v-if="sizeRange" class="ot-meta">{{ sizeRange }}</p>
    </div>
    <div class="ot-actions">
      <RouterLink :to="listingsLink" class="btn solid ot-btn">Visa listings</RouterLink>
      <RouterLink :to="detailLink" class="ot-link">Mer info →</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.ot-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px 20px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-card);
  transition: box-shadow 0.15s, border-color 0.15s;
}

.ot-card:hover {
  box-shadow: var(--shadow-lift);
  border-color: var(--line-strong);
}

.ot-icon {
  font-size: 28px;
  line-height: 1;
}

.ot-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.ot-name {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 900;
  margin: 0;
  color: var(--ink);
}

.ot-count {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
  margin: 0;
}

.ot-meta {
  font-size: 12px;
  color: var(--muted);
  margin: 0;
}

.ot-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ot-btn {
  text-align: center;
  padding: 10px 16px;
  font-size: 14px;
}

.ot-link {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-soft);
  text-align: center;
  padding: 4px 0;
}

.ot-link:hover {
  color: var(--ink);
}
</style>

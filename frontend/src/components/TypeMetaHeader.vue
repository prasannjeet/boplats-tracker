<!-- frontend/src/components/TypeMetaHeader.vue -->
<script setup lang="ts">
import { computed } from 'vue';
import type { ObjectType } from '@/types/objectType';
import { typeIcon } from '@/lib/derived';

const props = defineProps<{ objectType: ObjectType }>();

const priceRange = computed(() => {
  const { minPrice, maxPrice } = props.objectType;
  if (minPrice == null && maxPrice == null) return null;
  const fmt = (n: number) => Math.round(n).toLocaleString('sv-SE');
  if (minPrice != null && maxPrice != null && minPrice !== maxPrice) {
    return `${fmt(minPrice)}–${fmt(maxPrice)} kr/mån`;
  }
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
</script>

<template>
  <header class="type-header">
    <div class="type-icon" aria-hidden="true">{{ typeIcon(objectType.typeId) }}</div>
    <div class="type-body">
      <span class="kicker">Boendetyp</span>
      <h1 class="display type-title">{{ objectType.displayName ?? objectType.typeId }}<span class="dot"></span></h1>
      <p v-if="objectType.description" class="type-desc">{{ objectType.description }}</p>
      <dl v-if="objectType.numberOfMarketObjects != null || priceRange || sizeRange" class="type-facts">
        <div v-if="objectType.numberOfMarketObjects != null" class="fact-row">
          <dt>Antal objekt</dt>
          <dd>{{ objectType.numberOfMarketObjects.toLocaleString('sv-SE') }}</dd>
        </div>
        <div v-if="priceRange" class="fact-row">
          <dt>Prisintervall</dt>
          <dd>{{ priceRange }}</dd>
        </div>
        <div v-if="sizeRange" class="fact-row">
          <dt>Storlek</dt>
          <dd>{{ sizeRange }}</dd>
        </div>
      </dl>
    </div>
  </header>
</template>

<style scoped>
.type-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 24px;
  align-items: start;
  padding-top: 24px;
}

.type-icon {
  font-size: 56px;
  line-height: 1;
  padding-top: 8px;
}

.type-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.type-title {
  font-size: clamp(40px, 6vw, 80px);
  line-height: 0.92;
  margin: 8px 0 0;
}

.type-desc {
  max-width: 680px;
  font-size: 15px;
  line-height: 1.6;
  color: var(--ink-soft);
  margin: 0;
}

.type-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 0 32px;
  margin: 0;
  padding: 16px 0;
  border-top: 1px solid var(--line);
}

.fact-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 120px;
}

.fact-row dt {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--muted);
}

.fact-row dd {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}

@media (max-width: 600px) {
  .type-header {
    grid-template-columns: 1fr;
  }
  .type-icon {
    font-size: 40px;
    padding-top: 0;
  }
}
</style>

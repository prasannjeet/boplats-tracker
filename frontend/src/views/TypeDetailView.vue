<!-- frontend/src/views/TypeDetailView.vue -->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchObjectType } from '@/services/objectTypes';
import { useHouses } from '@/composables/useHouses';
import { sortHouses } from '@/composables/useFilters';
import TypeMetaHeader from '@/components/TypeMetaHeader.vue';
import HouseCard from '@/components/HouseCard.vue';
import type { ObjectType } from '@/types/objectType';

const props = defineProps<{ typeId: string }>();
const router = useRouter();

const { houses, load, loading } = useHouses();
const objectType = ref<ObjectType | null>(null);
const typeLoading = ref(true);
const typeNotFound = ref(false);

onMounted(async () => {
  load();
  try {
    const result = await fetchObjectType(props.typeId);
    if (result === null) {
      typeNotFound.value = true;
    } else {
      objectType.value = result;
    }
  } finally {
    typeLoading.value = false;
  }
});

const typedHouses = computed(() => {
  const active = houses.value.filter(
    (h) => h.endDate == null && (h.type ?? '') === props.typeId,
  );
  return sortHouses(active, 'deadline', Date.now());
});
</script>

<template>
  <div class="type-detail page">
    <div v-if="typeLoading || (loading && houses.length === 0)" class="state-pad">
      Loading…
    </div>

    <div v-else-if="typeNotFound" class="state-pad error">
      <h2>Property type not found.</h2>
      <button class="btn ghost" type="button" @click="router.push('/')">Go home</button>
    </div>

    <template v-else-if="objectType">
      <TypeMetaHeader :object-type="objectType" />

      <section class="listings-section">
        <div class="list-head">
          <strong>{{ typedHouses.length }} aktiva objekt</strong>
        </div>

        <div v-if="typedHouses.length > 0" class="list-stack">
          <HouseCard v-for="h in typedHouses" :key="h.internalId" :house="h" />
        </div>

        <div v-else class="empty">
          <p>Inga aktiva objekt för denna typ just nu.</p>
          <button class="btn ghost" type="button" @click="router.push('/listings')">
            Visa alla listings
          </button>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.type-detail {
  display: flex;
  flex-direction: column;
  gap: 48px;
}

.listings-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.list-head strong {
  font-size: 18px;
  font-weight: 700;
}

.list-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.empty {
  padding: 48px;
  text-align: center;
  background: var(--surface);
  border: 1px dashed var(--line-strong);
  border-radius: var(--r-lg);
}

.empty p {
  color: var(--muted);
  margin: 0 0 16px;
}

.state-pad {
  padding: 48px 0;
  text-align: center;
  color: var(--muted);
}

.state-pad.error {
  color: var(--accent);
}
</style>

import { ref, computed } from 'vue';
import { fetchHouses } from '@/services/houses';
import type { House } from '@/types/house';

const houses = ref<House[]>([]);
const loading = ref(false);
const loaded = ref(false);
const error = ref<string | null>(null);
const lastLoadedAt = ref<Date | null>(null);

async function load(force = false) {
  if (loading.value) return;
  if (loaded.value && !force) return;
  loading.value = true;
  error.value = null;
  try {
    houses.value = await fetchHouses();
    loaded.value = true;
    lastLoadedAt.value = new Date();
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e);
  } finally {
    loading.value = false;
  }
}

const freshness = computed<Date | null>(() => {
  if (houses.value.length === 0) return lastLoadedAt.value;
  let newest: number | null = null;
  for (const h of houses.value) {
    if (!h.lastDetailFetchedAt) continue;
    const t = new Date(h.lastDetailFetchedAt).getTime();
    if (newest == null || t > newest) newest = t;
  }
  return newest != null ? new Date(newest) : lastLoadedAt.value;
});

const cities = computed<string[]>(() => {
  const set = new Set<string>();
  for (const h of houses.value) {
    if (h.city) set.add(normaliseCity(h.city));
  }
  return Array.from(set).sort();
});

function normaliseCity(value: string): string {
  return value.charAt(0).toLocaleUpperCase('sv-SE') + value.slice(1).toLocaleLowerCase('sv-SE');
}

export function useHouses() {
  return {
    houses,
    loading,
    loaded,
    error,
    freshness,
    cities,
    load,
  };
}

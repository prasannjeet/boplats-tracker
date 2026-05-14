import { ref } from 'vue';
import { fetchObjectTypes } from '@/services/objectTypes';
import type { ObjectType } from '@/types/objectType';

const objectTypes = ref<ObjectType[]>([]);
const loading = ref(false);
const loaded = ref(false);
const error = ref<string | null>(null);

async function load(force = false) {
  if (loading.value) return;
  if (loaded.value && !force) return;
  loading.value = true;
  error.value = null;
  try {
    objectTypes.value = await fetchObjectTypes();
    loaded.value = true;
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e);
  } finally {
    loading.value = false;
  }
}

export function useObjectTypes() {
  return { objectTypes, loading, loaded, error, load };
}

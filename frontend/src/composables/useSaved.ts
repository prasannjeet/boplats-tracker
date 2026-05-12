import { ref, computed, watch } from 'vue';

const STORAGE_KEY = 'boplats:saved:v1';

function readInitial(): Set<number> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return new Set();
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return new Set(parsed.filter((v) => typeof v === 'number'));
  } catch {
    /* ignore */
  }
  return new Set();
}

const saved = ref<Set<number>>(readInitial());

watch(
  saved,
  (next) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(Array.from(next)));
  },
  { deep: true },
);

function toggle(internalId: number) {
  const next = new Set(saved.value);
  if (next.has(internalId)) next.delete(internalId);
  else next.add(internalId);
  saved.value = next;
}

function isSaved(internalId: number): boolean {
  return saved.value.has(internalId);
}

const count = computed(() => saved.value.size);

export function useSaved() {
  return { saved, count, toggle, isSaved };
}

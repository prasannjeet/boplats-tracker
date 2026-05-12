<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import { useHouses } from '@/composables/useHouses';
import { useSaved } from '@/composables/useSaved';
import HouseCard from '@/components/HouseCard.vue';

const { houses, load } = useHouses();
const { saved } = useSaved();

onMounted(() => load());

const items = computed(() => houses.value.filter((h) => saved.value.has(h.internalId)));
</script>

<template>
  <div class="page saved">
    <header class="head">
      <span class="kicker">Bookmarks</span>
      <h1 class="display title">
        Saved listings<span class="dot"></span>
      </h1>
      <p class="lead">Stored on this device only. Clear cookies and they disappear.</p>
    </header>

    <section v-if="items.length === 0" class="empty">
      <h2>You haven't saved anything yet.</h2>
      <p>Tap the bookmark on any listing to keep it here.</p>
      <RouterLink to="/listings" class="btn solid">Browse listings</RouterLink>
    </section>

    <section v-else class="list-stack">
      <HouseCard v-for="h in items" :key="h.internalId" :house="h" />
    </section>
  </div>
</template>

<style scoped>
.saved {
  display: flex;
  flex-direction: column;
  gap: 36px;
}

.title {
  font-size: clamp(56px, 8vw, 110px);
  line-height: 0.92;
  margin-top: 6px;
}

.lead {
  font-size: 15px;
  color: var(--ink-soft);
  max-width: 480px;
  margin: 0;
}

.empty {
  padding: 64px 48px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--r-lg);
  background: var(--surface);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.empty h2 {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 900;
  margin: 0;
}

.empty p {
  color: var(--muted);
  margin: 0 0 12px;
}

.list-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
</style>

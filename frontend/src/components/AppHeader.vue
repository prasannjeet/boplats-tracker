<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { useSaved } from '@/composables/useSaved';

const route = useRoute();
const { count } = useSaved();

const links = [
  { to: '/', label: 'Home', name: 'home' },
  { to: '/listings', label: 'All listings', name: 'listings' },
  { to: '/listings?view=split', label: 'Map', name: 'map' },
];

const isActive = (linkName: string) => {
  if (linkName === 'home') return route.name === 'home';
  if (linkName === 'map') return route.name === 'listings' && route.query.view === 'split';
  if (linkName === 'listings') return route.name === 'listings' && route.query.view !== 'split';
  return false;
};

const navHash = computed(() => Date.now()); // unused, keeps reactive references aligned
void navHash;
</script>

<template>
  <header class="app-header">
    <RouterLink to="/" class="brand" aria-label="Boplats Tracker home">
      <span class="logo" aria-hidden="true">b</span>
      <span class="brand-name">Boplats Tracker</span>
    </RouterLink>

    <nav class="nav-pills" aria-label="Primary">
      <RouterLink
        v-for="link in links"
        :key="link.name"
        :to="link.to"
        :class="['nav-pill', { active: isActive(link.name) }]"
      >
        {{ link.label }}
      </RouterLink>
    </nav>

    <RouterLink to="/saved" class="saved-pill" :aria-label="`Saved listings (${count})`">
      <span>Saved</span>
      <span class="saved-count">{{ count }}</span>
    </RouterLink>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px var(--page-pad);
  background: color-mix(in srgb, var(--bg) 92%, transparent);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 15px;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1.5px solid var(--ink);
  font-family: var(--font-display);
  font-size: 15px;
  background: var(--surface-2);
}

.brand-name {
  letter-spacing: -0.01em;
}

.nav-pills {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px;
  border-radius: var(--r-pill);
  background: var(--surface);
  border: 1px solid var(--line);
}

.nav-pill {
  padding: 8px 18px;
  border-radius: var(--r-pill);
  font-size: 14px;
  font-weight: 500;
  color: var(--ink-soft);
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-pill:hover {
  background: var(--bg-soft);
  color: var(--ink);
}

.nav-pill.active {
  background: var(--ink);
  color: var(--white);
  font-weight: 600;
}

.saved-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px 8px 18px;
  border-radius: var(--r-pill);
  background: var(--surface);
  border: 1px solid var(--line);
  font-size: 14px;
  font-weight: 600;
}

.saved-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  background: var(--accent);
  color: var(--white);
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 800px) {
  .nav-pills {
    display: none;
  }
  .brand-name {
    display: none;
  }
}
</style>

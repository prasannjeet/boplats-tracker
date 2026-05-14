<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import { useHouses } from '@/composables/useHouses';
import { useFilters } from '@/composables/useFilters';
import HouseCard from '@/components/HouseCard.vue';
import HouseFeatureCard from '@/components/HouseFeatureCard.vue';
import { daysUntil, formatRelative } from '@/lib/format';
import { filterHouses } from '@/composables/useFilters';
import ObjectTypeCard from '@/components/ObjectTypeCard.vue';
import { useObjectTypes } from '@/composables/useObjectTypes';

const { houses, freshness, load, loading, error } = useHouses();
const { filters } = useFilters();
const { objectTypes, load: loadObjectTypes } = useObjectTypes();

onMounted(() => {
  load();
  loadObjectTypes();
});

const now = computed(() => Date.now());

const active = computed(() => houses.value.filter((h) => h.endDate == null));

const stats = computed(() => {
  const all = active.value;
  const matched = filterHouses(all, filters.value, now.value);
  const cities = new Set<string>();
  let withCoords = 0;
  for (const h of all) {
    if (h.city) cities.add(h.city.toLocaleLowerCase('sv-SE'));
    if (h.latitude != null && h.longitude != null) withCoords++;
  }
  return {
    total: all.length,
    matched: matched.length,
    cities: cities.size,
    withCoords,
  };
});

const endingSoon = computed(() => {
  return active.value
    .filter((h) => {
      const d = daysUntil(h.applicationDeadline, new Date(now.value));
      return d != null && d >= 0 && d <= 7;
    })
    .sort((a, b) => new Date(a.applicationDeadline as string).getTime() - new Date(b.applicationDeadline as string).getTime())
    .slice(0, 3);
});

const highestQueue = computed(() => {
  return active.value
    .filter((h) => h.queuePoints != null)
    .sort((a, b) => (b.queuePoints as number) - (a.queuePoints as number))
    .slice(0, 3);
});

const browseTypes = computed(() =>
  objectTypes.value.filter((ot) => (ot.numberOfMarketObjects ?? 0) > 0),
);

const lastFetched = computed(() => formatRelative(freshness.value?.toISOString() ?? null));
</script>

<template>
  <div class="home page">
    <section class="hero">
      <div class="hero-text">
        <span class="kicker">Växjö region rental intelligence</span>
        <h1 class="display hero-title">
          Find the home<br />
          that closes<br />
          next<span class="dot"></span>
        </h1>
        <p class="hero-copy">
          Search active rental listings with deadlines, queue pressure, floor plans, move-in dates, and map-aware
          filters in one responsive catalogue.
        </p>
      </div>

      <aside class="hero-card" aria-label="At a glance">
        <header class="hero-card-head">
          <span class="kicker">At a glance</span>
          <span class="hero-card-dot" aria-hidden="true"></span>
        </header>
        <dl class="hero-card-list">
          <div class="hero-card-row">
            <dt>Region</dt>
            <dd>Växjö · Lammhult · Rottne</dd>
          </div>
          <div class="hero-card-row">
            <dt>Rent range</dt>
            <dd>3 000–12 500 kr / month</dd>
          </div>
          <div class="hero-card-row">
            <dt>Deadlines</dt>
            <dd>Next 7 days highlighted</dd>
          </div>
        </dl>
        <RouterLink to="/listings" class="btn solid hero-cta">
          Browse {{ active.length }} listings
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14" /><path d="m13 6 6 6-6 6" /></svg>
        </RouterLink>
      </aside>
    </section>

    <section class="stat-row" aria-label="Catalogue overview">
      <div class="stat">
        <strong class="stat-value">{{ stats.total }}</strong>
        <span class="stat-label">active homes</span>
      </div>
      <div class="stat">
        <strong class="stat-value">{{ stats.matched }}</strong>
        <span class="stat-label">match current filters</span>
      </div>
      <div class="stat">
        <strong class="stat-value">{{ stats.cities }}</strong>
        <span class="stat-label">cities with listings</span>
      </div>
      <div class="stat">
        <strong class="stat-value">{{ lastFetched }}</strong>
        <span class="stat-label">latest detail fetch</span>
      </div>
    </section>

    <section v-if="endingSoon.length > 0" class="section">
      <header class="section-head">
        <div>
          <span class="kicker">Ending soon</span>
          <h2 class="display section-title">
            Apply before the<br />
            queue closes<span class="dot"></span>
          </h2>
        </div>
        <RouterLink to="/listings?deadline=next7" class="section-link">View all listings →</RouterLink>
      </header>
      <div class="cards-stack">
        <HouseCard v-for="h in endingSoon" :key="h.internalId" :house="h" />
      </div>
    </section>

    <section v-if="highestQueue.length > 0" class="section">
      <header class="section-head">
        <div>
          <span class="kicker">Most in demand</span>
          <h2 class="display section-title">
            Highest queue-point<br />
            pressure<span class="dot"></span>
          </h2>
        </div>
        <p class="section-aside">
          Useful when you want to understand which homes are attractive but difficult to win.
        </p>
      </header>
      <div class="feature-row">
        <HouseFeatureCard v-for="h in highestQueue" :key="h.internalId" :house="h" emphasis="queue" />
      </div>
    </section>

    <section v-if="browseTypes.length >= 2" class="section">
      <header class="section-head">
        <div>
          <span class="kicker">Alla typer</span>
          <h2 class="display section-title">
            Bläddra efter<br />
            typ<span class="dot"></span>
          </h2>
        </div>
      </header>
      <div class="type-grid">
        <ObjectTypeCard v-for="ot in browseTypes" :key="ot.typeId" :object-type="ot" />
      </div>
    </section>

    <div v-if="loading && houses.length === 0" class="state-pad">Loading listings…</div>
    <div v-if="error" class="state-pad error">Couldn't load listings: {{ error }}</div>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 88px;
}

.hero {
  display: grid;
  grid-template-columns: 1fr minmax(280px, 380px);
  gap: 48px;
  align-items: start;
  padding-top: 24px;
}

.hero-text {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-title {
  font-size: clamp(64px, 9vw, 132px);
  line-height: 0.92;
}

.hero-copy {
  max-width: 540px;
  font-size: 17px;
  line-height: 1.55;
  color: var(--ink-soft);
  margin: 0;
}

.hero-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  padding: 24px 26px 22px;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-card);
  margin-top: 96px;
}

.hero-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  margin-bottom: 4px;
}

.hero-card-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--accent);
}

.hero-card-list {
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.hero-card-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 14px;
  padding: 14px 0;
  border-top: 1px solid var(--line);
}

.hero-card-row:first-child {
  border-top: none;
  padding-top: 8px;
}

.hero-card-row dt {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
  font-weight: 500;
}

.hero-card-row dd {
  margin: 0;
  font-weight: 600;
  font-size: 14px;
  text-align: right;
  color: var(--ink);
}

.hero-cta {
  margin-top: 14px;
  padding: 16px 20px;
  font-size: 15px;
}

.hero-cta svg {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 32px 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.stat-value {
  display: block;
  font-family: var(--font-display);
  font-size: clamp(32px, 4vw, 56px);
  font-weight: 900;
  letter-spacing: -0.02em;
  line-height: 1;
  color: var(--accent);
}

.stat-label {
  margin-top: 8px;
  display: block;
  font-size: 13px;
  color: var(--ink-soft);
}

.section {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.section-head {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 24px;
  align-items: end;
}

.section-title {
  font-size: clamp(40px, 6vw, 72px);
  line-height: 0.92;
  margin: 12px 0 0;
}

.section-link {
  font-weight: 600;
  border-bottom: 2px solid var(--ink);
  padding-bottom: 2px;
  white-space: nowrap;
}

.section-aside {
  margin: 0;
  max-width: 360px;
  font-size: 14px;
  color: var(--ink-soft);
}

.cards-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.feature-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 22px;
}

.state-pad {
  padding: 32px 0;
  color: var(--muted);
  text-align: center;
}

.state-pad.error {
  color: var(--accent);
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

@media (max-width: 960px) {
  .hero {
    grid-template-columns: 1fr;
  }
  .hero-card {
    margin-top: 0;
  }
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .feature-row {
    grid-template-columns: 1fr;
  }
  .section-head {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .stat-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>

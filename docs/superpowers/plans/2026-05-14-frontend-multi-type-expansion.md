# Frontend Multi-Type Property Expansion — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand the Vue 3 frontend to handle all property types now returned by the backend (apartments, parking, student housing, senior care, etc.) — previously the API was residential-only; it now returns ~1,297 items across 6+ types. Add a type-selector chip row, type-aware cards and map markers, amenities display in the detail view, a "browse by type" home page section, and a new `/types/:typeId` detail page.

**Architecture:** A new Spring `ObjectTypeController` exposes `GET /api/object-types` (list) and `GET /api/object-types/{typeId}` (single) — the `object_type` table is already synced by `HouseSyncService`. On the Vue side, a `useObjectTypes` composable loads this data once. The `House` TypeScript interface gains 4 new backend fields (`queueType`, `rentalObjectType`, `nrApplications`, `includedJson`). A `types: string[]` filter is added to `useFilters` (default `['residential']` — residential-only on first load). A new `TypeSelectorRow.vue` chip bar sits above the existing quick-filter row in `ListingsView`. All three card variants receive: a parking-photo placeholder, a non-residential type badge, and `nrApplications` display when queue points are absent. Map markers get type-aware SVG icons. `DetailView` gets an amenity-chips panel and an applicant count. `HomeView` gains a "browse by type" section. A new `/types/:typeId` route shows an `ObjectType` metadata header above a filtered listing of that type.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JPA (`ObjectTypeRepository` already exists); Vue 3.5, TypeScript, Vue Router 4, Leaflet 1.9.4. Backend build: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-25.jdk/Contents/Home && mymvn …`. Frontend: `cd frontend && npm run type-check` (vue-tsc) for compile-time verification.

---

## File Map

| Action | File |
|---|---|
| Create | `src/main/java/com/prasannjeet/vaxjobostader/controller/ObjectTypeController.java` |
| Create | `src/test/java/com/prasannjeet/vaxjobostader/controller/ObjectTypeControllerTest.java` |
| Create | `frontend/src/assets/parking-placeholder.png` (copy from Downloads) |
| Create | `frontend/src/types/objectType.ts` |
| Create | `frontend/src/services/objectTypes.ts` |
| Create | `frontend/src/composables/useObjectTypes.ts` |
| Modify | `frontend/src/types/house.ts` |
| Modify | `frontend/src/composables/useFilters.ts` |
| Modify | `frontend/src/lib/derived.ts` |
| Create | `frontend/src/components/TypeSelectorRow.vue` |
| Modify | `frontend/src/views/ListingsView.vue` |
| Modify | `frontend/src/components/HouseAirbnbCard.vue` |
| Modify | `frontend/src/components/HouseCard.vue` |
| Modify | `frontend/src/components/HouseGridCard.vue` |
| Modify | `frontend/src/components/HouseMap.vue` |
| Modify | `frontend/src/views/DetailView.vue` |
| Create | `frontend/src/components/ObjectTypeCard.vue` |
| Modify | `frontend/src/views/HomeView.vue` |
| Create | `frontend/src/components/TypeMetaHeader.vue` |
| Create | `frontend/src/views/TypeDetailView.vue` |
| Modify | `frontend/src/router/index.ts` |

---

## Task 1: Backend — ObjectTypeController

**Files:**
- Create: `src/main/java/com/prasannjeet/vaxjobostader/controller/ObjectTypeController.java`
- Create: `src/test/java/com/prasannjeet/vaxjobostader/controller/ObjectTypeControllerTest.java`

Context: `ObjectTypeRepository extends JpaRepository<ObjectType, String>` already exists. `ObjectType` entity has fields: `typeId` (PK String), `displayName`, `description`, `minPrice`, `maxPrice`, `minRooms`, `maxRooms`, `minSize`, `maxSize`, `numberOfMarketObjects`, `lastSyncedAt`. `HouseController` (in the same package) is the pattern to follow: `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor`.

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/prasannjeet/vaxjobostader/controller/ObjectTypeControllerTest.java
package com.prasannjeet.vaxjobostader.controller;

import com.prasannjeet.vaxjobostader.jpa.ObjectType;
import com.prasannjeet.vaxjobostader.jpa.ObjectTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ObjectTypeController.class)
class ObjectTypeControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ObjectTypeRepository repository;

    @Test
    void list_returnsAllObjectTypes() throws Exception {
        ObjectType type = new ObjectType(
            "residential", "Bostad", null, null, null, null, null, null, null, 169, null);
        when(repository.findAll()).thenReturn(List.of(type));

        mvc.perform(get("/api/object-types").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].typeId").value("residential"))
            .andExpect(jsonPath("$[0].displayName").value("Bostad"))
            .andExpect(jsonPath("$[0].numberOfMarketObjects").value(169));
    }

    @Test
    void getOne_unknownTypeId_returns404() throws Exception {
        when(repository.findById("unknown")).thenReturn(Optional.empty());

        mvc.perform(get("/api/object-types/unknown").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void getOne_knownTypeId_returnsType() throws Exception {
        ObjectType type = new ObjectType(
            "parking", "Parkering", null, null, null, null, null, null, null, 831, null);
        when(repository.findById("parking")).thenReturn(Optional.of(type));

        mvc.perform(get("/api/object-types/parking").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.typeId").value("parking"))
            .andExpect(jsonPath("$.displayName").value("Parkering"));
    }
}
```

Run to confirm failure:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-25.jdk/Contents/Home
mymvn test -Dtest=ObjectTypeControllerTest --no-transfer-progress 2>&1 | tail -10
```

Expected: compilation error — `ObjectTypeController` does not exist yet.

- [ ] **Step 2: Create ObjectTypeController**

```java
// src/main/java/com/prasannjeet/vaxjobostader/controller/ObjectTypeController.java
package com.prasannjeet.vaxjobostader.controller;

import com.prasannjeet.vaxjobostader.jpa.ObjectType;
import com.prasannjeet.vaxjobostader.jpa.ObjectTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/object-types")
@RequiredArgsConstructor
public class ObjectTypeController {

    private final ObjectTypeRepository objectTypeRepository;

    @GetMapping
    public List<ObjectType> list() {
        return objectTypeRepository.findAll();
    }

    @GetMapping("/{typeId}")
    public ResponseEntity<ObjectType> getOne(@PathVariable String typeId) {
        return objectTypeRepository.findById(typeId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

- [ ] **Step 3: Run the tests**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-25.jdk/Contents/Home
mymvn test -Dtest=ObjectTypeControllerTest --no-transfer-progress 2>&1 | tail -10
```

Expected: `BUILD SUCCESS` — all 3 tests pass.

- [ ] **Step 4: Run the full test suite to confirm no regressions**

```bash
mymvn test --no-transfer-progress 2>&1 | tail -15
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/prasannjeet/vaxjobostader/controller/ObjectTypeController.java \
        src/test/java/com/prasannjeet/vaxjobostader/controller/ObjectTypeControllerTest.java
git commit -m "feat: expose /api/object-types and /api/object-types/{typeId} endpoints"
```

---

## Task 2: Frontend asset — parking placeholder image

**Files:**
- Create: `frontend/src/assets/parking-placeholder.png`

The image exists at `/Users/PRASI/Downloads/parking.png`. Copy it into the frontend assets folder (create the folder if it doesn't exist).

- [ ] **Step 1: Copy the image**

```bash
mkdir -p /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend/src/assets
cp /Users/PRASI/Downloads/parking.png \
   /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend/src/assets/parking-placeholder.png
```

Expected: file exists at `frontend/src/assets/parking-placeholder.png`.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/assets/parking-placeholder.png
git commit -m "feat: add parking placeholder image asset"
```

---

## Task 3: Frontend types and service — ObjectType interface + API client

**Files:**
- Create: `frontend/src/types/objectType.ts`
- Create: `frontend/src/services/objectTypes.ts`

Context: The pattern for types is in `frontend/src/types/house.ts`. The pattern for API calls is in `frontend/src/services/houses.ts` which does a plain `fetch('/api/…')` and throws on non-ok.

- [ ] **Step 1: Create the ObjectType TypeScript interface**

```typescript
// frontend/src/types/objectType.ts
export interface ObjectType {
  typeId: string;
  displayName: string | null;
  description: string | null;
  minPrice: number | null;
  maxPrice: number | null;
  minRooms: number | null;
  maxRooms: number | null;
  minSize: number | null;
  maxSize: number | null;
  numberOfMarketObjects: number | null;
  lastSyncedAt: string | null;
}
```

- [ ] **Step 2: Create the objectTypes service**

```typescript
// frontend/src/services/objectTypes.ts
import type { ObjectType } from '@/types/objectType';

export async function fetchObjectTypes(): Promise<ObjectType[]> {
  const res = await fetch('/api/object-types');
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

export async function fetchObjectType(typeId: string): Promise<ObjectType | null> {
  const res = await fetch(`/api/object-types/${encodeURIComponent(typeId)}`);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}
```

- [ ] **Step 3: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/objectType.ts frontend/src/services/objectTypes.ts
git commit -m "feat: add ObjectType interface and objectTypes API service"
```

---

## Task 4: Frontend composable — useObjectTypes

**Files:**
- Create: `frontend/src/composables/useObjectTypes.ts`

Context: `useHouses` in `frontend/src/composables/useHouses.ts` is the exact pattern to follow — module-level refs so data is shared across all component instances, `load(force?)` guards against duplicate fetches.

- [ ] **Step 1: Create useObjectTypes**

```typescript
// frontend/src/composables/useObjectTypes.ts
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
```

- [ ] **Step 2: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/composables/useObjectTypes.ts
git commit -m "feat: add useObjectTypes composable"
```

---

## Task 5: Extend House interface and useFilters with type filter

**Files:**
- Modify: `frontend/src/types/house.ts`
- Modify: `frontend/src/composables/useFilters.ts`

### Part A — house.ts

The backend `House` entity already exposes `queueType`, `rentalObjectType`, `nrApplications`, and `includedJson` via `GET /api/houses` but the TypeScript interface doesn't declare them. Add them after `queuePoints`.

- [ ] **Step 1: Add 4 fields to the House interface**

In `frontend/src/types/house.ts`, after the `queuePoints` line:

```typescript
  queuePoints: number | null;
  queueType: string | null;
  rentalObjectType: string | null;
  nrApplications: number | null;
  includedJson: string | null;
```

The full updated block (lines 43–44 area in the original) becomes:

```typescript
  imageUrl: string | null;
  queuePoints: number | null;
  queueType: string | null;
  rentalObjectType: string | null;
  nrApplications: number | null;
  includedJson: string | null;
  lastDetailFetchedAt: string | null;
```

### Part B — useFilters.ts

Add `types: string[]` to `Filters`, default to `['residential']`, parse/serialize in URL, filter in `filterHouses`, count in `activeCount`.

- [ ] **Step 2: Add `types` to the Filters interface and defaultFilters**

In `frontend/src/composables/useFilters.ts`, add to the `Filters` interface after `preset`:

```typescript
  preset: QuickPreset;
  types: string[];
```

Add to `defaultFilters` after `preset: 'all'`:

```typescript
  preset: 'all',
  types: ['residential'],
```

- [ ] **Step 3: Update parseFilters to read the types param**

In `parseFilters`, add after the `preset` line:

```typescript
    preset: (getString(query.preset, defaultFilters.preset) as QuickPreset) ?? defaultFilters.preset,
    types: query.types !== undefined ? getList(query.types) : defaultFilters.types,
```

(Replace the existing `preset` line with the two lines above.)

- [ ] **Step 4: Update serializeFilters to write the types param**

In `serializeFilters`, add before the `return out` line:

```typescript
  const isDefaultTypes = f.types.length === 1 && f.types[0] === 'residential';
  if (!isDefaultTypes) out.types = f.types.join(',');
```

- [ ] **Step 5: Add type filtering to filterHouses**

In `filterHouses`, add as the **first** filter check (before the text search):

```typescript
    if (f.types.length && !f.types.includes(h.type ?? '')) return false;
```

- [ ] **Step 6: Count the types filter in activeCount**

In the `activeCount` computed in `useFilters()`, add after the `if (f.preset !== 'all') n++;` line:

```typescript
    const isDefaultTypes = f.types.length === 1 && f.types[0] === 'residential';
    if (!isDefaultTypes) n++;
```

- [ ] **Step 7: Update the "clear filters" call in ListingsView to reset types**

In `frontend/src/views/ListingsView.vue`, the empty-state clear button calls `update({...})` with an inline object. Add `types: ['residential']` to that object:

```vue
@click="update({ q: '', cities: [], rooms: [], areaMin: null, areaMax: null, rentMin: null, rentMax: null, queueMin: null, queueMax: null, deadline: 'all', hasFloorplan: false, minImages: null, hasDescription: false, preset: 'all', types: ['residential'] })"
```

- [ ] **Step 8: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 9: Commit**

```bash
git add frontend/src/types/house.ts frontend/src/composables/useFilters.ts frontend/src/views/ListingsView.vue
git commit -m "feat: extend House interface with new fields; add types filter to useFilters"
```

---

## Task 6: derived.ts additions — type icons, competition display, amenity parser

**Files:**
- Modify: `frontend/src/lib/derived.ts`

Add four new exports to `frontend/src/lib/derived.ts` at the bottom of the file.

- [ ] **Step 1: Add typeIcon, typeSvgIcon, competitionDisplay, parseAmenities**

Append to `frontend/src/lib/derived.ts`:

```typescript
export function typeIcon(typeId: string | null | undefined): string {
  switch ((typeId ?? '').toLowerCase()) {
    case 'parking': return 'P';
    case 'student': return 'S';
    case 'trygghetsboende': return 'T';
    case 'residential': return '⌂';
    default: return '⌂';
  }
}

export function typeSvgIcon(typeId: string | null | undefined): string {
  const t = (typeId ?? '').toLowerCase();
  if (t === 'parking') {
    return `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M13 3H6v18h4v-6h3c3.31 0 6-2.69 6-6s-2.69-6-6-6zm.2 8H10V7h3.2c1.1 0 2 .9 2 2s-.9 2-2 2z"/></svg>`;
  }
  return `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>`;
}

export function competitionDisplay(house: Pick<House, 'queuePoints' | 'nrApplications'>): string | null {
  if (house.queuePoints != null) {
    return `${Math.round(house.queuePoints).toLocaleString('sv-SE')} pts`;
  }
  if (house.nrApplications != null) {
    return `${house.nrApplications.toLocaleString('sv-SE')} sökande`;
  }
  return null;
}

export function parseAmenities(includedJson: string | null | undefined): string[] {
  if (!includedJson) return [];
  try {
    const parsed = JSON.parse(includedJson);
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((item: unknown) => {
        if (typeof item === 'object' && item !== null && 'displayName' in item) {
          return String((item as { displayName: unknown }).displayName);
        }
        return '';
      })
      .filter(Boolean);
  } catch {
    return [];
  }
}
```

Note: `House` is already imported at the top of derived.ts — `competitionDisplay` uses a `Pick<House, …>` parameter so no additional import is needed.

- [ ] **Step 2: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/lib/derived.ts
git commit -m "feat: add typeIcon, typeSvgIcon, competitionDisplay, parseAmenities to derived.ts"
```

---

## Task 7: TypeSelectorRow component

**Files:**
- Create: `frontend/src/components/TypeSelectorRow.vue`

This chip bar sits above the quick-filter row in `ListingsView`. Each chip represents one `ObjectType`. Clicking toggles that type in/out of `filters.types`. "Alla typer" sets `types: []` (no restriction). An ℹ link navigates to `/types/:typeId`.

- [ ] **Step 1: Create TypeSelectorRow.vue**

```vue
<!-- frontend/src/components/TypeSelectorRow.vue -->
<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import { useObjectTypes } from '@/composables/useObjectTypes';
import { useFilters } from '@/composables/useFilters';
import { typeIcon } from '@/lib/derived';

const { objectTypes, load } = useObjectTypes();
const { filters, update } = useFilters();

onMounted(() => load());

const activeTypes = computed(() => filters.value.types);
const isAllActive = computed(() => activeTypes.value.length === 0);

function toggleType(typeId: string) {
  const current = activeTypes.value;
  const next = current.includes(typeId)
    ? current.filter((t) => t !== typeId)
    : [...current, typeId];
  update({ types: next });
}

function selectAll() {
  update({ types: [] });
}
</script>

<template>
  <div class="type-row" role="toolbar" aria-label="Filter by property type">
    <button type="button" :class="['type-chip', { active: isAllActive }]" @click="selectAll">
      Alla typer
    </button>
    <span v-for="ot in objectTypes" :key="ot.typeId" class="type-chip-group">
      <button
        type="button"
        :class="['type-chip', { active: !isAllActive && activeTypes.includes(ot.typeId) }]"
        @click="toggleType(ot.typeId)"
      >
        <span class="type-icon" aria-hidden="true">{{ typeIcon(ot.typeId) }}</span>
        {{ ot.displayName ?? ot.typeId }}
        <span v-if="ot.numberOfMarketObjects != null" class="type-count">{{ ot.numberOfMarketObjects }}</span>
      </button>
      <RouterLink
        :to="`/types/${ot.typeId}`"
        class="type-info-link"
        :title="`Om ${ot.displayName ?? ot.typeId}`"
        aria-label="Mer info"
      >ℹ</RouterLink>
    </span>
  </div>
</template>

<style scoped>
.type-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.type-chip-group {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.type-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: var(--r-pill);
  background: var(--surface-2);
  border: 1px solid var(--line);
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-soft);
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.type-chip:hover {
  background: var(--bg-soft);
  color: var(--ink);
}

.type-chip.active {
  background: var(--ink);
  color: var(--white);
  border-color: var(--ink);
}

.type-icon {
  font-size: 12px;
}

.type-count {
  font-size: 11px;
  font-family: var(--font-mono);
  opacity: 0.7;
}

.type-info-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  font-size: 11px;
  color: var(--muted);
  text-decoration: none;
  transition: background 0.12s, color 0.12s;
}

.type-info-link:hover {
  color: var(--ink);
  background: var(--surface-2);
}
</style>
```

- [ ] **Step 2: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/TypeSelectorRow.vue
git commit -m "feat: add TypeSelectorRow component for property type filtering"
```

---

## Task 8: Wire TypeSelectorRow into ListingsView

**Files:**
- Modify: `frontend/src/views/ListingsView.vue`

- [ ] **Step 1: Import TypeSelectorRow and useObjectTypes**

In the `<script setup>` block of `ListingsView.vue`, add the two imports after the existing imports:

```typescript
import TypeSelectorRow from '@/components/TypeSelectorRow.vue';
import { useObjectTypes } from '@/composables/useObjectTypes';
```

Add this line after `const { houses, freshness, load, loading, error } = useHouses();`:

```typescript
const { load: loadObjectTypes } = useObjectTypes();
```

In `onMounted`, call both loaders:

```typescript
onMounted(() => {
  load();
  loadObjectTypes();
  sentinelObserver = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting) visibleCount.value += 20;
    },
    { rootMargin: '200px' },
  );
});
```

- [ ] **Step 2: Insert TypeSelectorRow in the template**

In the template, between `<SearchBar … />` and `<QuickFilters … />`, insert:

```vue
      <SearchBar :model-value="filters.q" @update:model-value="setQuery" @open-filters="sheetOpen = true" />
      <TypeSelectorRow />
      <QuickFilters :value="filters.preset" @update="(v) => update({ preset: v })" />
```

- [ ] **Step 3: Add type filter section to FilterSheet.vue**

The FilterSheet is the secondary place for the type filter (for discoverability). In `frontend/src/components/FilterSheet.vue`, add the `useObjectTypes` import:

```typescript
import { useObjectTypes } from '@/composables/useObjectTypes';
```

After the existing `const { cities } = useHouses();` line, add:

```typescript
const { objectTypes, load: loadObjectTypes } = useObjectTypes();
onMounted(() => loadObjectTypes());
```

(Add `onMounted` to the imports from `'vue'`: `import { computed, watch, onBeforeUnmount, onMounted } from 'vue';`)

In the template `.scroll` div, insert a new "Typ" section **before** the existing "Location" section:

```vue
        <section class="group">
          <h3>Typ</h3>
          <div class="pill-row">
            <button
              type="button"
              :class="['pill', { active: filters.types.length === 0 }]"
              @click="update({ types: [] })"
            >
              Alla typer
            </button>
            <button
              v-for="ot in objectTypes"
              :key="ot.typeId"
              type="button"
              :class="['pill', { active: filters.types.length > 0 && filters.types.includes(ot.typeId) }]"
              @click="update({ types: filters.types.includes(ot.typeId) ? filters.types.filter(t => t !== ot.typeId) : [...filters.types, ot.typeId] })"
            >
              {{ ot.displayName ?? ot.typeId }}
            </button>
          </div>
        </section>
```

- [ ] **Step 4: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/ListingsView.vue frontend/src/components/FilterSheet.vue
git commit -m "feat: add TypeSelectorRow to ListingsView; add Typ filter section to FilterSheet"
```

---

## Task 9: Update HouseAirbnbCard — parking placeholder, type badge, smart meta line, competitionDisplay

**Files:**
- Modify: `frontend/src/components/HouseAirbnbCard.vue`

Changes: (1) import parking placeholder and use it as fallback when no images; (2) replace static `queueLabel` with `competitionDisplay()` so parking spots show applicant count instead of blank; (3) show type badge for non-residential listings; (4) build meta line from only non-null values so parking ("5 000 kr · — · —") doesn't show dashes.

- [ ] **Step 1: Replace the entire HouseAirbnbCard.vue script and template**

```vue
<!-- frontend/src/components/HouseAirbnbCard.vue -->
<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import type { House } from '@/types/house';
import Carousel from './Carousel.vue';
import SaveButton from './SaveButton.vue';
import { competitionTier, competitionDisplay, galleryImages, locationLabel, shortHeadline } from '@/lib/derived';
import { formatArea, formatRent, formatRooms } from '@/lib/format';
import parkingPlaceholder from '@/assets/parking-placeholder.png';

const props = defineProps<{
  house: House;
  highlighted?: boolean;
}>();

const images = computed(() => {
  const imgs = galleryImages(props.house);
  if (imgs.length > 0) return imgs;
  if ((props.house.type ?? '').toLowerCase() === 'parking') return [parkingPlaceholder];
  return imgs;
});
const headline = computed(() => shortHeadline(props.house));
const location = computed(() => locationLabel(props.house));
const tier = computed(() => competitionTier(props.house.queuePoints));
const detailRoute = computed(() => ({
  name: 'detail' as const,
  params: { internalId: String(props.house.internalId) },
}));
const competitionChip = computed(() => competitionDisplay(props.house));
const typeLabel = computed(() => {
  const t = props.house.type ?? '';
  if (t.toLowerCase() === 'residential' || t === '') return null;
  return props.house.rentalObjectType ?? t;
});
const metaLine = computed(() => {
  const parts: string[] = [];
  if (props.house.rent != null) parts.push(formatRent(props.house.rent));
  if (props.house.rooms != null) parts.push(formatRooms(props.house.rooms));
  if (props.house.area != null) parts.push(formatArea(props.house.area));
  return parts.join(' · ') || '—';
});
</script>

<template>
  <RouterLink :to="detailRoute" class="airbnb-card" :class="{ highlighted }">
    <div class="media">
      <Carousel :images="images" :alt="headline" cover />
      <span v-if="competitionChip" :class="['queue-chip', tier]">{{ competitionChip }}</span>
      <span v-if="typeLabel" class="type-badge">{{ typeLabel }}</span>
      <div class="save-wrap">
        <SaveButton :internal-id="house.internalId" :label="headline" />
      </div>
    </div>
    <div class="body">
      <h3 class="title">{{ headline }}</h3>
      <p class="loc">{{ location }}</p>
      <p class="meta">{{ metaLine }}</p>
    </div>
  </RouterLink>
</template>
```

Keep the existing `<style scoped>` block unchanged, but add these two rules inside it:

```css
.type-badge {
  position: absolute;
  bottom: 10px;
  left: 12px;
  padding: 4px 10px;
  border-radius: var(--r-pill);
  background: rgba(0, 0, 0, 0.62);
  color: var(--white);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  pointer-events: none;
}
```

- [ ] **Step 2: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/HouseAirbnbCard.vue
git commit -m "feat: update HouseAirbnbCard with parking placeholder, type badge, competitionDisplay"
```

---

## Task 10: Update HouseCard and HouseGridCard

**Files:**
- Modify: `frontend/src/components/HouseCard.vue`
- Modify: `frontend/src/components/HouseGridCard.vue`

### HouseCard

Changes: use `competitionDisplay()` instead of the inline `queueLabel` computation; add parking placeholder fallback; add type badge in the card image area.

- [ ] **Step 1: Update HouseCard.vue script section**

In `frontend/src/components/HouseCard.vue`, update the import line for `derived.ts`:

```typescript
import { competitionDisplay, competitionLabel, competitionTier, galleryImages, hasFloorplan, locationLabel, shortHeadline } from '@/lib/derived';
```

Add the parking placeholder import after the Carousel import:

```typescript
import parkingPlaceholder from '@/assets/parking-placeholder.png';
```

Replace the `images` computed and `queueLabel` computed with:

```typescript
const images = computed(() => {
  const imgs = galleryImages(props.house);
  if (imgs.length > 0) return imgs;
  if ((props.house.type ?? '').toLowerCase() === 'parking') return [parkingPlaceholder];
  return imgs;
});
const queueLabel = computed(() => competitionDisplay(props.house) ?? '—');
const typeLabel = computed(() => {
  const t = props.house.type ?? '';
  if (t.toLowerCase() === 'residential' || t === '') return null;
  return props.house.rentalObjectType ?? t;
});
```

- [ ] **Step 2: Add type badge to HouseCard template**

In the `<template>` of `HouseCard.vue`, the `<Carousel>` is at the top. Wrap it and the badge in a relative container — or simply add the badge as an absolutely positioned sibling inside `.house-card`. Find the `<Carousel>` line and replace:

```vue
    <Carousel :images="images" :labels="imageLabels" :alt="headline" />
```

with:

```vue
    <div class="card-media">
      <Carousel :images="images" :labels="imageLabels" :alt="headline" />
      <span v-if="typeLabel" class="card-type-badge">{{ typeLabel }}</span>
    </div>
```

Add to HouseCard's `<style scoped>`:

```css
.card-media {
  position: relative;
}

.card-type-badge {
  position: absolute;
  bottom: 10px;
  left: 12px;
  padding: 4px 10px;
  border-radius: var(--r-pill);
  background: rgba(0, 0, 0, 0.62);
  color: var(--white);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  pointer-events: none;
  z-index: 1;
}
```

### HouseGridCard

Same pattern: parking placeholder fallback and type badge.

- [ ] **Step 3: Update HouseGridCard.vue**

In `frontend/src/components/HouseGridCard.vue`, add to the imports:

```typescript
import parkingPlaceholder from '@/assets/parking-placeholder.png';
```

Replace the `images` computed:

```typescript
const images = computed(() => {
  const imgs = galleryImages(props.house);
  if (imgs.length > 0) return imgs;
  if ((props.house.type ?? '').toLowerCase() === 'parking') return [parkingPlaceholder];
  return imgs;
});
const typeLabel = computed(() => {
  const t = props.house.type ?? '';
  if (t.toLowerCase() === 'residential' || t === '') return null;
  return props.house.rentalObjectType ?? t;
});
```

Also add `galleryImages` to the import line from `@/lib/derived`:

```typescript
import { galleryImages, locationLabel, shortHeadline } from '@/lib/derived';
```

In the template, find `<div class="media">` and add the type badge inside it, after the `<Carousel>`:

```vue
    <div class="media">
      <Carousel :images="images" :alt="headline" :cover="false" />
      <span v-if="typeLabel" class="type-badge">{{ typeLabel }}</span>
      <div class="save-overlay">
        <SaveButton :internal-id="house.internalId" :label="headline" />
      </div>
    </div>
```

Add to HouseGridCard's `<style scoped>`:

```css
.type-badge {
  position: absolute;
  bottom: 8px;
  left: 10px;
  padding: 3px 9px;
  border-radius: var(--r-pill);
  background: rgba(0, 0, 0, 0.62);
  color: var(--white);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  pointer-events: none;
}
```

Note: `.media` in HouseGridCard must have `position: relative` for the badge to position correctly. Check the existing style — if it doesn't have it, add `position: relative;` to the `.media` rule.

- [ ] **Step 4: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/HouseCard.vue frontend/src/components/HouseGridCard.vue
git commit -m "feat: add parking placeholder, type badge, and competitionDisplay to HouseCard and HouseGridCard"
```

---

## Task 11: HouseMap — type-aware marker icons

**Files:**
- Modify: `frontend/src/components/HouseMap.vue`

Currently all map markers use the same home SVG icon (`HOME_SVG`). Parking spots should show a "P" parking icon. The `pinIcon` function needs to call `typeSvgIcon(house.type)` instead of always using `HOME_SVG`. Also add a CSS class `neutral` for non-residential types where queue points are absent (neutral gray background instead of the tier-based color).

- [ ] **Step 1: Update HouseMap.vue script**

In `frontend/src/components/HouseMap.vue`, update the `derived` import to include `typeSvgIcon`:

```typescript
import { competitionTier, typeSvgIcon } from '@/lib/derived';
```

Remove the `HOME_SVG` constant (the one defined as a module-level const). Then update `pinIcon` to:

```typescript
function pinIcon(house: House, highlighted: boolean): DivIcon {
  const tier = house.queuePoints != null ? competitionTier(house.queuePoints) : 'neutral';
  const classes = ['pin', tier, highlighted ? 'highlight' : ''].filter(Boolean).join(' ');
  return L.divIcon({
    className: classes,
    html: typeSvgIcon(house.type),
    iconSize: [40, 40],
    iconAnchor: [20, 20],
  });
}
```

- [ ] **Step 2: Add neutral CSS class to HouseMap.vue styles**

In the `<style>` block (not scoped) of `HouseMap.vue`, add after `.leaflet-marker-icon.pin.unknown`:

```css
.leaflet-marker-icon.pin.neutral {
  background: var(--ink-soft);
  color: var(--white);
  border-color: var(--surface-2);
}
```

- [ ] **Step 3: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/HouseMap.vue
git commit -m "feat: type-aware map marker icons (parking P icon, neutral tier for non-residential)"
```

---

## Task 12: DetailView — amenities panel and applicant count

**Files:**
- Modify: `frontend/src/views/DetailView.vue`

Add a "Ingår i hyran" amenity chips panel (from `house.includedJson`) and an applicant count display (from `house.nrApplications`) in the Key facts section.

- [ ] **Step 1: Add parseAmenities import to DetailView**

In `frontend/src/views/DetailView.vue`, update the `derived` import to include `parseAmenities`:

```typescript
import {
  competitionLabel,
  competitionTier,
  galleryImages,
  hasFloorplan,
  locationLabel,
  parseAmenities,
  shortHeadline,
} from '@/lib/derived';
```

- [ ] **Step 2: Add computed properties**

In the `<script setup>` block, add after the `nearby` computed:

```typescript
const amenities = computed(() => parseAmenities(house.value?.includedJson));
const nrApplications = computed(() => house.value?.nrApplications ?? null);
```

- [ ] **Step 3: Add amenities and applicant count to the template**

In the template, after the `<section class="panel">` that contains "Key facts" (the `.facts` div), and before or after the description panel, add the amenities section:

```vue
    <section v-if="amenities.length > 0" class="panel">
      <h2 class="panel-title">Ingår i hyran</h2>
      <div class="amenities">
        <span v-for="a in amenities" :key="a" class="amenity-chip">{{ a }}</span>
      </div>
    </section>
```

In the "Key facts" section `.facts` div, add the applicant count fact after the `photos.length` fact:

```vue
        <div v-if="nrApplications != null" class="fact">
          <b>{{ nrApplications.toLocaleString('sv-SE') }}</b>
          <span>sökande</span>
        </div>
```

- [ ] **Step 4: Add styles for amenities**

In the `<style scoped>` block of `DetailView.vue`, add:

```css
.amenities {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.amenity-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: var(--r-pill);
  background: var(--surface-2);
  border: 1px solid var(--line);
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}
```

- [ ] **Step 5: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/DetailView.vue
git commit -m "feat: add amenities chips and applicant count to DetailView"
```

---

## Task 13: ObjectTypeCard component and HomeView browse-by-type section

**Files:**
- Create: `frontend/src/components/ObjectTypeCard.vue`
- Modify: `frontend/src/views/HomeView.vue`

### ObjectTypeCard

A compact card showing type icon, Swedish name, listing count, and price/size ranges. Clicking "Visa listings" navigates to `/listings?types=<typeId>`. An "Mer info" link goes to `/types/<typeId>`.

- [ ] **Step 1: Create ObjectTypeCard.vue**

```vue
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
```

### HomeView browse-by-type section

- [ ] **Step 2: Add useObjectTypes and ObjectTypeCard to HomeView**

In `frontend/src/views/HomeView.vue`, add to the imports:

```typescript
import ObjectTypeCard from '@/components/ObjectTypeCard.vue';
import { useObjectTypes } from '@/composables/useObjectTypes';
```

Add in the script setup after `const { filters } = useFilters();`:

```typescript
const { objectTypes, load: loadObjectTypes } = useObjectTypes();
```

Update `onMounted` to also load object types:

```typescript
onMounted(() => {
  load();
  loadObjectTypes();
});
```

Add a computed for types with listings:

```typescript
const browseTypes = computed(() =>
  objectTypes.value.filter((ot) => (ot.numberOfMarketObjects ?? 0) > 0),
);
```

- [ ] **Step 3: Add the browse-by-type section to the HomeView template**

In the template, after the `highestQueue` section and before the loading/error state divs, add:

```vue
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
```

Add the `.type-grid` style to HomeView's `<style scoped>`:

```css
.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}
```

- [ ] **Step 4: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/ObjectTypeCard.vue frontend/src/views/HomeView.vue
git commit -m "feat: add ObjectTypeCard and browse-by-type section to HomeView"
```

---

## Task 14: TypeMetaHeader, TypeDetailView, and router

**Files:**
- Create: `frontend/src/components/TypeMetaHeader.vue`
- Create: `frontend/src/views/TypeDetailView.vue`
- Modify: `frontend/src/router/index.ts`

### TypeMetaHeader

Renders the type's display name, description, and metadata (price/size ranges, count). Used only by `TypeDetailView`.

- [ ] **Step 1: Create TypeMetaHeader.vue**

```vue
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
```

### TypeDetailView

Shows the `TypeMetaHeader` for a given `typeId` (loaded from `/api/object-types/:typeId`), then a list of all active houses of that type sorted by deadline. Uses the existing `useHouses` composable to access house data. Filters by `house.type === typeId` client-side (no extra API call).

- [ ] **Step 2: Create TypeDetailView.vue**

```vue
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
```

### Router

- [ ] **Step 3: Add the /types/:typeId route**

In `frontend/src/router/index.ts`, add before the catch-all route:

```typescript
  { path: '/types/:typeId', name: 'type-detail', component: () => import('@/views/TypeDetailView.vue'), props: true },
```

The full routes array becomes:

```typescript
const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
  { path: '/listings', name: 'listings', component: () => import('@/views/ListingsView.vue') },
  { path: '/listings/:internalId', name: 'detail', component: () => import('@/views/DetailView.vue'), props: true },
  { path: '/saved', name: 'saved', component: () => import('@/views/SavedView.vue') },
  { path: '/types/:typeId', name: 'type-detail', component: () => import('@/views/TypeDetailView.vue'), props: true },
  { path: '/:catchAll(.*)', redirect: '/' },
];
```

- [ ] **Step 4: Type-check**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run type-check 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 5: Full frontend build**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run build 2>&1 | tail -20
```

Expected: `✓ built in …` — zero TypeScript errors, zero Vite build errors.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/TypeMetaHeader.vue \
        frontend/src/views/TypeDetailView.vue \
        frontend/src/router/index.ts
git commit -m "feat: add TypeMetaHeader, TypeDetailView, and /types/:typeId route"
```

---

## Task 15: Final verification

- [ ] **Step 1: Full backend test suite**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-25.jdk/Contents/Home
mymvn test --no-transfer-progress 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`, zero failures.

- [ ] **Step 2: Full frontend build**

```bash
cd /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend && npm run build 2>&1 | tail -20
```

Expected: clean build, zero TypeScript errors.

- [ ] **Step 3: Confirm new API endpoint is reachable (with backend running)**

```bash
curl -s http://localhost:8080/api/object-types | head -c 200
```

Expected: JSON array of ObjectType objects (may be empty if no sync has run yet in dev).

- [ ] **Step 4: Confirm no stale references**

```bash
grep -rn "HOME_SVG" /Users/PRASI/Documents/My_Documents/boplats-tracker/frontend/src/
```

Expected: no output (the constant was removed from HouseMap.vue in Task 11).

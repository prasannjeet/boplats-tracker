import { computed } from 'vue';
import { useRoute, useRouter, type LocationQueryRaw } from 'vue-router';
import type { House } from '@/types/house';
import { competitionTier, costPerM2, hasFloorplan, imageCount } from '@/lib/derived';
import { daysUntil } from '@/lib/format';

export type ViewMode = 'list' | 'grid' | 'split';
export type QuickPreset = 'all' | 'spacious' | 'lowqueue' | 'deadline' | 'floorplan';
export type SortKey =
  | 'newest'
  | 'deadline'
  | 'rentLow'
  | 'rentHigh'
  | 'areaLarge'
  | 'areaSmall'
  | 'cppLow'
  | 'queueLow'
  | 'queueHigh'
  | 'roomsMany';
export type DeadlinePreset = 'all' | 'today' | 'next3' | 'next7' | 'month';

export interface Filters {
  q: string;
  cities: string[];
  rooms: number[];
  areaMin: number | null;
  areaMax: number | null;
  rentMin: number | null;
  rentMax: number | null;
  queueMin: number | null;
  queueMax: number | null;
  includeUnknownQueue: boolean;
  deadline: DeadlinePreset;
  hidePassed: boolean;
  hasFloorplan: boolean;
  minImages: number | null;
  hasDescription: boolean;
  view: ViewMode;
  sort: SortKey;
  preset: QuickPreset;
}

export const defaultFilters: Filters = {
  q: '',
  cities: [],
  rooms: [],
  areaMin: null,
  areaMax: null,
  rentMin: null,
  rentMax: null,
  queueMin: null,
  queueMax: null,
  includeUnknownQueue: true,
  deadline: 'all',
  hidePassed: true,
  hasFloorplan: false,
  minImages: null,
  hasDescription: false,
  view: 'split',
  sort: 'deadline',
  preset: 'all',
};

function getString(v: unknown, fallback: string): string {
  if (typeof v === 'string') return v;
  if (Array.isArray(v) && typeof v[0] === 'string') return v[0];
  return fallback;
}

function getNum(v: unknown): number | null {
  const s = getString(v, '');
  if (!s) return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

function getBool(v: unknown, fallback: boolean): boolean {
  const s = getString(v, '');
  if (s === '') return fallback;
  return s === '1' || s === 'true';
}

function getList(v: unknown): string[] {
  const s = getString(v, '');
  if (!s) return [];
  return s.split(',').map((p) => p.trim()).filter(Boolean);
}

function parseFilters(query: Record<string, unknown>): Filters {
  return {
    q: getString(query.q, ''),
    cities: getList(query.cities),
    rooms: getList(query.rooms).map(Number).filter(Number.isFinite),
    areaMin: getNum(query.areaMin),
    areaMax: getNum(query.areaMax),
    rentMin: getNum(query.rentMin),
    rentMax: getNum(query.rentMax),
    queueMin: getNum(query.queueMin),
    queueMax: getNum(query.queueMax),
    includeUnknownQueue: getBool(query.includeUnknownQueue, defaultFilters.includeUnknownQueue),
    deadline: (getString(query.deadline, defaultFilters.deadline) as DeadlinePreset) ?? defaultFilters.deadline,
    hidePassed: getBool(query.hidePassed, defaultFilters.hidePassed),
    hasFloorplan: getBool(query.hasFloorplan, defaultFilters.hasFloorplan),
    minImages: getNum(query.minImages),
    hasDescription: getBool(query.hasDescription, defaultFilters.hasDescription),
    view: (getString(query.view, defaultFilters.view) as ViewMode) ?? defaultFilters.view,
    sort: (getString(query.sort, defaultFilters.sort) as SortKey) ?? defaultFilters.sort,
    preset: (getString(query.preset, defaultFilters.preset) as QuickPreset) ?? defaultFilters.preset,
  };
}

function serializeFilters(f: Filters): LocationQueryRaw {
  const out: Record<string, string> = {};
  if (f.q) out.q = f.q;
  if (f.cities.length) out.cities = f.cities.join(',');
  if (f.rooms.length) out.rooms = f.rooms.join(',');
  if (f.areaMin != null) out.areaMin = String(f.areaMin);
  if (f.areaMax != null) out.areaMax = String(f.areaMax);
  if (f.rentMin != null) out.rentMin = String(f.rentMin);
  if (f.rentMax != null) out.rentMax = String(f.rentMax);
  if (f.queueMin != null) out.queueMin = String(f.queueMin);
  if (f.queueMax != null) out.queueMax = String(f.queueMax);
  if (f.includeUnknownQueue !== defaultFilters.includeUnknownQueue) out.includeUnknownQueue = f.includeUnknownQueue ? '1' : '0';
  if (f.deadline !== defaultFilters.deadline) out.deadline = f.deadline;
  if (f.hidePassed !== defaultFilters.hidePassed) out.hidePassed = f.hidePassed ? '1' : '0';
  if (f.hasFloorplan) out.hasFloorplan = '1';
  if (f.minImages != null) out.minImages = String(f.minImages);
  if (f.hasDescription) out.hasDescription = '1';
  if (f.view !== defaultFilters.view) out.view = f.view;
  if (f.sort !== defaultFilters.sort) out.sort = f.sort;
  if (f.preset !== defaultFilters.preset) out.preset = f.preset;
  return out;
}

function deadlineWindowDays(preset: DeadlinePreset): number | null {
  switch (preset) {
    case 'today':
      return 0;
    case 'next3':
      return 3;
    case 'next7':
      return 7;
    case 'month':
      return 30;
    default:
      return null;
  }
}

function applyQuickPreset(f: Filters, h: House, now: number): boolean {
  switch (f.preset) {
    case 'spacious':
      return (h.area ?? 0) >= 65;
    case 'lowqueue':
      return competitionTier(h.queuePoints) === 'low' || (h.queuePoints == null && f.includeUnknownQueue);
    case 'deadline':
      if (!h.applicationDeadline) return false;
      const days = daysUntil(h.applicationDeadline, new Date(now));
      if (days == null) return false;
      return days >= 0 && days <= 7;
    case 'floorplan':
      return hasFloorplan(h);
    default:
      return true;
  }
}

function matchesText(h: House, q: string): boolean {
  if (!q) return true;
  const needle = q.toLocaleLowerCase('sv-SE');
  const haystacks: Array<string | null | undefined> = [
    h.displayName,
    h.address,
    h.completeAddress,
    h.street,
    h.city,
    h.areaName,
    h.localId,
    h.number,
    h.description,
  ];
  for (const v of haystacks) {
    if (v && v.toLocaleLowerCase('sv-SE').includes(needle)) return true;
  }
  return false;
}

function cityKey(value: string | null | undefined): string {
  if (!value) return '';
  return value.charAt(0).toLocaleUpperCase('sv-SE') + value.slice(1).toLocaleLowerCase('sv-SE');
}

export function filterHouses(houses: House[], f: Filters, now = Date.now()): House[] {
  const deadlineDays = deadlineWindowDays(f.deadline);
  return houses.filter((h) => {
    if (!matchesText(h, f.q)) return false;
    if (f.cities.length && !f.cities.includes(cityKey(h.city))) return false;
    if (f.rooms.length && (h.rooms == null || !f.rooms.includes(h.rooms))) return false;
    if (f.areaMin != null && (h.area == null || h.area < f.areaMin)) return false;
    if (f.areaMax != null && (h.area == null || h.area > f.areaMax)) return false;
    if (f.rentMin != null && (h.rent == null || h.rent < f.rentMin)) return false;
    if (f.rentMax != null && (h.rent == null || h.rent > f.rentMax)) return false;
    if (h.queuePoints == null) {
      if (!f.includeUnknownQueue) return false;
    } else {
      if (f.queueMin != null && h.queuePoints < f.queueMin) return false;
      if (f.queueMax != null && h.queuePoints > f.queueMax) return false;
    }
    if (f.hidePassed && h.applicationDeadline) {
      if ((daysUntil(h.applicationDeadline, new Date(now)) ?? 0) < 0) return false;
    }
    if (deadlineDays != null && h.applicationDeadline) {
      const days = daysUntil(h.applicationDeadline, new Date(now));
      if (days == null) return false;
      if (days < 0 || days > deadlineDays) return false;
    } else if (deadlineDays != null && !h.applicationDeadline) {
      return false;
    }
    if (f.hasFloorplan && !hasFloorplan(h)) return false;
    if (f.minImages != null && imageCount(h) < f.minImages) return false;
    if (f.hasDescription && !(h.description && h.description.trim().length > 0)) return false;
    if (!applyQuickPreset(f, h, now)) return false;
    return true;
  });
}

export function sortHouses(houses: House[], sort: SortKey, now = Date.now()): House[] {
  const arr = houses.slice();
  switch (sort) {
    case 'deadline':
      arr.sort((a, b) => deadlineMs(a, now) - deadlineMs(b, now));
      break;
    case 'newest':
      arr.sort((a, b) => fetchedMs(b) - fetchedMs(a));
      break;
    case 'rentLow':
      arr.sort((a, b) => (a.rent ?? Infinity) - (b.rent ?? Infinity));
      break;
    case 'rentHigh':
      arr.sort((a, b) => (b.rent ?? -Infinity) - (a.rent ?? -Infinity));
      break;
    case 'areaLarge':
      arr.sort((a, b) => (b.area ?? -Infinity) - (a.area ?? -Infinity));
      break;
    case 'areaSmall':
      arr.sort((a, b) => (a.area ?? Infinity) - (b.area ?? Infinity));
      break;
    case 'cppLow':
      arr.sort((a, b) => (costPerM2(a) ?? Infinity) - (costPerM2(b) ?? Infinity));
      break;
    case 'queueLow':
      arr.sort((a, b) => (a.queuePoints ?? Infinity) - (b.queuePoints ?? Infinity));
      break;
    case 'queueHigh':
      arr.sort((a, b) => (b.queuePoints ?? -Infinity) - (a.queuePoints ?? -Infinity));
      break;
    case 'roomsMany':
      arr.sort((a, b) => (b.rooms ?? -Infinity) - (a.rooms ?? -Infinity));
      break;
  }
  return arr;
}

function deadlineMs(h: House, fallback: number): number {
  return h.applicationDeadline ? new Date(h.applicationDeadline).getTime() : fallback + 365 * 86400000;
}

function fetchedMs(h: House): number {
  return h.lastDetailFetchedAt ? new Date(h.lastDetailFetchedAt).getTime() : 0;
}

export function useFilters() {
  const route = useRoute();
  const router = useRouter();

  const filters = computed<Filters>(() => parseFilters(route.query as Record<string, unknown>));

  function update(patch: Partial<Filters>) {
    const next: Filters = { ...filters.value, ...patch };
    router.replace({ path: route.path, query: serializeFilters(next), hash: route.hash });
  }

  function reset() {
    router.replace({ path: route.path, query: {}, hash: route.hash });
  }

  function toggleArray<K extends 'cities' | 'rooms'>(key: K, value: Filters[K] extends Array<infer V> ? V : never) {
    const current = filters.value[key] as Array<typeof value>;
    const next = current.includes(value) ? current.filter((v) => v !== value) : [...current, value];
    update({ [key]: next } as Partial<Filters>);
  }

  const activeCount = computed(() => {
    const f = filters.value;
    let n = 0;
    if (f.q) n++;
    if (f.cities.length) n++;
    if (f.rooms.length) n++;
    if (f.areaMin != null || f.areaMax != null) n++;
    if (f.rentMin != null || f.rentMax != null) n++;
    if (f.queueMin != null || f.queueMax != null) n++;
    if (!f.includeUnknownQueue) n++;
    if (f.deadline !== defaultFilters.deadline) n++;
    if (!f.hidePassed) n++;
    if (f.hasFloorplan) n++;
    if (f.minImages != null) n++;
    if (f.hasDescription) n++;
    if (f.preset !== 'all') n++;
    return n;
  });

  return { filters, update, reset, toggleArray, activeCount };
}

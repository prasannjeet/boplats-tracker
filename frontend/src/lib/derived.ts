import type { House } from '@/types/house';

export type CompetitionTier = 'low' | 'medium' | 'high' | 'very-high' | 'unknown';

export function competitionTier(queuePoints: number | null | undefined): CompetitionTier {
  if (queuePoints == null) return 'unknown';
  if (queuePoints < 1500) return 'low';
  if (queuePoints < 2800) return 'medium';
  if (queuePoints < 4200) return 'high';
  return 'very-high';
}

export function competitionLabel(tier: CompetitionTier): string {
  switch (tier) {
    case 'low':
      return 'low queue';
    case 'medium':
      return 'medium competition';
    case 'high':
      return 'high competition';
    case 'very-high':
      return 'very high competition';
    default:
      return 'unknown queue';
  }
}

export function costPerM2(house: Pick<House, 'rent' | 'area'>): number | null {
  if (house.rent == null || house.area == null || house.area <= 0) return null;
  return house.rent / house.area;
}

export function locationLabel(house: House): string {
  const parts: string[] = [];
  if (house.city) parts.push(titleish(house.city));
  if (house.areaName && !parts.includes(titleish(house.areaName))) parts.push(titleish(house.areaName));
  if (house.floorDisplayName) parts.push(house.floorDisplayName);
  return parts.join(' · ');
}

function titleish(value: string): string {
  if (!value) return value;
  // sv-SE locale; keep Å/Ä/Ö
  return value.charAt(0).toLocaleUpperCase('sv-SE') + value.slice(1).toLocaleLowerCase('sv-SE');
}

export function primaryImage(house: House): string | null {
  if (house.images && house.images.length > 0) return house.images[0].address;
  return house.imageUrl ?? null;
}

export function galleryImages(house: House): string[] {
  const out: string[] = [];
  const seen = new Set<string>();
  for (const img of house.images ?? []) {
    if (img.address && !seen.has(img.address)) {
      seen.add(img.address);
      out.push(img.address);
    }
  }
  if (out.length === 0 && house.imageUrl) out.push(house.imageUrl);
  return out;
}

export function hasFloorplan(house: House): boolean {
  return (house.floorplans?.length ?? 0) > 0;
}

export function imageCount(house: House): number {
  return (house.images?.length ?? 0) || (house.imageUrl ? 1 : 0);
}

export function shortHeadline(house: House): string {
  return house.displayName ?? house.address ?? house.completeAddress ?? `Listing ${house.id}`;
}

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

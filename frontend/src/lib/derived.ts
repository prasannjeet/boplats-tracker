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

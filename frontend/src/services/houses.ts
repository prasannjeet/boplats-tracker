import type { House } from '@/types/house';

export async function fetchHouses(includeEnded = false): Promise<House[]> {
  const url = includeEnded ? '/api/houses?includeEnded=true' : '/api/houses';
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

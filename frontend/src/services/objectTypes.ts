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

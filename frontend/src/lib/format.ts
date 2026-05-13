export function formatRent(value: number | null | undefined): string {
  if (value == null) return '—';
  return `${Math.round(value).toLocaleString('sv-SE')} kr`;
}

export function formatArea(value: number | null | undefined): string {
  if (value == null) return '—';
  const rounded = Math.round(value * 10) / 10;
  return `${rounded.toLocaleString('sv-SE')} m²`;
}

export function formatCostPerM2(rent: number | null | undefined, area: number | null | undefined): string {
  if (rent == null || area == null || area <= 0) return '—';
  return `${Math.round(rent / area)} kr/m²`;
}

export function formatRooms(rooms: number | null | undefined): string {
  if (rooms == null) return '—';
  return rooms === 1 ? '1 room' : `${rooms} rooms`;
}

export function formatNumber(value: number | null | undefined): string {
  if (value == null) return '—';
  return Math.round(value).toLocaleString('sv-SE');
}

export function formatShortDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
}

export function formatLongDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}

export function formatDeadlineTimestamp(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return `${d.toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' })} · ${d
    .toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}`;
}

export function daysUntil(iso: string | null | undefined, ref: Date = new Date()): number | null {
  if (!iso) return null;
  const target = startOfLocalDay(new Date(iso)).getTime();
  const diff = target - startOfLocalDay(ref).getTime();
  return Math.floor(diff / 86400000);
}

function startOfLocalDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function formatCountdown(iso: string | null | undefined, ref: Date = new Date()): string {
  const d = daysUntil(iso, ref);
  if (d == null) return '—';
  if (d < 0) return 'Closed';
  if (d === 0) return 'Today';
  if (d === 1) return '1 day left';
  return `${d} days left`;
}

export function formatRelative(iso: string | null | undefined, ref: Date = new Date()): string {
  if (!iso) return '—';
  const diff = Math.max(0, ref.getTime() - new Date(iso).getTime());
  const m = Math.floor(diff / 60000);
  if (m < 1) return 'just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.floor(h / 24);
  return `${d}d ago`;
}

export function titleCase(value: string | null | undefined): string {
  if (!value) return '';
  return value
    .toLocaleLowerCase('sv-SE')
    .replace(/(^|\s|-)([\p{L}])/gu, (_m, sep, c) => sep + c.toLocaleUpperCase('sv-SE'));
}

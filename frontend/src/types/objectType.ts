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

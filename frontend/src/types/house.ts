// Mirrors com.prasannjeet.vaxjobostader.jpa.House.
// Date fields are returned as ISO strings by Jackson.

export interface AreaPathEntry {
  id: string;
  displayName: string;
}

export interface HouseImage {
  displayName: string | null;
  mimeType: string | null;
  address: string;
  linkedToType?: string | null;
}

export interface HouseFloorplan {
  displayName: string | null;
  mimeType: string | null;
  address: string;
}

export interface House {
  internalId: number;
  id: string;
  localId: string | null;

  type: string | null;
  displayName: string | null;
  number: string | null;

  address: string | null;
  description: string | null;

  rent: number | null;
  area: number | null;
  rooms: number | null;

  availableFrom: string | null;
  applicationDeadline: string | null;
  endDate: string | null;

  imageUrl: string | null;
  queuePoints: number | null;
  lastDetailFetchedAt: string | null;

  floorDisplayName: string | null;
  areaName: string | null;

  street: string | null;
  streetNumber: string | null;
  postcode: string | null;
  city: string | null;
  country: string | null;
  completeAddress: string | null;

  latitude: number | null;
  longitude: number | null;

  areaPathJson: AreaPathEntry[] | null;
  images: HouseImage[];
  floorplans: HouseFloorplan[];
}

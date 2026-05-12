# Frontend Structure Spec — boplats-tracker

> **Scope:** Structural skeleton only. No visual design, layout positioning, colors, typography, or component styling decisions are made here. This describes *what information appears where*, *what the user can do*, and *what data each view depends on*. The design agent decides everything else.

The intended feel is Airbnb-like: a browsable catalogue with rich filtering, map-based exploration, and a deep detail page per listing.

---

## 1. Data inventory (what we have to work with)

### Per-listing fields

| Field | Type | Notes / Observed range |
|---|---|---|
| `internalId` | bigint | Surrogate PK; identifies a *re-listing instance*. |
| `id` | string | External Momentum id; stable across re-listings. |
| `localId` | string | Human-readable property id (e.g. `4439940`). |
| `displayName` | string | Short label, e.g. `Herrgårdsgatan 13 A`. |
| `number` | string | Internal listing number, e.g. `AN-13335`. |
| `completeAddress` / `address` | string | Full address line. |
| `street`, `streetNumber`, `postcode`, `city`, `country` | strings | City values observed: Braås, Gemla, Ingelstad, Lammhult, ROTTNE, Tävelsås, Vederslöv, VÄXJÖ, Åryd. |
| `areaName` | string | Cadastral / property-area label. |
| `areaPath` | JSON array of `{id, displayName}` | Hierarchical area, e.g. `[{Lammhult}]`. Useful for grouping/breadcrumbs. |
| `floorDisplayName` | string | E.g. `Våning 2`, `Våning BV`, `Våning KV`, `Våning sut`. |
| `type` | string | Currently always `residential`. |
| `rooms` | int | Observed 1–4. |
| `area` | double (m²) | Observed 26–96.5. |
| `rent` | double (kr/month) | Observed 3 079–12 449. |
| `availableFrom` | date | Move-in date. |
| `applicationDeadline` | timestamp | When applications close. |
| `queuePoints` | double, nullable | Current cut-off (sticky once set). Observed 1 504–5 266. |
| `description` | text | Free-text marketing description (Swedish). |
| `imageUrl` | string | Single cover image URL. |
| `endDate` | date, nullable | Set when listing is no longer active. |
| `lastDetailFetchedAt` | timestamp | Freshness signal. |
| `latitude`, `longitude` | double | Geocoded coordinates of `completeAddress`. Every listing has them. |
| `images[]` | list of images | Each: `displayName`, `mimeType`, `address` (URL), `linkedToType`. |
| `floorplans[]` | list of floor plans | Each: `displayName`, `mimeType`, `address` (URL). |

### Aggregate facts (today)
- 160 active listings, 9 cities, all `residential`.
- ~3.3 images per listing on average; most listings have a floor plan.
- 100 % of active listings have geocoded coordinates.

### Derivable signals
- **Cost per m²** = `rent / area`.
- **Days until deadline** = `applicationDeadline − now`.
- **Time until move-in** = `availableFrom − now`.
- **Competition tier** from `queuePoints` (bucketed: low / medium / high / very high) — or shown as `—` when null.
- **Freshness** = age of `lastDetailFetchedAt`.

---

## 2. Top-level pages

The app has three primary pages plus auxiliary states.

1. **Home / Browse** — the main dashboard. Listings + filters + map. Default entry point.
2. **Listing Detail** — everything we know about a single `(id, availableFrom)` instance.
3. **Saved / Watchlist** — a bookmark list (we have no user accounts, so personalisation is per-device).

Auxiliary states: empty results, loading, error, "listing has ended" (read-only historical view).

---

## 3. Home / Browse page

### 3.1 Page-level elements

- **Header bar**
  - App name / logo
  - Global search box (free-text; matches against `displayName`, `street`, `city`, `areaName`, `localId`, `number`, `description`)
  - Saved-listings entry point (count badge)
  - Data-freshness indicator (e.g. "Updated 4 min ago", derived from newest `lastDetailFetchedAt`)
- **View switcher** — toggles between three modes of the same filtered result set:
  - **List view** (default)
  - **Map view**
  - **Split view** (list + map side-by-side) — desktop only
- **Sort control** — see §3.4.
- **Result summary** — "*N* listings match" + active-filter chips (each removable).
- **Filter panel** — see §3.3. Either a sidebar or a toggleable sheet; the design agent chooses placement.

### 3.2 Listing card (used in list view and as map-pin popovers)

Each card shows:
- Cover image (`imageUrl` or first of `images[]`)
- `displayName`
- `city` · `areaPath` (last segment) · `floorDisplayName`
- Rooms · Area (m²) · Rent (kr/mo) — the three "headline" numbers
- Derived: cost-per-m²
- `availableFrom`
- Deadline countdown (`applicationDeadline` − now), with a "deadline soon / passed" emphasis state
- Queue-points indicator (numeric value, or "—" if null, plus a competition-tier label)
- Save / bookmark toggle
- Click → navigates to Listing Detail

### 3.3 Filter panel — full filter inventory

The user must be able to combine any of these freely; all filters AND together; multi-select fields OR within themselves.

**Location**
- City (multi-select; populated from distinct `city` values)
- Area / `areaPath` segment (multi-select; hierarchical chips like `Lammhult`)
- Postcode prefix (text)
- "Within map view" toggle — restricts results to the current map viewport

**Property basics**
- Rooms (multi-select chips: 1, 2, 3, 4, …)
- Area (m²) — range slider (min/max)
- Floor (multi-select from `floorDisplayName` values: BV, KV, sut, 0, 1, 2, …, 13)
- Property type (`type`) — multi-select; currently only `residential`, but UI should not assume singularity

**Cost**
- Rent (kr/month) — range slider
- Cost-per-m² (derived) — range slider

**Dates**
- Available from — date range (move-in window)
- Application deadline — preset chips ("Today", "Next 3 days", "Next 7 days", "This month") + custom range
- Hide listings whose deadline has already passed (default: on)

**Competition / queue**
- Queue points — range slider (with explicit "include listings with no queue-points data" toggle, since the field is nullable)
- Competition tier — multi-select (low / medium / high / very high / unknown)

**Content quality**
- Has floor plan (boolean)
- Has multiple images (boolean; threshold e.g. ≥ 3)
- Has description (boolean)

**Listing status**
- Include ended listings (default: off). When on, the result set extends to historical rows and surfaces `endDate`.

**Free-text search** — matches the global search box from the header (kept here too for parity).

A "Reset all" affordance clears everything; a "Save this filter set" affordance persists it for one-click recall.

### 3.4 Sort options

- Newest first (by `availableFrom` or by record creation, whichever the design agent prefers — default for browsing)
- Deadline soonest
- Rent: low → high / high → low
- Area: large → small / small → large
- Cost-per-m²: low → high
- Queue points: low → high (least competitive first) / high → low
- Rooms: many → few

### 3.5 Map view

- One pin per listing, placed at `latitude` / `longitude`.
- Pin click → compact card popover (same content as §3.2 minus the cover image, or with a thumbnail).
- Pin clusters when many overlap.
- Map respects all active filters.
- Bidirectional sync with list: hovering a list item highlights its pin, and vice-versa (in split view).
- "Search this area" button reruns the query against the current viewport when the user pans.

### 3.6 Empty / loading / error states

- **Loading** — skeleton placeholders for cards and map.
- **Empty (no matches)** — message + "Clear filters" affordance + suggestion of the closest non-empty filter relaxation.
- **Data error** — message + retry, falling back to last successful data if available.
- **Stale data warning** — shown if `lastDetailFetchedAt` is older than a threshold for many listings.

---

## 4. Listing Detail page

Reached by clicking a card. The URL should embed `internalId` (so re-listings of the same `id` are distinguishable in history).

### 4.1 Sections (top-to-bottom is a suggestion; design agent decides arrangement)

**Hero / gallery**
- Full image gallery from `images[]` (lightbox / carousel)
- Floor plan(s) from `floorplans[]` (separate viewer or tab within the gallery)
- Cover fallback to `imageUrl` when `images[]` is empty

**Headline block**
- `displayName`
- Full `completeAddress`
- `areaPath` rendered as breadcrumbs (e.g. `Lammhult`)
- Save / bookmark toggle
- Share link (copy to clipboard)
- External link to the original Vidingehem listing
- "Open in Google Maps" link built from `completeAddress`

**Key facts grid** (each item is a labeled value)
- Rent (kr/month)
- Area (m²)
- Rooms
- Floor (`floorDisplayName`)
- Cost-per-m² (derived)
- Property type
- Local id (`localId`) and listing number (`number`)
- `areaName`

**Application status block**
- `availableFrom` (move-in date, plus "in X days" derived)
- `applicationDeadline` (full timestamp, plus countdown, plus "passed" state)
- `queuePoints` cut-off (numeric value or "no cut-off yet" when null)
- Competition tier (derived)
- A "How to apply" CTA that links out to the Vidingehem listing

**Description**
- Free-text `description` (Swedish), preserving paragraph breaks
- Optional: translate-to-English toggle (out of scope unless cheap)

**Location**
- Small embedded map centred on this listing's `latitude` / `longitude`
- Nearby listings (≤ N km, same `city`, or same `areaPath` segment) — horizontal scroll of cards

**Data provenance**
- "Last updated" — `lastDetailFetchedAt`
- Source — Vidingehem

**Listing history** (when this `id` has been listed multiple times)
- Timeline of prior instances with their `availableFrom`, deadline, and final `queuePoints` cut-off

### 4.2 Ended-listing variant

When `endDate` is non-null, the page is rendered in a read-only "this listing has ended" mode:
- A prominent ended-state banner
- `endDate` shown alongside the deadline
- "Apply" CTA is hidden / disabled
- All other information remains visible (useful for research)

### 4.3 Within-detail navigation

- Previous / next listing within the *currently filtered* result set (preserves the user's browse context)
- Back to results (preserves scroll position, filter state, view mode)

---

## 5. Saved / Watchlist page

- Same card layout as Home
- Sorted by the user's add-order by default, but with the same sort options as §3.4
- Per-item notes (free-text)
- Bulk actions: export saved IDs, clear all

---

## 6. Cross-cutting concerns the design agent must keep in mind

- **Mobile-first feasibility.** Filter panel on mobile is a sheet, map view is full-screen with a list peek, detail page is a single column.
- **Filter state lives in the URL.** All active filters, sort, and view mode are reflected as query params so any view is shareable and back/forward works.
- **No authentication.** Personalisation (saved listings, saved filters, notes) is per-device.
- **Locale.** Data is Swedish (city names, descriptions, floor labels). UI chrome can be English; data values pass through unchanged.
- **Nullables to respect.** `queuePoints`, `endDate`, `description`, `floorDisplayName`, `images`, `floorplans` can all be null/empty. Every view must have a defined empty state for these.
- **Stickiness of `queuePoints`.** A non-null value is authoritative even if a later snapshot would have been null — the frontend simply trusts the field.
- **Re-listings.** `(id, availableFrom)` is the identity. The UI distinguishes a re-listing from the original instance and lets the user see the history of an external `id`.
- **Images.** Listings carry roughly three images on average plus an optional floor plan; the cover thumbnail comes from `imageUrl`, and the detail page has the full gallery.

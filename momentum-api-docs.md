# Momentum API Documentation

This document outlines the API endpoints used to fetch residential property listings and their details from the Vidingehem Momentum API.

## Mandatory Headers
For all requests, the following headers are mandatory. The `Authorization: Bearer` token is **not** required for fetching public property data, including queue points.

```http
X-Api-Key: pJnKrR6B3FzRNFsF33xL8LhSs55KPJrm
X-Momentum-Client-Id: EAP-50486
Accept: application/json, text/plain, */*
```

---

## 1. Get Entire Listing

Fetches a list of all available residential properties. The limit can be adjusted (e.g., `limit=1000` to fetch all items).

**cURL Command:**
```bash
curl -s 'https://vidingehem-fastighet.momentum.se/Prod/Vidingehem/PmApi/v2/market/objects?type=residential&limit=1000' \
  -H 'X-Api-Key: pJnKrR6B3FzRNFsF33xL8LhSs55KPJrm' \
  -H 'X-Momentum-Client-Id: EAP-50486' \
  -H 'Accept: application/json, text/plain, */*'
```

**Response Structure (per item in `items` array):**
The response contains an object with `count` (total items) and `items` (an array of property objects).
Each item in the list contains the following fields:
- `id`: The unique identifier for the property (used for the detail API)
- `localId`
- `displayName`
- `description`
- `type`
- `pricing` (Object containing rent details)
- `location` (Object containing area and address information)
- `availability` (Object containing availableFrom date)
- `size` (Object containing area and shortRoomsDisplayName)
- `thumbnail`
- `queueType`

*Note: The list API does **not** return queue point information.*

---

## 2. Get Individual Property Details

Fetches comprehensive details for a specific property using its `id`.

**cURL Command:**
```bash
curl -s 'https://vidingehem-fastighet.momentum.se/Prod/Vidingehem/PmApi/v2/market/objects/{PROPERTY_ID}' \
  -H 'X-Api-Key: pJnKrR6B3FzRNFsF33xL8LhSs55KPJrm' \
  -H 'X-Momentum-Client-Id: EAP-50486' \
  -H 'Accept: application/json, text/plain, */*'
```
*(Replace `{PROPERTY_ID}` with the `id` from the list response, e.g., `6wgMmqtytWB4tThXCcHvDtMT`)*

**Response Structure:**
The detail response is a single object containing extensive information:
- `id`
- `localId`
- `number`
- `displayName`
- `type`
- `pricing`
- `location`
- `availability`
- `application` (Contains application deadline, `openTo`)
- `size`
- `files` (Contains images and floor plans)
- `attributes`
- `economy`
- `included`
- `info`
- `nrApplications`
- `publishing`
- `queueType`
- `queueTypeDisplayName`
- **`queuePointsCurrentPositionX`**: Represents the current queue points required for the property.

### About `queuePointsCurrentPositionX`
- **Nullable:** This field is often `null` for newly published properties or properties that do not yet have a queue cut-off established.
- **Public Data:** It does not require user authentication (no Bearer token needed). It is publicly exposed if you provide the valid API Key and Client ID.
- **Location:** It is exclusively found in the Detail API response, not in the List API response.

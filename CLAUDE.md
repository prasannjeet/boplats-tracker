# boplats-tracker

Housing tracker for Vidingehem properties via the Momentum API v2.

## Building

This project requires **Java 25** and uses a corporate Maven wrapper (`mymvn`).

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-25.jdk/Contents/Home
mymvn compile
mymvn test
```

## Architecture

Two jobs in `HouseSyncService`:

- **`syncHouseList()`** — `@Scheduled` every `listApiCacheDurationHours` (default 24h). Fetches the full property list and reconciles it against the active rows; then sweeps any house whose `applicationDeadline` has passed and ends it. The order matters: reconcile-then-sweep means a listing that the API still returns past its deadline is matched (and updated) before being ended, instead of producing a duplicate row that violates the `(id, available_from)` unique constraint. The sweep runs unconditionally — even when the API hiccups and returns an empty response — because past-deadline rows shouldn't linger as active just because the API was unhealthy. Active rows missing from a healthy API response are marked with `end_date`.
- **`syncHouseDetails()`** — self-rescheduling via `TaskScheduler` (no `@Scheduled`). After each detail fetch the next delay is recomputed as `detailRefreshIntervalHours / activeHouseCount`, clamped to `[detailFetchMinDelaySeconds, detailFetchMaxDelaySeconds]`. When no houses are active the loop polls every `detailFetchIdleDelaySeconds`. Each tick processes exactly one stale active house, so cadence is a consequence of workload rather than a magic constant.

### Data model

A listing instance is uniquely `(id, availableFrom)`, not `id` alone. The `house` table has a surrogate `internal_id` BIGINT primary key plus a unique constraint on `(id, available_from)`. Same external `id` with a different `availableFrom` is treated as a new row — re-listed properties get a fresh row and never mutate the historical one. API items missing `availableFrom` are skipped with a warning (we have not observed null `availableFrom` in production data; sentinel handling would hide debugging signal).

### Queue points

`queuePointsCurrentPositionX` is nullable — it appears only after a listing has enough applicants to establish a cut-off. Once a non-null value is captured, it is sticky: a later null response from the API does not overwrite it. A new non-null value does overwrite, so a moving cut-off is reflected.

### Design history

Background and rationale for the current sync model: `docs/superpowers/specs/2026-05-06-house-sync-redesign-design.md`.

## Key env vars

| Var | Default | Purpose |
|-----|---------|---------|
| `VB_URL` | — | Momentum API base URL |
| `VB_API_KEY` | — | API key header value |
| `SLACK_WEBHOOK_URL` | — | Slack incoming webhook |
| `VX_PREFIX_LINK` | — | URL prefix for property links in Slack messages |
| `LIST_API_CACHE_DURATION_HOURS` | 24 | Hours between full list syncs |
| `DETAIL_REFRESH_INTERVAL_HOURS` | 12 | Hours before a house's details are considered stale |
| `DETAIL_FETCH_MIN_DELAY_SECONDS` | 30 | Lower bound on adaptive detail-fetch delay |
| `DETAIL_FETCH_MAX_DELAY_SECONDS` | 1800 | Upper bound on adaptive detail-fetch delay |
| `DETAIL_FETCH_IDLE_DELAY_SECONDS` | 1800 | Delay used when no houses are active |

## Legacy

- `Homes` + `HomesRepository` (in `jpa/`) — map to the old `homes` DB table which still exists.
- `legacy/HomeSearchConfig` + `legacy/MarketPlaceDescription` — user preference types kept because `SlackService` is retained for future use.
- `SlackService` / `SlackServiceImpl` / `SlackClient` — Slack notification path is intact but no longer scheduled. Kept available for future re-introduction.

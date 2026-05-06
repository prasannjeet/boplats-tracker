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

Two scheduled jobs in `HouseSyncService`:

- **`syncHouseList()`** — runs every `listApiCacheDurationHours` (default 24h). Fetches the full property list from the API, upserts into the `house` table, marks removed properties with `end_date`.
- **`syncHouseDetails()`** — runs every `detailApiCallIntervalSeconds` (default 60s). Picks up to `detailSyncBatchSize` (default 5) active houses whose `last_detail_fetched_at` is null or older than `detailRefreshIntervalHours` (default 12h), fetches full detail per property (address, deadline, image, queue points), saves back.

Queue points (`queuePointsCurrentPositionX`) are nullable — they appear only after a listing has enough applicants to establish a cut-off. The detail sync re-fetches all active houses on a rolling cycle, so queue points are captured whenever they appear.

## Key env vars

| Var | Default | Purpose |
|-----|---------|---------|
| `VB_URL` | — | Momentum API base URL |
| `VB_API_KEY` | — | API key header value |
| `SLACK_WEBHOOK_URL` | — | Slack incoming webhook |
| `VX_PREFIX_LINK` | — | URL prefix for property links in Slack messages |
| `LIST_API_CACHE_DURATION_HOURS` | 24 | Hours between full list syncs |
| `DETAIL_API_CALL_INTERVAL_SECONDS` | 60 | Seconds between detail sync ticks |
| `DETAIL_REFRESH_INTERVAL_HOURS` | 12 | Hours before a house's details are considered stale |
| `DETAIL_SYNC_BATCH_SIZE` | 5 | Houses processed per detail sync tick |

## Legacy

- `Homes` + `HomesRepository` (in `jpa/`) — map to the old `homes` DB table which still exists.
- `legacy/HomeSearchConfig` + `legacy/MarketPlaceDescription` — user preference types kept because `SlackService` is retained for future use.
- `homeSearchConfig.json` — legacy user preference file (no longer loaded; kept for reference).

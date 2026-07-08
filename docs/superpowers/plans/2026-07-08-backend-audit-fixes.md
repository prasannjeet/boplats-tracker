# Backend Audit Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the confirmed `end_date` drift data bug, silence recovered-failure log noise, purge dead dependencies and dead code, and remove two operational foot-guns (open sync endpoint, infinite startup retry loop).

**Architecture:** No structural changes. One behavioral guard in `HouseSyncService.syncHouseList()`, one Liquibase data-repair changeset, log-level adjustments, a stdlib rewrite of `SlackClient`, pom cleanup, and deletions. Every phase leaves the build green and is independently committable/deployable.

**Tech Stack:** Java 25, Spring Boot 4.0.5, Maven (`./mvnw`), JUnit 5 + Mockito + AssertJ, Liquibase, MySQL 8.4 (prod) / H2 (tests).

---

## Context an engineer needs (verified 2026-07-08)

### The system

Housing tracker for Vidingehem properties via the Momentum API v2. Backend is Spring Boot serving a REST API (`/api/houses`, `/api/object-types`) plus a bundled Vue SPA. Two sync jobs in `HouseSyncService`:

- `syncHouseList()`: `@Scheduled` every 24h. Fetches the full property list (~1300 items), reconciles against DB rows keyed by `(id, available_from)` (a listing instance is that pair, NOT `id` alone; re-listed properties get a fresh row), then sweeps past-deadline rows via `markPastDeadlineEnded`.
- `syncOneHouseDetail()`: self-rescheduling tick, processes exactly one stale active house per tick, delay adapts to active-house count (~107s at the current 405 active houses).

### The confirmed bug (Phase 1)

The reconcile loop calls `house.setEndDate(null)` unconditionally for every item the API returns. The Momentum API keeps returning listings long after their application deadline passed. So every daily sync revives ~900 dead rows, and the sweep immediately re-ends them with TODAY's date. Verified in the prod DB (2026-07-08):

- 914 rows had `end_date = 2026-07-08` (the day of inspection);
- 1438 rows had `end_date` drifted more than 3 days past `DATE(application_deadline)`.

Historical "when did this listing end" data is being overwritten daily. The fix: only revive a row whose stored `applicationDeadline` is null or still in the future. A Liquibase changeset repairs the already-drifted rows.

Known pre-existing limitation (do NOT try to fix): if Momentum extends a deadline after our sweep ended the row, we never notice, because detail refresh only touches rows with `end_date IS NULL`. This was equally true before the fix (the sweep re-ended revived rows in the same transaction before any detail tick could see them).

### The log noise (Phase 2)

Over 2 weeks of prod logs: 815 ERROR lines, ALL of them transient detail-fetch failures (387x HTTP 400, 7x 407, 7x 500) that self-healed on the next cycle. Each failure logs TWICE at ERROR (once in `VaxjobostaderClient.getPropertyDetail`, once with full stack trace in `HouseSyncService.syncOneHouseDetail`). Spot-checked failing IDs: each failed 1 to 3 times and all now have complete data. The retry loop is already correct (`finally` always advances `lastDetailFetchedAt`, so nothing blocks the queue). Only the log severity is wrong.

### Deployment facts

- Deployed on Coolify (host `testenv` = 192.168.0.20 via SSH). App container matches `docker ps` filter on image `docker.nexus.coolify.ooguy.com/vb-back:latest`; at audit time it was `ngywitt8k6cmhy6pnhsg4u0e-164702665288` (container name changes per deploy, the `ngywitt8k6cmhy6pnhsg4u0e` prefix is the stable Coolify resource id).
- MySQL container on the same host: `zmlnnw4bxdd7kkkcllafkydc`, database `vaxjo`, user `vaxjo`. Password: read the app container's `DATABASE_PASSWORD` env var via `docker inspect`.
- Container restart policy is `unless-stopped` (verified), so removing the `while(true)` retry loop in `main()` is safe: Docker restarts the container if startup fails.
- CI (`.github/workflows/ci-cd.yml`) builds on push to `master`, runs tests, and pushes the image to the Nexus registry. Coolify then deploys that image.
- Liquibase runs at app startup, before the schedulers, so the repair changeset and the code guard land atomically in one deploy.

### Build on this machine (Linux dev box)

The repo CLAUDE.md mentions `mymvn` and a macOS `JAVA_HOME`; neither exists here. Use the Maven wrapper with the sdkman JDK 25:

```bash
cd /home/dev/documents/projects/boplats-tracker
export JAVA_HOME=$HOME/.sdkman/candidates/java/25.0.2-amzn
./mvnw test          # unit tests
```

The default `java` on PATH is 21 and will NOT compile this project; always export `JAVA_HOME` first. The pom also builds the Vue frontend via frontend-maven-plugin during packaging phases; plain `test` may download node tooling into `.frontend-tools/` on first run. That is normal.

### Commit conventions (from AGENTS.md, override any defaults)

- Commit as the repository's already-configured author ONLY. NO `Co-Authored-By:` trailer. NO mention of Claude/agent/"Generated with" anywhere in the message.
- No em dash characters in any user-facing string (log messages count). Use commas/colons instead.
- One commit per task, message style follows the repo's existing `feat:`/`fix:` prefix convention (see `git log --oneline`).

### Explicitly out of scope (user decisions)

- `GeocodingProvider` interface with a single implementation: KEEP as-is (user chose to keep it).
- `Homes`, `HomesRepository`, `legacy/` package, the whole Slack notification path: kept deliberately per CLAUDE.md. The Slack path gets its HTTP client modernized (Phase 3) but is NOT deleted.
- `jackson-databind` 2.18.2 explicit dependency: KEEP. Spring Boot 4's web stack uses Jackson 3 (`tools.jackson`); the Momentum client, `StaticUtils`, `WcfDateDeserializer`, and the `Beans.objectMapper()` bean all use Jackson 2 (`com.fasterxml`), which Boot 4 does not provide. The pin is internally consistent (databind pulls matching core/annotations).
- Frontend (`frontend/`): untouched.

---

## Phase 1: end_date drift fix

### Task 1: Guard against reviving past-deadline rows

**Files:**
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/service/HouseSyncService.java` (reconcile loop, the `house.setEndDate(null);` line, around line 148)
- Test: `src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java`

- [ ] **Step 1: Write two failing tests**

Add to `HouseSyncServiceTest` in the `// -------- Composite-key reconcile --------` section, after `reconcile_updatesExistingEndedOnMatchAndClearsEndDate`. Note the existing helpers used: `house(String, Date)`, `listItem(String, Date, double)`, `date(int, int, int)`. Deadlines are built relative to the real clock because `syncHouseList()` uses `new Date()` internally; fixed calendar dates would rot.

```java
    @Test
    void reconcile_doesNotRevivePastDeadlineEndedRow() throws Exception {
        Date avail = date(2026, 6, 1);
        Date pastDeadline = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));
        Date originalEnd = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(29));
        House existing = house("A", avail);
        existing.setApplicationDeadline(pastDeadline);
        existing.setEndDate(originalEnd);
        existing.setRent(100.0);

        when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", avail, 250.0)));
        when(repository.findAllByEndDateIsNull()).thenReturn(List.of());
        when(repository.findAllByExternalIds(any())).thenReturn(List.of(existing));

        service.syncHouseList();

        // Fields are still refreshed from the API...
        assertThat(existing.getRent()).isEqualTo(250.0);
        // ...but the historical end date must NOT be reset.
        assertThat(existing.getEndDate()).isEqualTo(originalEnd);
    }

    @Test
    void reconcile_revivesEndedRowWhoseDeadlineIsStillOpen() throws Exception {
        Date avail = date(2026, 6, 1);
        Date futureDeadline = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
        House existing = house("A", avail);
        existing.setApplicationDeadline(futureDeadline);
        existing.setEndDate(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)));

        when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", avail, 250.0)));
        when(repository.findAllByEndDateIsNull()).thenReturn(List.of());
        when(repository.findAllByExternalIds(any())).thenReturn(List.of(existing));

        service.syncHouseList();

        assertThat(existing.getEndDate()).isNull();
    }
```

Add the import at the top of the test file (the file does not have it yet):

```java
import java.util.concurrent.TimeUnit;
```

- [ ] **Step 2: Run the new tests, verify the first fails**

```bash
export JAVA_HOME=$HOME/.sdkman/candidates/java/25.0.2-amzn
./mvnw test -Dtest=HouseSyncServiceTest -pl . -q
```

Expected: `reconcile_doesNotRevivePastDeadlineEndedRow` FAILS (endDate was nulled, so `isEqualTo(originalEnd)` fails on an actual value of null). `reconcile_revivesEndedRowWhoseDeadlineIsStillOpen` PASSES (current behavior revives everything). If the first test does not fail, stop and re-check: the guard may already exist or the test is wrong.

- [ ] **Step 3: Implement the guard**

In `HouseSyncService.syncHouseList()`, replace the single line `house.setEndDate(null);` with:

```java
                    // Revive an ended row only while its stored deadline is still open.
                    // The API keeps returning listings long past their deadline; reviving
                    // them made the sweep re-end them with a fresh end_date every day,
                    // destroying the historical end date (confirmed in prod: 1438 rows
                    // had drifted). New rows and rows without a fetched deadline are
                    // unaffected (deadline == null).
                    Date deadline = house.getApplicationDeadline();
                    if (deadline == null || deadline.after(now)) {
                        house.setEndDate(null);
                    }
```

`now` is the `Date now = new Date();` already captured at the top of `syncHouseList()`.

- [ ] **Step 4: Run the full sync test class, verify all pass**

```bash
./mvnw test -Dtest=HouseSyncServiceTest -q
```

Expected: ALL tests pass, including the pre-existing `reconcile_updatesExistingEndedOnMatchAndClearsEndDate` (its existing row has `applicationDeadline == null`, so the guard still revives it).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/prasannjeet/vaxjobostader/service/HouseSyncService.java src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java
git commit -m "fix: do not revive past-deadline rows during list reconcile

The API returns listings long after their deadline. Unconditionally
clearing end_date revived ~900 dead rows per sync, and the deadline
sweep re-ended them with the current date, overwriting the historical
end date daily (1438 drifted rows observed in prod)."
```

### Task 2: Liquibase repair changeset for already-drifted rows

**Files:**
- Create: `src/main/resources/db/changelog/house/repair-end-date-drift.xml`
- Modify: `src/main/resources/db.changelog-master.xml`

- [ ] **Step 1: Create the changeset**

Create `src/main/resources/db/changelog/house/repair-end-date-drift.xml`. The `dbms` filter restricts it to MySQL/MariaDB because `DATE()` is not H2-portable; H2 (tests) skips it cleanly. It intentionally repairs only rows whose `end_date` landed AFTER the deadline (drift victims). Rows ended before their deadline (removed from the API early) are correct and untouched.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                      http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd">

    <!-- One-time data repair. Before the reconcile guard (see
         HouseSyncService.syncHouseList), every daily sync revived rows the
         API still returned past their deadline, and the sweep re-ended them
         with the current date. end_date therefore kept sliding forward for
         dead listings. The application deadline is the best available
         approximation of when those listings actually ended. -->
    <changeSet id="repair-end-date-drift" author="prasannjeet" dbms="mysql, mariadb">
        <sql>
            UPDATE house
            SET end_date = DATE(application_deadline)
            WHERE end_date IS NOT NULL
              AND application_deadline IS NOT NULL
              AND end_date &gt; DATE(application_deadline);
        </sql>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: Include it in the master changelog**

In `src/main/resources/db.changelog-master.xml`, add as the LAST include, after the `object-type-table.xml` line:

```xml
    <include file="db/changelog/house/repair-end-date-drift.xml" relativeToChangelogFile="true"/>
```

- [ ] **Step 3: Verify tests still pass (H2 context boot runs Liquibase and must skip the changeset)**

```bash
./mvnw test -q
```

Expected: BUILD SUCCESS. `ApplicationTests` boots the full context against H2; the new changeset is filtered out by `dbms` and must not error.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/changelog/house/repair-end-date-drift.xml src/main/resources/db.changelog-master.xml
git commit -m "fix: repair drifted end_date values from pre-guard revive churn"
```

## Phase 2: Log noise reduction

### Task 3: Transient detail-fetch failures log once, at WARN, without stack trace

**Files:**
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/client/VaxjobostaderClient.java` (`getPropertyDetail`, around line 111)
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/service/HouseSyncService.java` (`syncOneHouseDetail` catch block, around line 258)

Context: a failed detail fetch is routine (3 to 4 percent of fetches, always recovered on the next 12h cycle because `lastDetailFetchedAt` advances in `finally`). It currently produces two ERROR lines plus a 27-frame stack trace per failure. The status code is the entire diagnostic story. List-sync failures stay at ERROR in `syncHouseList`'s catch-all: a failed daily list sync IS significant.

- [ ] **Step 1: Move the status detail into the exception, drop the client-side ERROR line**

In `VaxjobostaderClient.getPropertyDetail`, replace:

```java
        if (response.statusCode() != 200) {
            log.error("Failed to fetch detail for ID: {}. Status: {}", id, response.statusCode());
            throw new IOException("Failed to fetch detail from Momentum API");
        }
```

with:

```java
        if (response.statusCode() != 200) {
            throw new IOException(
                "Detail fetch for " + id + " returned HTTP " + response.statusCode());
        }
```

- [ ] **Step 2: Downgrade the service-side log to a single WARN line**

In `HouseSyncService.syncOneHouseDetail`, replace:

```java
        } catch (Exception e) {
            log.error("Failed to fetch detail for house {}.", house.getId(), e);
        } finally {
```

with:

```java
        } catch (Exception e) {
            // Transient and self-healing: the finally block advances the
            // timestamp, so this house simply retries next cycle. Not an ERROR.
            log.warn("Detail fetch failed for house {}, will retry next cycle: {}",
                house.getId(), e.getMessage());
        } finally {
```

- [ ] **Step 3: Run tests**

```bash
./mvnw test -q
```

Expected: BUILD SUCCESS (no test asserts on these log strings).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/prasannjeet/vaxjobostader/client/VaxjobostaderClient.java src/main/java/com/prasannjeet/vaxjobostader/service/HouseSyncService.java
git commit -m "fix: log transient detail-fetch failures once at WARN without stack trace

All 815 ERROR lines in two weeks of prod logs were recovered 400/407/500
blips, each logged twice. Reserve ERROR for failures that need action."
```

## Phase 3: Dependency purge

### Task 4: Rewrite SlackClient on java.net.http, drop SetUtils from SlackServiceImpl

**Files:**
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/client/SlackClient.java` (full rewrite, same public surface)
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/service/SlackServiceImpl.java` (`getNewHomesObjectNos`, `sendSlackNotification`)

Context: `SlackClient` is the ONLY user of `commons-httpclient` 3.1 (EOL 2007, known CVEs); `SlackServiceImpl.getNewHomesObjectNos` is the ONLY user of `commons-collections4`. The Slack path is kept (CLAUDE.md: retained for future re-introduction) but is not scheduled anywhere, so there is no runtime behavior to regress. Both callers of `sendSlackNotification` are `@SneakyThrows`, so widening its `throws` clause compiles fine. The project already uses `java.net.http.HttpClient` in `VaxjobostaderClient` and `NominatimGeocodingProvider`; this makes it three-for-three.

- [ ] **Step 1: Rewrite SlackClient**

Replace the entire content of `src/main/java/com/prasannjeet/vaxjobostader/client/SlackClient.java` with:

```java
package com.prasannjeet.vaxjobostader.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.prasannjeet.vaxjobostader.util.StaticUtils.getMapper;

@RequiredArgsConstructor
@Slf4j
public class SlackClient {

  private static final ObjectMapper mapper = getMapper();
  private final String slackUrl;

  public void sendSlackMessage(String message) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder(URI.create(slackUrl))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(getBody(message)))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    log.info("Slack Message Sent. Response: {}", response.body());

    if (!response.body().contains("ok")) {
      throw new RuntimeException(
          "Failed response received from slack. Response: " + response.body());
    }
  }

  String getBody(String message) {
    return mapper.createObjectNode()
        .put("text", message)
        .toString();
  }

}
```

(The `getHeader()` method disappears with the old API. Slack's webhook success response is the literal body `ok`, so the `contains("ok")` check is behavior-preserving.)

- [ ] **Step 2: Replace SetUtils.difference in SlackServiceImpl**

In `src/main/java/com/prasannjeet/vaxjobostader/service/SlackServiceImpl.java`:

Replace the method:

```java
  private Set<String> getNewHomesObjectNos(List<House> preferredHomes, UserSelectedHomes userSelectedHomes) {
    Set<String> preferredHomesObjectNos = getObjectNos(preferredHomes);
    Set<String> lastObjects = new HashSet<>(userSelectedHomes.getPreferredObjects());
    return difference(preferredHomesObjectNos, lastObjects).toSet();
  }
```

with:

```java
  private Set<String> getNewHomesObjectNos(List<House> preferredHomes, UserSelectedHomes userSelectedHomes) {
    Set<String> newObjectNos = new HashSet<>(getObjectNos(preferredHomes));
    newObjectNos.removeAll(userSelectedHomes.getPreferredObjects());
    return newObjectNos;
  }
```

Delete the now-unused import line:

```java
import static org.apache.commons.collections4.SetUtils.difference;
```

Then widen the private sender's throws clause. Replace:

```java
  private void sendSlackNotification(String webHook, String message) throws IOException {
```

with:

```java
  private void sendSlackNotification(String webHook, String message) throws IOException, InterruptedException {
```

- [ ] **Step 3: Compile and run tests**

```bash
./mvnw test -q
```

Expected: BUILD SUCCESS. (The `testbeans/SlackServiceImpl` used in tests is a separate no-op class; it is unaffected.)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/prasannjeet/vaxjobostader/client/SlackClient.java src/main/java/com/prasannjeet/vaxjobostader/service/SlackServiceImpl.java
git commit -m "refactor: move SlackClient to java.net.http, drop commons-collections4 usage"
```

### Task 5: pom.xml purge

**Files:**
- Modify: `pom.xml`

Context: verified by grep, `google-cloud-translate` and `mksapi-jar` have ZERO usages in `src/`. `commons-httpclient` and `commons-collections4` lost their last users in Task 4. `slf4j-simple` competes with Boot's logback at startup (SLF4J "multiple providers" warning); `slf4j-api` comes transitively from Boot at the right version. `mariadb-java-client` is a second, unused JDBC driver (the `jdbc:mysql://` URL selects `mysql-connector-j`); note the SEPARATE `mariadb-java-client` declaration inside the `liquibase-maven-plugin` `<dependencies>` block near line 373 which MUST STAY (the plugin uses it for CLI migrations). `h2` is compile-scoped and ships inside the production fat jar; tests are its only consumer.

- [ ] **Step 1: Remove six dependency blocks from `<dependencies>`**

Delete these blocks entirely (locations approximate, search by artifactId):

```xml
    <dependency>
      <groupId>org.mariadb.jdbc</groupId>
      <artifactId>mariadb-java-client</artifactId>
      <version>3.0.6</version>
    </dependency>
```

```xml
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-collections4</artifactId>
      <version>4.4</version>
    </dependency>
```

```xml
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>2.0.0</version>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-simple</artifactId>
      <version>2.0.0</version>
      <scope>runtime</scope>
    </dependency>
```

```xml
    <dependency>
      <groupId>com.google.cloud</groupId>
      <artifactId>google-cloud-translate</artifactId>
      <version>2.29.0</version>
    </dependency>
```

```xml
    <dependency>
      <groupId>commons-httpclient</groupId>
      <artifactId>commons-httpclient</artifactId>
      <version>3.1</version>
    </dependency>
```

```xml
    <dependency>
      <groupId>com.mks.api</groupId>
      <artifactId>mksapi-jar</artifactId>
      <version>4.16.2671</version>
    </dependency>
```

- [ ] **Step 2: Remove the google libraries-bom from `<dependencyManagement>`**

Its only purpose was the translate library. Delete:

```xml
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.google.cloud</groupId>
        <artifactId>libraries-bom</artifactId>
        <version>26.1.1</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
```

- [ ] **Step 3: Scope h2 to test**

Change:

```xml
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <version>2.1.214</version>
    </dependency>
```

to:

```xml
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <version>2.1.214</version>
      <scope>test</scope>
    </dependency>
```

- [ ] **Step 4: Verify the dependency tree is clean and slf4j has one provider**

```bash
./mvnw dependency:tree -q 2>/dev/null | grep -iE "mariadb|collections4|slf4j-simple|google-cloud|mksapi|commons-httpclient|h2database" || echo "CLEAN (only h2 in test scope expected)"
```

Expected: only `com.h2database:h2 ... :test` appears (and possibly transitive slf4j-api from Boot, which is fine). None of the removed artifacts appear at compile/runtime scope.

- [ ] **Step 5: Full test run**

```bash
./mvnw test -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add pom.xml
git commit -m "build: remove unused and duplicate dependencies, scope h2 to test

google-cloud-translate, mksapi-jar: zero usages. commons-httpclient,
commons-collections4: last users rewritten on stdlib. mariadb-java-client:
duplicate driver, jdbc:mysql URL selects mysql-connector-j (the copy inside
liquibase-maven-plugin stays). slf4j pins: Boot manages both, slf4j-simple
competed with logback. h2 was shipping in the production jar."
```

## Phase 4: Operational foot-guns

### Task 6: Remove the open manual-sync endpoint and the infinite startup retry loop

**Files:**
- Delete: `src/main/java/com/prasannjeet/vaxjobostader/controller/Listing.java`
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/Application.java`

Context, endpoint: `GET /list/update` is unauthenticated, `@CrossOrigin(origins = "*")`, and triggers a full Momentum list sync synchronously. Besides being an open door to hammering the upstream API, a manual trigger overlapping the `@Scheduled` run means two concurrent `syncHouseList` transactions doing read-then-insert on the same keys, which can violate the `(id, available_from)` unique constraint. The scheduler covers all real needs; verified no test references this controller. If a manual trigger is ever needed again: restart the container (the scheduler fires `fixedDelay` from startup).

Context, retry loop: `Application.main` wraps `SpringApplication.run` in `while(true)` with a 5s sleep, which hides fatal misconfiguration (a bad env var becomes a silent eternal crash loop with a one-line message) and duplicates what Docker already does. The deployed container's restart policy is `unless-stopped` (verified on the Coolify host), so Docker restarts on startup failure, with visibility in `docker ps` restart counts.

- [ ] **Step 1: Delete the controller**

```bash
git rm src/main/java/com/prasannjeet/vaxjobostader/controller/Listing.java
```

- [ ] **Step 2: Simplify Application.main**

Replace the entire content of `src/main/java/com/prasannjeet/vaxjobostader/Application.java` with:

```java
package com.prasannjeet.vaxjobostader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        // Startup failure is fatal on purpose: the container restart policy
        // (unless-stopped) handles retries with visibility, and a broken
        // configuration should crash loudly instead of looping silently.
        SpringApplication.run(Application.class, args);
    }
}
```

- [ ] **Step 3: Run tests**

```bash
./mvnw test -q
```

Expected: BUILD SUCCESS. (`ApplicationTests` boots the context directly, not via `main`; no test hits `/list/update`.)

- [ ] **Step 4: Commit**

```bash
git add -A src/main/java/com/prasannjeet/vaxjobostader/controller/Listing.java src/main/java/com/prasannjeet/vaxjobostader/Application.java
git commit -m "fix: remove open /list/update endpoint and infinite startup retry loop

The endpoint was unauthenticated with CORS *, and a manual trigger racing
the scheduled sync could violate the (id, available_from) unique
constraint. Docker's unless-stopped restart policy replaces the retry
loop with visible, backed-off restarts."
```

## Phase 5: Dead code sweep

### Task 7: Delete verified-unused classes, methods, and fields

**Files:**
- Delete: `src/main/java/com/prasannjeet/vaxjobostader/exception/ClientException.java`
- Delete: `src/main/java/com/prasannjeet/vaxjobostader/util/LoggingUtils.java`
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/jpa/HouseRepository.java` (remove one method)
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/service/geocoding/GeocodeOutcome.java` (remove one method)
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDetail.java` (remove one record component)
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/config/AppConfig.java` (remove redundant annotation)
- Modify: `src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java` (two helper constructors)
- Modify: `src/test/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDtoTest.java` (one assertion)

All of these were verified unused by grep during the audit (2026-07-08). Do NOT touch `Homes`, `HomesRepository`, `UserSelectedHomes`, `legacy/`, `PlaceName`, `HomeUtil`, or anything Slack-related: kept deliberately per CLAUDE.md. Do NOT touch `WcfDateDeserializer` (used by `HouseAvailability` and `HouseApplication`), `ListLobConverter`/`StringLobConverter` (used by legacy entities), or `GeocodingProvider` (user chose to keep the interface).

- [ ] **Step 1: Delete the two dead classes**

```bash
git rm src/main/java/com/prasannjeet/vaxjobostader/exception/ClientException.java src/main/java/com/prasannjeet/vaxjobostader/util/LoggingUtils.java
```

- [ ] **Step 2: Remove the unused repository method**

In `src/main/java/com/prasannjeet/vaxjobostader/jpa/HouseRepository.java`, delete the line:

```java
    Optional<House> findByIdAndAvailableFromAndEndDateIsNull(String id, Date availableFrom);
```

and the now-unused import:

```java
import java.util.Optional;
```

- [ ] **Step 3: Remove the unused convenience method on GeocodeOutcome**

In `src/main/java/com/prasannjeet/vaxjobostader/service/geocoding/GeocodeOutcome.java`, delete:

```java
    public Optional<Coordinates> coordinatesOptional() {
        return Optional.ofNullable(coordinates);
    }
```

and the now-unused import:

```java
import java.util.Optional;
```

- [ ] **Step 4: Remove the parsed-but-never-read `queueTypeDisplayName` from HouseDetail**

In `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDetail.java`, delete the component line `String queueTypeDisplayName,`. The record becomes:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseDetail(
    String id,
    String localId,
    String number,
    String displayName,
    String type,
    String queueType,
    String rentalObjectType,
    Integer nrApplications,
    HousePricing pricing,
    HouseLocation location,
    HouseAvailability availability,
    HouseApplication application,
    HouseSize size,
    HouseFiles files,
    Double queuePointsCurrentPositionX,
    List<HouseIncluded> included
) {}
```

(Parsing is unaffected: `@JsonIgnoreProperties(ignoreUnknown = true)` silently drops the JSON field.)

- [ ] **Step 5: Fix the two test helpers that construct HouseDetail positionally**

In `src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java`, both helpers lose one `null` argument (the old 7th component). Replace:

```java
    private static HouseDetail detailWithQueuePoints(Double qp) {
        return new HouseDetail(
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null,
            qp,
            null
        );
    }
```

with:

```java
    private static HouseDetail detailWithQueuePoints(Double qp) {
        return new HouseDetail(
            null, null, null, null, null,
            null, null, null,
            null, null, null, null, null, null,
            qp,
            null
        );
    }
```

and replace:

```java
    private static HouseDetail detailWithNewFields(String queueType, String rentalObjectType,
                                                    Integer nrApplications, List<HouseIncluded> included) {
        return new HouseDetail(
            null, null, null, null, null,
            queueType, null, rentalObjectType, nrApplications,
            null, null, null, null, null, null,
            null,
            included
        );
    }
```

with:

```java
    private static HouseDetail detailWithNewFields(String queueType, String rentalObjectType,
                                                    Integer nrApplications, List<HouseIncluded> included) {
        return new HouseDetail(
            null, null, null, null, null,
            queueType, rentalObjectType, nrApplications,
            null, null, null, null, null, null,
            null,
            included
        );
    }
```

- [ ] **Step 6: Fix the DTO test assertion**

In `src/test/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDtoTest.java` line 56, delete the line:

```java
            assertThat(detail.queueTypeDisplayName()).isEqualTo("Bostad");
```

(The surrounding assertions on other fields stay.)

- [ ] **Step 7: Remove the redundant annotation on AppConfig**

In `src/main/java/com/prasannjeet/vaxjobostader/config/AppConfig.java`, delete the `@EnableConfigurationProperties` annotation line and its import `org.springframework.boot.context.properties.EnableConfigurationProperties`. (`@Configuration` + `@ConfigurationProperties` on the same class already registers the bean; the no-arg enable annotation does nothing here.)

- [ ] **Step 8: Full test run**

```bash
./mvnw test -q
```

Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor: delete dead code

ClientException, LoggingUtils, HouseRepository.findByIdAndAvailableFromAndEndDateIsNull,
GeocodeOutcome.coordinatesOptional, HouseDetail.queueTypeDisplayName: zero callers.
Drop redundant @EnableConfigurationProperties on AppConfig."
```

## Phase 6: Verification and deploy

### Task 8: End-to-end verification, push, deploy, post-deploy checks

**Files:** none (verification only)

- [ ] **Step 1: Clean full build**

```bash
export JAVA_HOME=$HOME/.sdkman/candidates/java/25.0.2-amzn
./mvnw clean verify -q
```

Expected: BUILD SUCCESS. This also exercises the frontend build and the fat-jar packaging (which no longer contains h2: spot-check with `unzip -l target/vb-back-1.3.jar | grep -c h2database` returning 0 if curious).

- [ ] **Step 2: Review the whole diff once**

```bash
git log --oneline master@{u}..HEAD
git diff master@{u}..HEAD --stat
```

Expected: 7 commits, roughly -350 net lines. No unrelated files.

- [ ] **Step 3: Push and watch CI**

```bash
git push origin master
gh run watch --exit-status || gh run list --limit 1
```

Expected: CI green (build, tests, image pushed to `docker.nexus.coolify.ooguy.com/vb-back:latest`).

- [ ] **Step 4: Deploy via Coolify**

Coolify deploys the freshly pushed `vb-back:latest`. If auto-deploy on registry push is not configured, trigger the redeploy in the Coolify UI (resource id `ngywitt8k6cmhy6pnhsg4u0e`), or ask the user to. Then confirm the new container is up:

```bash
ssh testenv 'docker ps --format "{{.Names}}\t{{.Status}}" | grep ngywitt'
```

Expected: a container with the `ngywitt...` prefix, recently started.

- [ ] **Step 5: Post-deploy check 1, Liquibase repair ran**

```bash
ssh testenv 'C=$(docker ps --format "{{.Names}}" | grep ngywitt | head -1); PW=$(docker inspect $C --format "{{range .Config.Env}}{{println .}}{{end}}" | grep "^DATABASE_PASSWORD=" | cut -d= -f2-); docker exec zmlnnw4bxdd7kkkcllafkydc mysql -uvaxjo -p"$PW" vaxjo -N -e "SELECT COUNT(*) FROM house WHERE end_date IS NOT NULL AND application_deadline IS NOT NULL AND end_date > DATE(application_deadline);" 2>/dev/null'
```

Expected: `0` (was 1438 before the repair).

- [ ] **Step 6: Post-deploy check 2, the drift stays fixed after the next daily sync**

`@Scheduled(fixedDelay)` fires the first list sync shortly after startup, then every 24h. After at least one "House list sync complete" log line appears:

```bash
ssh testenv 'C=$(docker ps --format "{{.Names}}" | grep ngywitt | head -1); docker logs $C 2>&1 | grep "House list sync complete" | tail -3'
```

Expected: `ended-past-deadline` drops to a small number (only listings whose deadline genuinely passed since the previous sync), NOT ~900. And re-running the Step 5 query still returns 0.

- [ ] **Step 7: Post-deploy check 3, log noise gone**

After the app has run long enough to hit a transient detail failure (hours to days), or simply as an ongoing observation:

```bash
ssh testenv 'C=$(docker ps --format "{{.Names}}" | grep ngywitt | head -1); docker logs --since 24h $C 2>&1 | grep -c " ERROR "; docker logs --since 24h $C 2>&1 | grep "Detail fetch failed" | tail -3'
```

Expected: ERROR count near zero; transient failures appear as single WARN lines.

- [ ] **Step 8: Report results to the user**

Summarize: commits shipped, CI status, the three post-deploy checks with actual numbers, and any residual risks observed.

---

## Self-review notes

- Spec coverage: audit findings 1 (end_date drift) → Tasks 1+2; 2 (log noise) → Task 3; 3 (/list/update) → Task 6; 4 (h2 scope) → Task 5; 5 (commons-httpclient) → Tasks 4+5; 6 (dual drivers) → Task 5; 7 (slf4j pins) → Task 5; 8 (main retry loop) → Task 6; ponytail deletions (google-cloud-translate, mksapi, ClientException, LoggingUtils, unused repo method, coordinatesOptional, queueTypeDisplayName, EnableConfigurationProperties) → Tasks 5+7. Excluded by user decision: GeocodingProvider interface. Excluded per CLAUDE.md: legacy/Slack/Homes deletions. jackson-databind pin: investigated during planning and deliberately kept (Jackson 2 is genuinely used by the Momentum client stack; Boot 4 manages only Jackson 3).
- Deploy note: Phases are independently deployable, but deploying Phase 1 requires BOTH Task 1 and Task 2 to be meaningful (guard without repair leaves 1438 drifted rows; repair without guard re-drifts within a day). They ship in one jar, so the plan's single final deploy satisfies this automatically. If deploying incrementally, never deploy Task 2 without Task 1.
- The `markPastDeadlineEnded` bulk update flushes pending entity state first (JPQL `@Modifying` with default flush mode), so the guard's decision not to null `endDate` is what the DB commits. No flush-ordering surprise.


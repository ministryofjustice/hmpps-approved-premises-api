# 28. Application status signals for SAS (outbox-backed, deduplicated)

Date: 2026-03-31

## Status

Proposed

## Context

The Single Accommodation Service (SAS) needs CAS **application**, **placement**, and **request-for-placement** statuses to drive its rules engine without calling `GET /CAS/external/cases/{crn}/applications/suitable` for every row on the case list. The intended direction is an event-driven flow where SAS persists derived fields (for example on a `sas_case` table) and refreshes them when CAS data changes.

A spike (SAS-328) mapped how those statuses change in CAS and which existing **approved-premises** domain events fire at each point. That analysis showed:

- Many distinct granular domain events (order of a dozen or more) can affect the derived statuses.
- Several user actions change state without a corresponding domain event today, so "subscribe to everything that already exists" is incomplete unless CAS adds more events.
- Subscribing to **all** granular event types in SAS implies a large and fragile SQS filter policy; future work (for example CAS3 statuses) could repeat the same scaling problem.

The team needed a pattern that keeps SAS aligned with CAS state without N-per-row HTTP calls, minimises missed updates, and avoids an unmanageable fan-in of event types on the SAS side.

## Options

### 1. Consume existing CAS domain events (and add missing ones)

SAS subscribes to the many event types that can affect application status; where CAS does not publish an event, add one.

**Pros:** Aligns with the HMPPS pattern of granular domain events and audit in `domain_events`; relatively little change if only "missing" events are added.

**Cons:** SAS must subscribe to a large and growing set of event types; filter policies become heavy; risk of gaps if any trigger is mis-analysed; future CAS3 status work may multiply the same problem.

### 2. JPA entity lifecycle callbacks: emit a single umbrella event

CAS registers listeners on the relevant JPA entities and emits one new event type (for example `approved-premises.application.status-updated`) whenever persisted state changes in a way that can affect the derived statuses.

**Pros:** SAS listens to **one** event type; implementation is local to CAS persistence.

**Cons:** Callbacks can fire multiple times per logical operation; emitting only via SNS without a durable outbox step loses a clear audit of what was published and complicates "exactly once" style reasoning; pairing with the existing granular domain-events model could mean duplicate notifications unless carefully designed.

### 3. JPA lifecycle callbacks → **outbox** → deduplicated publish (chosen)

Same umbrella event type as option 2, but **persist** each intended notification to an outbox table (for example `sas_outbox_event`). A scheduled component (for example `SasEventPublisher`) reads pending rows, **deduplicates** by event type and CRN (and equivalent keying), then publishes **one** message per logical change burst. Publish the resulting messages to a **SAS-only** SNS topic (not the HMPPS-wide `domain-events` topic used for general domain events).

**Pros:** Reduces noise from duplicate JPA callbacks; provides a durable audit of what was queued and published; keeps SAS integration on a single event type; avoids polluting global timelines with duplicate "status updated" alongside every granular domain event.

**Cons:** More CAS implementation work than option 1’s "add listeners only"; still requires productionisation of listeners and outbox semantics (same order of magnitude as hardening option 2).

### 4. Scheduled job querying `domain_events`

CAS publishes an umbrella event by scanning `domain_events` on a schedule instead of SAS subscribing to many types.

**Pros:** Fewer moving parts on the subscriber side.

**Cons:** If the scan misses a change or lags, SAS can drift; options 2 and 3 tie updates more directly to persistence of the entities that drive the statuses.

## Decision

Adopt **option 3**: use JPA entity lifecycle callbacks on CAS entities that influence the derived statuses to record **`approved-premises.application.status-updated`** (or equivalent) into a **SAS-facing outbox**, run a **deduplicating publisher**, and deliver to a **dedicated SAS SNS topic**.

Granular domain events remain the canonical HMPPS audit trail where they already exist; this path is **additional** SAS-specific signalling, not a replacement for the general domain-event pipeline for other consumers.

## Consequences

- **SAS** can subscribe to a **single** topic (or filtered stream) for CAS application-status refreshes and update local projections without per-case synchronous calls to the "suitable applications" endpoint.
- **CAS** owns correctness of **when** to enqueue an outbox row (entity graph changes that affect derived statuses); the **deduplicator** owns **how often** SAS receives a publish for a given CRN in a window.
- **Operational** requirements: monitoring outbox depth, publisher failures, and dead-letter handling for the SAS topic; configuration of ingress/base URL for any `detailUrl` pointing back into CAS APIs for full event detail.
- **Implementation note:** The first slice (e.g. commit `c5daa8a25`) introduces JPA `@EntityListeners` on `ApprovedPremisesApplicationEntity`, `PlacementApplicationEntity`, and `CASSpaceBookingEntity`, tracks application status changes for post-update filtering, and emits `CAS_APPLICATION_STATUS_UPDATED` with a `detailUrl` to the existing "suitable applications" resource. **Follow-up work** should align persistence and publishing with the outbox table, deduplication job, and SAS-dedicated SNS topic described above.

## Caveats

- JPA callbacks can still fire more often than a single business operation; the outbox and deduplication step are **required** to deliver the intended signal-to-noise ratio for SAS.
- Endpoints that change derived state **without** touching the instrumented entities may still require additional hooks or domain events; this ADR does not remove the need for completeness when mapping triggers.
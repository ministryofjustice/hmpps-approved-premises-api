# 23. Begin extracting CAS1 code to its own namespace

Date: 2024-05-15

## Status

Accepted

## Context

The shared CAS API currently consists of a mixture of:

- endpoints, controllers and services which service 2 or 3 of the CAS services
- code which is service-specific

The reason for the amount of shared code is partly historic. First came CAS1, and then CAS3
was procured with a very tight development timescale. To fast-track that development, we
took the deliberate step to take on technical debt by extending existing CAS1 code to
handle the somewhat similar, and somewhat different, needs of CAS3.

There are areas where servicing all 3 services from the same endpoints / classes /
functions makes good sense but as the 3 services grow in complexity and are under active
development by 3 separate teams it is now often clearly more practical and less risky to
extract service-specific work into simpler service-specific code.

For more information on how CAS2 and CAS3 have worked to decouple their service-specific
code into their own namespaces see:

- [ADR 18. Introduce CAS2 API namespace] [cas2_adr] from Sept 2023
- [ADR 20. Introduce CAS3 API namespace] [cas3_adr] from Feb 2024

CAS1 is now beginning work on the Match and Manage areas where availability and occupancy
of accommodation are managed. We are starting with some work to extend the capabilities of
"out of service" accommodation which is currently modelled as "lost beds" in code shared
with CAS3.

## Decision

We will extract the CAS1 "lost bed" code to service-specific endpoints and modules which we
can extend according to our developing needs.

We'll move existing functionality to specific-specific code  e.g. by:

- exposing endpoints such as `GET /cas1/manage/out-of-service-beds`
- handling requests with an `OutOfServiceBedsController` in the `approvedpremisesapi.controller.cas1`
  namespace
- using an `OutOfServiceBedService` in the `approvedpremisesapi.service.cas1` namespace

## Consequences

This will result in more overall components and classes but these elements will be smaller,
easier to understand and maintain and less risky to change.

We've seen these benefits already in the similar work done by CAS2 and CAS3.

[cas2_adr]:
https://github.com/ministryofjustice/approved-premises-api/blob/main/doc/architecture/decisions/0018-introduce-cas2-api-namespace.md
"ADR 18. Introduce CAS2 API namespace"

[cas3_adr]:
https://github.com/ministryofjustice/approved-premises-api/blob/main/doc/architecture/decisions/0020-introduce-cas3-api-namespace.md
"ADR 20. Introduce CAS3 API namespace"
# 13. transition-internal-service-naming-towards-casx

Date: 2023-06-16

## Status

Accepted

## Context

- Approved Premises (CAS1) was the first project and set their Frontend and API
  up accordingly
- Temporary Accommodation (CAS3) joined the space and became the second user of
  the API
  - During this transition the team has carried various changes to support
      different services. In doing so introduced "temporary-accommodation" as
      the language alongside "approved-premises":
    - X-Service-Name header
    - Multiple database tables have columns such as `service`, `serviceName` and
     `type` that identify which record a service belongs to
- CAS2 (yet to have an alternate name) is joining to become the third user of
  the API

A question was raised that given CAS2 didn't yet have a name, what should they
identify as?

All three services are tiers of the Community Accommodation Service (CAS) which
is unlikely to change from a programme level and it's also what the shared
infrastructure is already called. "Approved Premises" and "Temporary
Accommodation" are much more likely to change.

Externally we will still want our repositories and service names to be human
readable so the names Approved Premises and Temporary Accommodation will still
exist for now.

The naming of the API itself is slightly related. It is currently named Approved
Premises API which we imagine should also eventually be renamed to Community
Accommodation Services API.

## Decision

- New identification should use the CASX naming convention.
- CAS2 will start to identify themselves as `CAS2` in the codebase.
- CAS1 and CAS3 will continue to identify as "Approved Premises" and "Temporary
Accommodation" until there are opportunities to refactor them away
- When referring to this identifier we should consolidate on `serviceName`
  rather than `service` or `type`

## Consequences

- Using CASX as identifiers internally are more concise, easily understood by
the tech team and should be better future proofed from external changes
- There will be a period of increased complexity in the codebase where CAS1 and
CAS3 identify differently from CAS2
- We will have to remember the direction we are headed in which is hopefully
aided by this ADR
- CAS1 and CAS3 may use inconsistent identification until the migration is
complete
- Changing the expected values for X-Service-Name will require changes in the
  frontends

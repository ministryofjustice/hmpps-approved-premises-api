# 5. reuse-api-for-cas1-and-cas3

Date: 2022-10-22

## Status

Accepted

## Context

CAS1 (Approved Premises) and CAS3 (Temporary Accommodation) share a common
business domain - they both manage accommodation in various properties, match
potential residents, and manage the arrival and departure of residents. They
also both accept referrals to their services from Probation Practitioners.

Both services break down their workflow into a common pattern: Apply, Assess,
Match, Manage, and Report.

CAS1 have been working on this API for the last few months and have developed a
minimal viable property management service called mini-manage. The API includes
development tooling and has infrastructure in place to run it in a live
environment. CAS3 have a short 3 month deadline to go live with their own
property management service.

In order to increase the odds of success for CAS3 and to save time and money,
CAS3 have been exploring [1,2] what software can be reused in collaboration with
CAS1.

Both CAS teams are currently procured separately so we cannot merge to form one
API team/squad. There are longer term ambitions that this will eventually be the
case.

1. One option was to fork the API now and develop a separate set of services.
This would provide CAS3 a lot of reuse in the short term but would close the
door to the long term benefits of having a single service. We believed that it
would be highly unlikely to ever be cost effective to merge back those two
services back together in the future.
2. The second and preferred option was to attempt to work within the same API,
accept the impact to delivery in the short term in an attempt gain the longer
term benefits for MoJ. Taking this path would not block the path to forking in
future should the trade-offs change.

Our proposal for option 2[1] was taken to our joint service owner for approval
on October 13th 2022.

[1]
<https://docs.google.com/document/d/1zX28jObx2MCt57mafi-d4GE4vO-UAexLs_Ly2KQfU3U/edit?pli=1>

[2]
<https://docs.google.com/document/d/1vclqsYO0BRlMWTfp5rA0Ev-GnK82VsU9d7Si07mWlqo/edit#heading=h.4hk4dnxkopf9>

## Decision

- Both CAS teams will aim to share and build upon the same API
- Relatedly there is a frontend repository[3] that CAS3 will fork and develop
independently
- A single technical architect that's shared across both projects to maintain
  oversight and improve the chances of delivering a joined up service

[3] <https://github.com/ministryofjustice/hmpps-approved-premises-ui>

## Consequences

- both teams may have to collaborate and design parts of the service together,
  hopefully the tech leads and technical architect can do this for the most part
  to protect the rest of the teams time
- we may find that working on the same API proves too logistically difficult in
  which case CAS3 could fork away at that point
- we may find there isn't as much shared behaviour as we anticipate, in which
  case CAS3 could work away at that point
- it may slow the CAS1 team down and affect their delivery, we'll have a ways of
  working session to see if we can manage that risk
- a single API for all CAS services should:
  - make it more likely that a single team can support it in future, reducing
    time and money over the long term
  - provide precedent for the CAS2 service to follow, saving time and money
  - provide a single API to interact with the rest of the MoJ digital services
    such as NDelius, requiring single sets of integrations and single
    penetration tests
  - enable more reuse over the long term. Including substantial features such as
    the domain event system[4] which both teams could use to push data
    downstream into systems that depend on it

[4]
<https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/main/doc/architecture/decisions/0004-generate-domain-events-when-ap-data-is-changed.md>

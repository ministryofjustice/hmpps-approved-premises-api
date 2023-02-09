# 11. Control "applicant" access using Delius team caseload

Date: 2023-01-23

## Status

Accepted

## Context

The service will manage all but one of its roles internally. This ADR concerns that one
exception: the "applicant" role which is not modelled explicitly within the service, but
is derived from caseload information from Delius.

The Approved Premises service exposes personal data from NOMIS and from OASys and so
it's necessary to restrict creating and viewing "applications" to probation officers with an
appropriate caseload.


### The "applicant role"

An "applicant" needs access to more than their individual caseload, in order to create and
access applications for people managed by fellow team members.

We know that probation officers should have membership of only one team, but currently
this is not the case. At present, approx 270 staff in the North East region are members of multiple teams.

When restricting our "Applicant" role to CRNs which are within the caseload of their
team(s) we are considering the following scenarios:

1. applicant creates a new application for a given CRN 
2. applicant views existing applications, created by themselves 
3. applicant views existing applications, created by team members


## Decision

We're basing our applicant access control on the caseload(s) of the team(s) in which the
applicant has membership.

### 1. Creating an application

To achieve [1] probation-integration (PI) team are providing a new endpoint on the
[approved-premises-and-delius](https://github.com/ministryofjustice/hmpps-probation-integration-services/tree/main/projects/approved-premises-and-delius) API which they've built.

Given a GET request like:

```
curl "https://approved-premises-and-delius/teams/managingCase/{crn}?staffCode={staffCode}"

```

that endpoint will return a list of teams in which i) the given Delius staff member has membership and 
ii) which has the given CRN in its caseload as a list of "managing team codes":

```json
{  
	"teamCodes": ["N01ABC"]
}
```
We note that more than one team can have a person in their caseload so the new 
`/teams/managingCase/{crn}?staffCode={staffCode}` endpoint may return more than one team. 
Storing a list of team codes against each application (e.g. in `applications.managing_team_codes`) 
would be a reasonable solution.


### 2. Listing applications link to user's team

Search for applications with a team ID matching the membership of the logged-in user using
`applications.managing_team_codes`.

NB: the service needs to start storing "team codes" as a property on the `applications` table.

### 3. Listing applications created by user

Search for applications created by the logged-in user (`applications.created_by_user_id`). 
Actually viewing the application could be restricted to people who are currently a member 
of a team managing the CRN. This would cope with the scenario where the creator of an application 
has moved to a different team and so no longer needs access to the people in their former 
caseload.

## Consequences

### Inter-PDU applications

Once the service is extended to additional regions the service will have to handle
the common scenario where a person is referred from one region or PDU for accommodation in another.

For example: an application is made by a London team to accommodate a person currently in 
custody in London. The requested location for the accommodation is Leeds. If the person takes up a 
place in a Leeds AP then their case will be transferred to a Leeds team. 

Should the team code of the new Leeds team be added to the application's list of 
`managing_teams_codes` to grant access to the new team? 

There could be flexibility whether to remove or retain the access of the team which created 
the application, through the list of team codes in `applications.managing_team_codes`.

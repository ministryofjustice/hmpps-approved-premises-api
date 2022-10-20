# 4. Generate Domain Events when AP data is changed

Date: 2022-10-18

## Status

Approved

## Context

Other services within HMPPS depend on data which will be mastered in this new Approved
Premises service. Here are some examples of information generated in the new service which
need to be reflected in NDelius, the main legacy application within probation:

- a person's change of address
- the reason why an application for accommodation in an Approved Premises has been rejected
- the location where a person being released will be accommodated
- the reason why a person's planned stay in an Approved Premises won't be taking place
- the fact that a person failed to arrive at their accommodation at the expected time

## Decision

In order to support those needs we are adopting the HMPPS pattern of publishing Domain
Events which represent changes to the data which our service "masters". Following the
guidance from the HMPPS Technical Architect community:

- [HMPPS ADR - Domain Event Schema](https://dsdmoj.atlassian.net/wiki/spaces/NDSS/pages/3732570166/ADR+-+Domain+Event+Schema)
- [HMPPS ADR - Publishing Microservice Events](https://dsdmoj.atlassian.net/wiki/spaces/NDSS/pages/2811691200/ADR+-+Publishing+Microservice+Events)

we will implement the following process:

### 1. Our service emits a Domain Event message


A lightweight message is broadcast to a system-wide channel announcing the type of event
and providing a link to full event information (`detailUrl`) e.g.

```json
{
  "eventType": "approved-premises.person.arrived",
  "version": 1,
  "description": "A person has arrived at an Approved Premises",
  "detailUrl": "https://approved-premses/events/person-arrived/68df9f6c-3fcb-4ec6-8fcf-96551cd9b080",
  "occurredAt": "2022-12-04T10:42:43+00:00",
  "additionalInformation": {
    "applicationId": "68df9f6c-3fcb-4ec6-8fcf-96551cd9b080"
  }
  "personReference": {
    "identifiers": [
      {"type": "CRN", "value":"X08769"},
      {"type": "NOMS","value":"A1234CR"}
    ]
  }
}
```

This "pub-sub" mechanism will use AWS SNS subscriptions and SQS queues in the HMPPS-wide
`domain-events` namespace.

### 2. Delius integration obtains full details

The interested party, in this case the "Delius integration" which intends to use the
domain event in order to update Delius, follows the `detailsUrl` provided to obtain the
full details of the event from our "events" API. The representation describes who arrived
at which AP, and when, e.g:

```json
{
  "id": "364145f9-0af8-488e-9901-b4c46cd9ba37",
  "timestamp": "2022-11-30T14:53:44",
  "eventType": "approved-premises.person.arrived",
  "eventDetails": {
    "personReference": {
      "crn": "C123456",
      "noms": "A1234ZX"
    },
    "bookingId": "14c80733-4b6d-4f35-b724-66955aac320c",
    "premises": {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "name": "Hope House",
      "apCode": "NEHOPE1",
      "legacyApCode": "Q057",
      "probationArea": {
        "code": "N54",
        "name": "North East Region"
      }
    },
    "keyWorker": {
      "staffCode": "N54A999",
      "staffIdentifier": 1501234567,
      "forenames": "John",
      "surname": "Smith",
      "username": "JohnSmithNPS"
    },
    "arrivedAt": "2022-11-30T14:50:00",
    "expectedDepartureOn": "2023-02-28",
    "notes": "Arrived a day late due to rail strike. Informed in advance by COM."
  }
}
```

### 3. Delius integration updates NDelius

This information is used to make changes to Delius, e.g. updating the person's address,
creating a "contact" record within NDelius etc

## Consequences

This mechanism will enable our service to represent important changes in formats which are
meaningful to the Approved Premises and wider CAS (Community Accommodation Service).

Other services, such as Delius, can subscribe to our Domain Event messages and take the
necessary actions within their own domains and contexts.

It's correct that our Approved Premises exposes the data which other services need -- and
it's important that it draws a boundary around its own "context", so that the business of
updating dependent systems is a separate activity, facilitated by our service but owned by
others.

Leading on from this work we anticipate a further ADR describing a decision to generate a
category of internal Events which are used to support our needs for auditing, error
reporting and KPIs. These events would not be published to the global `domain-events`
namespace but consumed within the Approved Premise / CAS domain as needed.

We also believe that events should be self-contained, by which we mean that associated
information, such as staff details, should be included rather than referred to using
links. This  makes the event representations reliable and trustworthy as auditing
snapshots with sufficient context of each event.

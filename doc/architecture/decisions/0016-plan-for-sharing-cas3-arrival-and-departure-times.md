
# 16. plan-for-cas3-sharing-arrival-and-departure-times

Date: 2023-09-11

## Status

Accepted

## Context

Community Probation Practitioners (CPP) working with CAS3 are currently
responsible for updating nDelius with address changes for the PoP they are
working with. They do this manually.

As the CAS3 digital service rolls out to more regions the need to automate this
increases as we'd like to save CPPs time and increase the timeliness and quality
of the data.

CAS3 would like to send two new domain events:

1. Person arrived - this will be when a CAS3 booking is updated with an active
   record
2. Person departed - this will be when a CAS3 booking is updated with a departed
   record

The CAS API has already adopted the HMPPS pattern of publishing Domain Events
for sending data about CAS1/AP. Read more about Domain events here:

- [HMPPS ADR - Domain Event
  Schema](https://dsdmoj.atlassian.net/wiki/spaces/NDSS/pages/3732570166/ADR+-+Domain+Event+Schema)
- [HMPPS ADR - Publishing Microservice
  Events](https://dsdmoj.atlassian.net/wiki/spaces/NDSS/pages/2811691200/ADR+-+Publishing+Microservice+Events)

### The arrival and departure time problem

CAS3 have worked with nDelius and the integrations team to agree [a schema for
these
events](https://miro.com/app/board/uXjVM0r32Jo=/?moveToWidget=3458764561090547707&cot=14&share_link_id=730358528659).

The schema we've been asked to send includes:

```
"timestamp": "2022-11-30T14:53:44",
"eventType": "temporary-accommodation.person.arrived",
"arrivedAt": "2022-11-30T14:51:30",
"notes": "",
…
```

```
"timestamp": "2022-11-30T14:53:44",
"eventType": "temporary-accommodation.person.departed",
"departedAt": "2023-03-30T14:51:30",
"notes": "",
…
```

Our Homeless Prevention Team users are the ones who will be updating bookings
within the service. This user group is able to update bookings on receipt of a
daily update of arrivals and departures sent to them from their property
supplier/s, to the best of our knowledge these only include _dates_.

From our existing research[1] property suppliers either do not supply
arrival/departure _times_ or do so inconsistently:

> For departure time, PoPs can leave whenever so I doubt this info is available
> or at least not in all cases.

The research[1] did however note that it was the supplier/case worker and CPPs
responsibility to provide a time. It's important to note that this is a
different user group and they don't have access to CAS3 in order to update a
booking, though presumably they would have continued access to nDelius.

[1] <https://mojdt.slack.com/archives/C03GF35FY7P/p1693473788514249>

We therefore have to plan for times being unavailable at the point a booking is
updated as arrived or departed.

NB. Theoretically the CAS3 booking system would allow a person to arrive and
depart on the same date. They could also depart from one bedspace and arrive
into another on the same day to facilitate a transfer. Although not a common
event expressing such sequences of events with default time would result in time
clashes. No accurate timeline of arrival1, departure1 and arrival2 could be
assembled with this data.

### Options

1. Only send dates and rename the field to `arrivedOn` and `departedOn` with
   values of "YYYY-MM-DD"
1. Send dates with default/stand-in times that attempt to convey they don't
   contain a time. This approach would not support multiple arrivals and
   departures on the same day.
     - `"arrivedAt": "YYYY-MM-DD 00:00:00"`
     - `"departedAt": "YYYY-MM-DD 00:00:01"` or `"YYYY-MM-DD 23:59:59"`
1. Process change - For each property supplier in each region to start providing
   arrival and departure times for residents, making this a required part of the
   CAS3 booking form. This assumes the suppliers are able to record times for
   CAS3 properties. If they are, it would then be a big change across many
   organisations in a short time. This could result in HPTs no longer being able
   to update bookings and address changes events being blocked entirely.

For option 1 and 2 we presume that the CPP or case worker would able to sign
into nDelius and update the contact with a more accurate time, increasing the
quality of the data.

## Decision

CAS3 will send default times of midday for both events and include a note
explaining the placeholder time.

nDelius and the Integration team will set these contacts as editable such that
the CPP can update with a more precise time.

## Consequences

- By providing a time from the start we make it easier to automatically provide
  a more precise time later
- Providing a default time runs the risk of those times being misinterpreted as
  accurate. We will use the note to mitigate this

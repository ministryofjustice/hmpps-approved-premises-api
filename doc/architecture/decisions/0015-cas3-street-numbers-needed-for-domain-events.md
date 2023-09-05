# 15. cas3-street-numbers-needed-for-domain-events

Date: 2023-09-05

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

### The Street number problem

CAS3 have worked with nDelius and the integrations team to agree [a schema for
these
events](https://miro.com/app/board/uXjVM0r32Jo=/?moveToWidget=3458764561090547707&cot=14&share_link_id=730358528659).
A core part of this data is the premises address.

CAS3 premises are canonically stored within CAS3 compared to CAS1 where they're
stored elsewhere. CAS3 have been helping regions structure and seed their new
dataset from spreadsheets and into the new digital service. There are currently
698 premises.

[CAS3 stores the following address information for CAS3
premises](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/20ae09b6f3b9fe4ea7534c3396671a1290ee2511/src/main/resources/static/api.yml#L3645).

The schema we've been asked to send is this:

```
…
"premises": {
    "streetNumber": "",
    "addressLine1": "",
    "addressLine2": "", // optional
    "postcode": "",
    "town" // optional
  },
…
```

**The problem is CAS3 do not include the street number as a standalone piece of
data**. The street number or building name is included in the `addressLine1`
which is a free text field.

Eg:

```
"addressLine1": "Flat 1, 8 Street Road",
"addressLine2": "Walton",
"postcode": "L9 XXX",
"town": "Liverpool",
"region: "North West"
```

CAS3 have been asking users for `addressLine1` following [this GOV.UK address
pattern](https://design-system.service.gov.uk/patterns/addresses/) on the basis
that it's a tested way of asking for this information. It also marries up with
the existing schema for CAS1/AP premises which don't handle the street number.
Presumably this is solved in their domain events by referencing the
[APCode](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/20ae09b6f3b9fe4ea7534c3396671a1290ee2511/src/main/resources/static/api.yml#L3748C16-L3748C16).

### Team constraints

CAS3 have recently are now running with a reduced team of 2 developers and are
exiting at the end of 2023. Our core goals are:

1. Make the service work for all regions
1. Enable users to download MI report data
1. Respond to urgent user feedback and bugs
1. Pass a private beta service assessment
1. Complete national rollout
1. Handover to the new team

The amount of new work CAS3 can pick up is therefore limited and needs to be
carefully considered.

### The options

1. Send the domain events with the address data we have
    - This is easy to do as we have the data and the framework for sending
      domain events
    - It doesn't provide a `streetNumber` which may make the rest of the data
      unusable on the nDelius side
1. Use the Ordnance survey API when creating/receiving a domain event
    - This would be the quickest to implement, leaving the premises forms, model
      and the data seeding processes as they were
    - We have spiked how effective this API can be and with 300 real CAS3
      addresses but only 75% returned a street number or a building name. It is
      therefore not reliable enough to automate with the data we have and some
      manual intervention will be needed
1. Restructure the CAS3 premises data to have `addressLine1` and `streetNumber`
   as two separate fields:
    1. Figure out what we expect users to provide within the service. Do they
       provide the street number in both the `streetNumber` and `addressLine1`
       field? What if the `streetNumber` is in fact a streetName? What should
       they do in instances of "Flat 1, 2 Brick Lane"? The right way to do this
       is likely an address finder where they search and select an address and
       we fill in the fields in the background. This would however break our
       property seeding processes which expect raw text inputs for each field.
    1. Add a new `streetNumber` field as an optional hidden field
    1. Feature flag new and edit property features to prevent new data coming in
    1. Run the data migration task
        - use the Ordnance survey to automate 75% of changes and manually work
          through the rest to prepare a migration script
        - we can't add real address data into the public repo so would need to
          use [the remote seed
          pattern](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/main/doc/how-to/run_migration_job_remotely.md)
          and support a new job
    1. Remove new/edit premises feature flag and make the new street number a
       required field

Option 1 might not be compatible, option 2 isn't reliable enough and option 3
will require a significant effort than we may not have time to complete.

## Decision

CAS3 will start by sending domain events with the existing address data (option
1):

```
…
"premises": {
    "addressLine1": "",
    "addressLine2": "", // optional
    "postcode": "",
    "town": "" // optional,
    "region": ""
  },
…
```

## Consequences

- Partial arrival and departure domain events can be sent by CAS3 soon, long
  before National roll out is completed by the end of 2023
- The missing street number may cause data integrity problems within nDelius
- The CAS3 team can focus on their existing goals for 2023
- The new CAS3 team can factor the sweeping address change (option 3) into their
  roadmap for 2024 once the need for supporting the data seeding is removed

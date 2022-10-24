# 6. rooms-and-beds-for-both-cas-services

Date: 2022-10-24

## Status

Accepted

## Context

The CAS1 team have a longer term vision of the data layer needed for their
property management service[1]. At the time of writing this part of the service
is partially complete and called "mini-manage". It includes Premises but not
Rooms or Beds.

CAS1 have introduced a temporary bed concept by allowing users to store a
`bed_count` on Premises directly. It was imagined that this would eventually
change over.

CAS3 are looking to extend mini-manage now in way that works for CAS3 but is
also compatible for CAS1. Both teams built a shared understanding going into
this work:

- Both CAS1 and CAS3 need to represent rooms in some fashion and associate them
  with bookings in future
- CAS1 will have rooms with one or multiple beds
- CAS3 will have one bed per room, it is highly unlikely but still possible
  that in future this might change
- Physical spaces such as Premises and Rooms look to have different
  characteristics for CAS1 and CAS3. For example, CAS3 needs to specify whether
  a room has a shared kitchen but CAS1 may not, however CAS1 might need to know
  if it has an en suite.

[1] <https://dbdiagram.io/d/6200fa0285022f4ee54d7569>

## Decision

- We will attempt to build and share the core room and bed resources
- We will separate out the storage of unique characteristics from
  rooms and beds in a way that supports future flexibility for both services
- We imagine[2] we can create a new Characteristics table where records can be
  created to represent boolean attributes. These could be associated with
  premises or rooms via two further join tables:

         ,---------------------------------------.
         |characteristics                        |
         |---------------------------------------|
         |*id: uuid                              |
         |--                                     |
         |*name: string                          |
         |*service_scope: string <<default: "*">>|
         |*model_scope: string <<default: "*">>  |
         `---------------------------------------'
                 |                |
                 |                |----------`
                 |                          .|.
                 |           ,---------------------------.
                 |           |premises_characteristics   |
                 |           |---------------------------|
                 |           |*premise_id: uuid          |
                 |           |*characteristic_id: uuid   |
                 |           `---------------------------'
                 |                           .|.
                 |                            |
                 |                            |
                .|.                  ,---------------------------.
      ,---------------------------.  |premises                   |
      |room_characteristics       |  |---------------------------|
      |---------------------------|  |*id: uuid                  |
      |*room_id: uuid             |  |--                         |
      |*characteristic_id: uuid   |  |address: string            |
      `---------------------------'  |postcode: string           |
             .|.                     `---------------------------'
              |-------`                      |
                     |             `----------
                     |             .|.
               ,---------------------------.
               |rooms                      |
               |---------------------------|
               |*id: uuid                  |
               |--                         |
               `---------------------------'
                              |
                             .|.
               ,---------------------------.
               |Bed                        |
               |---------------------------|
               |*id: uuid                  |
               |--                         |
               `---------------------------'

[2] [PlantUML](https://www.plantuml.com/plantuml/uml/fP5VQiCm3CRVVGhJnu8l44eftGN6Bb0yMsl5OYj8og0qlVkIT0dDC2omn-Zxau-V3wAUjKsraK_CZMUVR8qPzDg09TOB6GDPKQ_svTAhGO5H58Ez6Mkt62kBMU4CR7SxtohbDhq3x_UzwjnWoJv1PKKPytdu5k0l372IGCqizE487xwjjRARxMQCRnD5gF_otQTvRqIfD97x--bfvt0B2zz2dVjU6H8Ab2gRX_IFrhNqV8mC8a_y6X8D56SYIm3hUkV84UBQXFa5ekkSerk1UjbOncbCuTNkv5pNZSrcJfKSFwAPNEElkiUciNWw726Q0-JOfleR)

## Consequences

- For CAS3, the new 'create a Room' endpoint will also need to create a bed.
  The implementation for this will need to be considered to allow CAS1 to do
  this across two different transactions
- Splitting out characteristics into a new table, potentially with join tables,
  will require more set up work but this cost should be reasonable
- Admin may in future need to modify the list of characteristics, having these
  stored in a table rather than hardcoded should enable that
- CAS1 will have to maintain their temporary `Premises.bed_count` mechanism for
  beds alongside this extension work until such time as it can be refactored
- We may learn of a Characteristic in future that isn't a boolean that may
  require us to revisit this plan to accommodate multiple values
- Neither service currently anticipate the need to add characteristics to an
  individual bed

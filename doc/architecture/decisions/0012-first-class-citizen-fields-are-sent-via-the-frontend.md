# 12. First class citizen fields are sent via the frontend

Date: 2023-02-28

## Status

Accepted

## Context

In [ADR 008](./0008-add-first-class-citizen-fields-to-applications.md), we made the decision to 
use [JsonLogic](https://jsonlogic.com) to fetch important (first-class-citizen) fields
from an application once an application had been submitted. This is becoming increasingly
unweildy, and causes problems with validating against schemas as well as ensuring that the UI and
the API remain in sync

## Decision

Rather than making it the responsibility of the backend to fetch first class citizen fields from 
an Application, we will switch to making this the responsibility of the front-end. For example, 
if we want to fetch, the sentence type (as referenced in [ADR 008](./0008-add-first-class-citizen-fields-to-applications.md))
from an application, rather than using a JSON logic rule, we will add an additional required
`sentenceType` field to be POSTed to the `/applications/{applicationId}/submission` endpoint.

## Consequences

This means that if the structure of the application data is changed in the frontend, it only needs 
to be changed in one place, rather than having to update JSONlogic rules in the database before 
deploying a new version of the frontend. Any issues with fetching this data will be picked up by 
integration tests in the UI, reducing the risk of any errors making it through to production.

As the UI types are constructed from the OpenAPI spec, any change in the data that the API
expects (for example, a new field) will be reflected in the change to types in the UI, 
signalling that the UI will need to be changed to accommodate this new structure. It is hoped
that communication between both frontend and backend developers will be such that the team
can work together to roll this change out in a manner that keeps disruption to users to a minimum.
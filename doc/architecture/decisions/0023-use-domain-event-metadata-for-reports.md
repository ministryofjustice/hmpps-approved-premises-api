# 23. Use Domain Event Metadata for Reports

Date: 2024-05-20

## Status

Accepted

## Context

When adding or enhancing reports, we typically require access to information captured via the UI processes that is only stored in the database as part of an entities 'data' or 'document' JSON.

The schemas for such JSON is managed by the UI and its structure liable to change at any point. The API provides a convenient way for the UI to store this data across user sessions, but other than that it should be treated as a black-box.

Whilst there is general agreement we should not be using this JSON in reports, the current solution of promoting required fields into first class properties is time-consuming and is leading to several additional columns being added to the data model to capture data that may only ever be used in reporting.

Testing has also shown that accessing data from the JSON (particularly in the case of applications) can significantly slow down reporting queries (for example, accessing two fields from applications.data added an additional 3 seconds to report execution time, without adding any indexing to the JSON).

## Decision

The Domain Event model will be updated to include a Map<String,String> of metadata, with the intention of using this data when generating reports instead of directly accessing data/document json. APIs that create Domain Events will be updated to capture any fields required in metadata that aren't already provided in first class fields manner.

This is a good fit for capturing report-specific data because domain events already provide a 'snapshot' of the application's state during key reporting events (e.g. application submitted, application withdrawn).

Furthermore, in places where reports access domain event data json (something which is less of an issue because the API controls the domain event schema), it would be preferred if the equivalent data was instead captured in the metadata, simplifying reporting definition and providing a small performance improvement.

## Consequences

This change will ensure that the API code no longer needs to understand the structure of json in the data/document columns, as was the original intention for the data/document JSON.

The change will also remove the brittle coupling between reports and the JSON structures, which are liable to change in a breaking manner. Trying to support multiple schemas for the data/document JSON will become increasingly unsustainable.
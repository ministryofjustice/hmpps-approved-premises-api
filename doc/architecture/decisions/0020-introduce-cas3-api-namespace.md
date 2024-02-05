# 18. Introduce CAS3 API namespace

Date: 2024-02-02

## Status

Accepted

## Context

At present there is a single set of API endpoints and controllers. For 
example the `/applications` endpoint is handled by the  `ApplicationsController.applicationsGet()` 
function which then branches 3 
ways according to which of the 3 CAS services is calling the endpoint. (The 
identity of the calling service is communicated by the consumer via the
`X-Service-Name` header.)

Recently we have got requirement to get new referral reports with different content
compare to existing CAS1 API, so instead of changing existing API to add additional functionality
it will make sense to create new API which then internally trigger the need for new CAS3 namespace
which We believe that this will make the codebase clearer and more maintainable.

## Decision

We will implement a CAS3-specific API namespace and controllers as per current structure 
We anticipate that this will make development and maintenance faster by 
clearly separating CAS3 functionality from that of the other services.

We start with developing new API in new CAS3 namespace which enable us to refactor
existing CAS3 API into new CAS3 namespace


## Consequences

Changes will need to be made to the UI clients, e.g. to make HTTP requests
to `https://ap.example.com/cas3/**/`
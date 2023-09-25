# 18. Introduce CAS2 API namespace

Date: 2023-09-13

## Status

Accepted

## Context

At present there is a single set of API endpoints and controllers. For 
example the `/applications` endpoint is handled by the  `ApplicationsController.applicationsGet()` 
function which then branches 3 
ways according to which of the 3 CAS services is calling the endpoint. (The 
identity of the calling service is communicated by the consumer via the
`X-Service-Name` header.)

In "ADR 15 Handle Nomis users without inheritance in CAS2" we describe the 
decision to extract CAS2-specific "User" and "Application" code out to 
stand-alone classes outside of the inheritance hierarchy. We believe that 
this will make the codebase clearer and more maintainable.

Before this work begins we see an opportunity to bring further 
benefits of clarity and explicitness by separating CAS2 endpoints into their 
own namespace and implementing CAS2 controllers. For example, we'd have 2 
applications endpoints, a new one for CAS2 and the existing one handling 
CAS1 and CAS3 rather than all three services: 

```
/applications -> ApplicationsController (CAS1 and CAS3)
/cas2/applications -> CAS2ApplicationsController
```
## Decision

We will implement a CAS2-specific API namespace and controllers as outlined. 
We anticipate that this will make development and maintenance faster by 
clearly separating CAS2 functionality from that of the other services. 

Before starting the work to support Nomis users we'll extract the existing 
behaviour to CAS2 controllers and endpoints.

## Consequences

Changes will need to be made to the UI clients, e.g. to make HTTP requests
to `https://ap.example.com/cas2/applications` rather than `https://ap.
example.com/applications`.

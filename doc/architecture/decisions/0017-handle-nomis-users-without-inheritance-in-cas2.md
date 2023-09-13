# 17. Handle Nomis users without inheritance in CAS2

Date: 2023-09-13

## Status

Accepted

## Context

The users of the CAS2 service need to authenticate using Nomis credentials as
they are Prison Offender Managers (POMs). This is in contrast to CAS1 and CAS3,
whose users log in with their Delius credentials.

At present the CAS API's concept of a `User` is hard-wired to Delius. Its
properties include:

- `deliusUsername`
- `deliusStaffCode`
- `deliusStaffIdentifier`
- `probationRegion`


Some of these have equivalents in the `NomisUserDetail` representation returned 
from the [Nomis User Roles API](https://github.com/ministryofjustice/nomis-user-roles-api)'s 
[`GET /users/{username}` endpoint](http://nomis-user-dev.aks-dev-1.studio-hosting.service.justice.gov.uk/swagger-ui/index.html#/user-resource/getUserDetails) :

```json
{
  "username": "testuser1",
  "staffId": 324323,
  "firstName": "John",
  "lastName": "Smith",
  "activeCaseloadId": "BXI",
  "accountStatus": "OPEN",
  "accountType": "GENERAL",
  "primaryEmail": "test@test.com",
  "dpsRoleCodes": ["string"],
  "accountNonLocked": true,
  "credentialsNonExpired": true,
  "enabled": true,
  "admin": true,
  "active": true,
  "staffStatus": "ACTIVE"
}
```

and others are very much specific to Delius.

The CAS API makes use of [JPA Inheritance](https://thorben-janssen.com/complete-guide-inheritance-strategies-jpa-hibernate/) 
to handle CAS1/2/3 specific flavours of "Application". An `Application` belongs 
to a `User` (a Delius user).

In handling Nomis users we have two polymorphism/inheritance challenges to 
either tackle or sidestep:

1. Allow `Application`s to be associated with either Delius or Nomis users.
2. Allow authorisation code to handle both Nomis and Delius users.

NB: some classes such as `UserAccessService` would need to handle 
both the polymorphic `Application` and a new polymorphic `User`.

We have considered two approaches:

### 1. **Introduce more JPA inheritance**

In this option we would associate users with an abstract class 
such as `AuthenticatedUser`s rather than Delius `User`s.

This would require changes to existing classes and concepts to allow them 
to cope with:

- `Application`s associated with either Delius or Nomis users

- `UserDetail` representations coming from either Delius or Nomis

- Authorisation/access checks (LAO) coming from either Delius or Nomis 
  in an `UserAccessService` class or similar

#### Advantages
- retains existing pattern for handling service-specific needs

#### Disadvantages
- increases complexity and size of classes and functions which are already 
  over-long

- requires extensive refactoring of code used by existing live CAS 
  services (1 and 3). Despite good test coverage this is risky, due 
  to the need to migrate existing application and user records to the new formats

### 2. **Introduce CAS2 specific classes**

In this (preferred) option we would handle Nomis users and their applications with CAS2-specific classes, e.g.

- `Cas2ApplicationEntity`
- `Cas2JsonSchemaService`
- `NomisUserDetail`
- `NomisUserEntity`
- `NomisUserService`
- `NomisUserAccessService`

#### Advantages

- is a low tech approach with does not require sophisticated use of JPA 
  inheritance. It's therefore easier for a broad range of developers to 
  understand and maintain

- results in reduction in size and complexity of large classes and 
  functions (they will be left dealing "only" with CAS1 and CAS3). In 
  this way it drives a desirable refactoring of the codebase to leave 
  it in a clearer state with the CAS2 service-specific code largely 
  decoupled into discrete classes.

#### Disadvantages

- there will be more classes and repeated code
- by moving `CAS2Application` out of the "JOINED" polymorphic inheritance 
  strategy it will be less "easy" to query across all flavours of 
  application e.g. to produce a list of all applications across all 
  3 CAS services

## Decision

When we consider the current codebase e.g.

- [`ApplicationsController.put()`](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/077d50ab85c78d12a9e552b36e4a86af1a38cc27/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/controller/ApplicationsController.kt#L143-L168)
- [`ApplicationsTransformer.transforJpaToApi()`](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/a4fa1437188e9e5bf212fa9b150d29b6a1157216/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/transformer/ApplicationsTransformer.kt#L37-L111)

we often see long functions composed of 3 service-specific case clauses. 
In other places we see that these unwieldy functions have been decomposed 
into smaller service-specific functions e.g. in the case of the 
[`ApplicationsController.post()`](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/077d50ab85c78d12a9e552b36e4a86af1a38cc27/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/controller/ApplicationsController.kt#L112-L121) 
which hands off the 3 service-specific cases to 3 functions within the
[ApplicationService](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/8634e2e5494af173707dcf45df9c4fb12514d089/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/service/ApplicationService.kt):

- `ApplicationService.submitApprovedPremisesApplication()`
- `ApplicationService.submitCas2Application()`
- `ApplicationService.submitTemporaryAccommodationApplication()`

Our decision is to continue down this path of decomposition by 
using CAS2 service-specific classes to implement the particular needs 
of our service. This means we will:

### 1. Handle Nomis users separately

In the medium term we believe that it makes sense to move towards 
an implementation-agnostic concept such as `AuthenticatedUser`. 
This is because we foresee the need for services to offer authentication 
by more than one identity provider, e.g. that CAS2 might need to grant 
access to Delius users as well as Nomis users. We know that CAS2 will also
provide access to external NACRO users, though these will not be associated 
with applications. 

`AuthenticatedUser` could be an abstract class, implemented by classes 
which are tied to their particular authentication sources e.g.:

```
 AuthenticatedUser (abstract)
    │
    ├── DeliusUser 
    │
    ├── NomisUser
    │
    └── NacroUser
    
```

But to begin with we'll implement the `NomisUser` class independently 
for use by CAS2. At present each service allows authentication from a 
single auth source (either Delius or Nomis) so we don't need that 
general purpose `AuthenticatedUser` immediately. We'll implement 
classes such as:

- `NomisUserDetail`
- `NomisUserEntity`
- `NomisUserService`
- `NomisUserAccessService`

### 2. Handle CAS2 Applications separately

Retaining the `Cas2Application`'s position as a "joined" subclass of 
the abstract `Application` would increase complexity without clear 
benefit.

Rather, we will create a stand-alone `Cas2Application` entity which 
belongs to a `NomisUser`. We'll create CAS2-specific classes as needed, e.g.:

- `Cas2ApplicationEntity`
- `Cas2JsonSchemaService`
- `Cas2ApplicationsTransformer`
- `Cas2ApplicationService`

## Consequences

- When we come (soon) to grant access to external NACRO users we will consider
  whether to implement the `AuthenticatedUser` class described above. NACRO
  users will have read-only access to existing `CAS2Application`s. i.e. no 
  association with applications is required, "only" authentication and 
  authorisation. 

# Approved Premises API (now Community Accommodation)

This is the shared backend for the Community Accommodation services, currently:

- [Approved Premises
  (CAS1)](https://github.com/ministryofjustice/hmpps-approved-premises-ui)
- [Temporary Accommodation
  (CAS3)](https://github.com/ministryofjustice/hmpps-temporary-accommodation-ui)

## Prerequisites

TBC

## Setup

When running the application for the first time, run the following command:

```bash
script/setup # TODO - this script is currently a stub
```

If you're coming back to the application after a certain amount of time, you can
run:

```bash
script/bootstrap # TODO - this script is currently a stub
```

## Running the application

To run the server, from the root directory, run:

```bash
script/server
```

This runs the project as a Spring Boot application on `localhost:8080`

### Running/Debugging from IntelliJ

To run from IntelliJ, first start the database:

```bash
script/development_database
```

Then in the "Gradle" panel (`View->Tool Windows->Gradle` if not visible), expand
`approved-premises-api`, `Tasks`, `application` and right click on
`bootRunLocal` and select either Run or Debug.

## Making requests to the application

Most endpoints require a JWT from HMPPS Auth - an instance of this runs in
Docker locally (started alongside the database) on port 9091.  You can get a JWT
by running:

```
script/get_client_credentials_jwt
```

The `access_token` value in the output is the JWT.  These are valid for 20
minutes.

This value is then included in the Authorization header on requests to the API,
as a bearer token, e.g.

```
Authorization: Bearer {the JWT}
```

## Running the tests

To run linting and tests, from the root directory, run:

```bash
script/test
```

### Running/Debugging from IntelliJ

To run from IntelliJ, first start the database:

```bash
script/test_database
```

Then either:

- Run or Debug the `verification`, `test` Task from the "Gradle" panel
- Open an individual test class and click the green play icon in the gutter at
  the class level or on a specific test method

## OpenAPI documentation

The API which is offered to front-end UI apps is documented using
Swagger/OpenAPI. The initial contract covers the migration of certain
bed-management functions from Delius into the new service.

This is available in development at
[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

## Manage infrastructure & view logs

This application is hosted on the MoJ Cloud Platform. For further details head
over to [our infrastructure
documentation](/doc/how-to/manage-infrastructure.md).

## Release process

Our release process aligns with the other CAS teams and as such [lives in
Confluence](https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4247847062/Release+process).
The steps are also available in the
[PULL_REQUEST_TEMPLATE](/.github/PULL_REQUEST_TEMPLATE.md#release-checklist).

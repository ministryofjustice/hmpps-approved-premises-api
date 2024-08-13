# Approved Premises API (now Community Accommodation)

This is the shared backend for the Community Accommodation services, currently:

- [Approved Premises (CAS1)](https://github.com/ministryofjustice/hmpps-approved-premises-ui)
- [Temporary Accommodation (CAS3)](https://github.com/ministryofjustice/hmpps-temporary-accommodation-ui)

## Prerequisites

TBC

## Setup

When running the application for the first time, run the following command:

```bash
script/setup # TODO - this script is currently a stub
```

If you're coming back to the application after a certain amount of time, you can run:

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

Then in the "Gradle" panel (`View->Tool Windows->Gradle` if not visible), expand `approved-premises-api`, `Tasks`, 
`application` and right click on `bootRunLocal` and select either Run or Debug.

Note that setting a breakpoint will block the handling of any subsequent web requests (don't use breakpoints to test what happens when subsequent request are received that are in contention).

### Linting

There is a linting check stage in the pipeline, so to ensure this passes run the linting check locally before pushing:

```bash
./gradlew ktlintCheck
```

And (if required) run the following to update the code automatically to follow the coding style:

```bash
./gradlew ktlintFormat
```

## Making requests to the application

Most endpoints require a JWT from HMPPS Auth - an instance of this runs in Docker locally (started alongside the database) 
on port 9091.  You can get a JWT by running:

```
script/get_client_credentials_jwt
```

The `access_token` value in the output is the JWT.  These are valid for 20 minutes.

This value is then included in the Authorization header on requests to the API, as a bearer token, e.g.

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
 - Open an individual test class and click the green play icon in the gutter at the class level or on a specific test method

## OpenAPI documentation

The API which is offered to front-end UI apps is documented using Swagger/OpenAPI.
The initial contract covers the migration of certain bed-management functions from Delius into the new service.

This is available in development at [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

## Infrastructure

The service is deployed to the [MoJ Cloud Platform](https://user-guide.cloud-platform.service.justice.gov.uk). This is 
managed by Kubernetes and Helm Charts which reside within this repo at [`./helm_deploy`](./helm_deploy/approved-premises-api/).


To get set up with Kubernetes and configure your system so that the `kubectl` command authenticates, see this 
[[MoJ guide to generating a 'kube' config](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/kubectl-config.html#generating-a-kubeconfig-file)].

You should then be able to run `kubectl` commands, e.g. to list the 'pods' in a given 'namespace':

```bash
$ kubectl -n hmpps-community-accommodation-dev get pods

NAME                                                READY   STATUS    RESTARTS   AGE
hmpps-approved-premises-api-655968557b-5qlbc        1/1     Running   0          83m
hmpps-approved-premises-api-655968557b-bp7v9        1/1     Running   0          83m
hmpps-approved-premises-ui-5cf65777bf-bqtlc         1/1     Running   0          74m
hmpps-approved-premises-ui-5cf65777bf-n4j89         1/1     Running   0          74m
hmpps-temporary-accommodation-ui-67b49b8dcd-p85pt   1/1     Running   0          125m
hmpps-temporary-accommodation-ui-67b49b8dcd-tgjd5   1/1     Running   0          125m
```
**NB**: this [`kubectl` cheatsheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/) is a good reference to 
other commands you may need.

## Environments

[Details of the different environments and their roles can be found in
Confluence](https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4330226204/Environments).

## Release process

Our release process aligns with the other CAS teams and as such [lives in Confluence](https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4247847062/Release+process). The steps are also available in the [PULL_REQUEST_TEMPLATE](/.github/PULL_REQUEST_TEMPLATE.md#release-checklist).
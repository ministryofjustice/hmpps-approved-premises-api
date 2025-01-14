# Approved Premises API (now Community Accommodation)

This is the shared backend for the Community Accommodation User Interfaces

- [CAS1 - Approved Premises](https://github.com/ministryofjustice/hmpps-approved-premises-ui)
- [CAS2 - Short-Term Accommodation](https://github.com/ministryofjustice/hmpps-community-accommodation-tier-2-ui)
- [CAS3 - Temporary Accommodation ](https://github.com/ministryofjustice/hmpps-temporary-accommodation-ui)

## Running Tests

To run tests, you'll need to start a few docker dependencies using:

```shell
/script/test_database
```

You can then run tests in intellij or via the command line e.g.

```shell
/script/test
```

## Running the Application

To run the application locally, use [ap-tools README](https://github.com/ministryofjustice/hmpps-approved-premises-tools/blob/main/README.md) as this will manage starting all the required dependencies

## Making requests to the application

Most endpoints require a JWT from HMPPS Auth - an instance of this runs in Docker locally on port 9091 (via ap-tools). You can get a JWT by running:

```
script/get_client_credentials_jwt
```

The `access_token` value in the output is the JWT.  These are valid for 20 minutes.

This value is then included in the Authorization header on requests to the API, as a bearer token, e.g.

```
Authorization: Bearer {the JWT}
```

## Coding Notes

### Documentation

* Architectural decisions are documented in [doc/architecture/decisions](doc/architecture/decisions)
* Miscellaneous guidance is provided in [doc/how-to](doc/how-to)

### Linting / Static Analysis

There is are linting and static analysis checks in the build pipeline. You can lint and check for issues before pushing by running

```bash
./gradlew ktlintFormat && ./gradlew detekt
```

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

### Environments

[Details of the different environments and their roles can be found in
Confluence](https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4330226204/Environments).

## Release process

Our release process aligns with the other CAS teams and as such [lives in Confluence](https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4247847062/Release+process). The steps are also available in the [PULL_REQUEST_TEMPLATE](/.github/PULL_REQUEST_TEMPLATE.md#release-checklist).


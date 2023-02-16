# Manage Infrastructure

The service is hosted on the [MoJ Cloud
Platform](https://user-guide.cloud-platform.service.justice.gov.uk/#getting-started).
It's a platform where we can host our applications and interact with them
through Kubernetes. This requires our applications to all be Dockerised.

Each repository will have a `helm_deploy` directory that specifies configuration
for this service in each of the environments.

When this app deploys via merging to `main`, CircleCI will automatically
propagate those changes to the cluster. This happens through the
`hmpps/deploy_env` job that is provided by the [MoJ CircleCI
Orb](https://github.com/ministryofjustice/hmpps-circleci-orb).

Each environment will correspond to a Cloud Platform 'namespace'. The namespace
is an isolated cluster. We can use the [Cloud Platform Environments
repository](https://github.com/ministryofjustice/cloud-platform-environments/tree/main/namespaces/live.cloud-platform.service.justice.gov.uk)
to define our backing services, certificates etc.

## Prerequisites

* Be a part of the Ministry of Justice GitHub organisation. This should be done
  as part of your your team onboarding process.
* Be a part of the `hmpps-community-accommodation` team. This should be done as
  part of your team onboarding process.
* [Follow the Cloud Platform guidance to connect to the Kubernetes
  cluster](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/kubectl-config.html#connecting-to-the-cloud-platform-39-s-kubernetes-cluster)

## Kubernetes cheat sheet

To use Kubernetes to interact with the cluster there's [a cheat
sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/). Keep reading
for tasks we commonly use.

## Common Kubernetes tasks

### View the application logs of a pod

#### Dev

Find the name of the pod you'd like to get the logs for:

```bash
kubectl --namespace hmpps-community-accommodation-<env> get pods
```

Follow the logs:

```bash
kubectl -n hmpps-community-accommodation-<env> logs --follow <pod name> --all-containers
```

#### Prod

The API logs available directly from containers are minimal. We believe this is
by design to limit the information being surfaced for security purposes.

##### App Insights

Stack traces for failures can be found using Microsoft Application Insights.

1. Complete your security clearance
1. Receive your MoJ laptop
1. Get an account following [this
   guidance](https://github.com/ministryofjustice/dso-infra-azure-ad/tree/main/users-groups#self-service-workflow)
   for prod
1. [Visit App
   Insights](https://portal.azure.com/#@nomsdigitechoutlook.onmicrosoft.com/resource/subscriptions/a5ddf257-3b21-4ba9-a28c-ab30f751b383/resourceGroups/nomisapi-prod-rg/providers/Microsoft.Insights/components/nomisapi-prod/failures)
1. Use the filter to find events of interest. These events will probably be
   requests that the API sends downstream such as `/secure/offenders/crn/`

##### Sentry

We also use
[Sentry](https://ministryofjustice.sentry.io/projects/hmpps-approved-premises-api-k1/?project=4503931792392192)
to track and alert on errors and performance of the API itself.

### View/change the value of an environment variable

Environment variables are themselves [defined with
Helm](https://github.com/ministryofjustice/hmpps-temporary-accommodation-ui/blob/main/helm_deploy/hmpps-temporary-accommodation-ui/values.yaml#L50).

For environment variables that aren't secrets we can set these values in our
[Helm
charts](https://github.com/ministryofjustice/hmpps-temporary-accommodation-ui/blob/main/helm_deploy/values-prod.yaml#L12).

For environment variables that contain secrets we can't set these in GitHub so
we have to set the values by hand.

First find the secret set you'd like to view/change:

```bash
kubectl --namespace hmpps-community-accommodation-<env> get secrets
```

Add the secret name and view or make the change:

```bash
kubectl --namespace hmpps-community-accommodation-<env> edit secret <secret set name>
```

Consider a [rolling restart](#rolling-restart) to apply this change.

### Rolling restart

Restart an individual service without downtime. Each service will have multiple
containers running. This process will attempt to start a new replica set
alongside the existing set that's currently serving real requests. If
the new set is healthy, Kubernetes will gracefully replace the existing set and
then remove the old. Useful as one way to refresh environment
variables.

First find the service name you'd like to restart:

```bash
kubectl --namespace hmpps-community-accommodation-<env> get services
```

Start the restart:

```bash
kubectl --namespace hmpps-community-accommodation-<env> rollout restart deployment <service name>
```

You can observe the progress if you like:

```bash
watch kubectl --namespace hmpps-community-accommodation-<env> get pods
```

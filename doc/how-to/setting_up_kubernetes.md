# Setting up Kubernetes

In order to connect to the api you must have Kubernetes set up correctly

- Follow the instructions on [this page](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/kubectl-config.html)
- Once complete, set a default namespace for `kubectl get pod` to work. This will be the default location where you will be retrieving pods.

```
// One of the below
kubectl config set-context --current --namespace=hmpps-community-accommodation-dev
kubectl config set-context --current --namespace=hmpps-community-accommodation-test
kubectl config set-context --current --namespace=hmpps-community-accommodation-preprod
kubectl config set-context --current --namespace=hmpps-community-accommodation-prod
```
- Now you can run `kubectl get pod` and it will retrieve pods within that namespace

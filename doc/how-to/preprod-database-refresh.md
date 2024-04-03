# Database refresh - Prod to Preprod

The service has a [cronjob](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/main/helm_deploy/hmpps-approved-premises-api/templates/cronjob-prod-to-preprod-refresh.yaml) that runs every Monday at 8am
to refresh the data in preprod using data from production.

If setting this up for the first time use **extreme caution** - this job connects to production databases.

**__Ensure that all environment variables are named and setup correctly.__**

## Prerequisite

For this job to work the preprod database credentials need to be available production namespace. 
This is achieved by having terraform export the preprod credentials, which is an output of the 
Terraform rds module, to a secret in the production namespace. See this example:

https://github.com/ministryofjustice/cloud-platform-environments/blob/f30756a1fec8ad4857cc43b211fc4c08010890ae/namespaces/live.cloud-platform.service.justice.gov.uk/hmpps-community-accommodation-preprod/resources/rds.tf#L62C4-L62C4

## Overview

The refresh job performs a `pg_dump` using the existing production credentials, already setup in 
the production namespace of the application. The job then dumps the existing users from preprod
(to ensure that preprod users don't lose access). It then uses the preprod credentials 
(see prerequisite) to carry out a `pg_restore` of the production database, and then adds the
preprod users back to the preprod database.

### Known Issues

#### Pre-Prod users not being re-inserted if column mismatch

If there are additional columns on the users, user_qualification_assignments or user_role_assignments tables in pre-prod (when compared to prod), the restore of pre-prod specific users/permissions will fail

## Run an adhoc database refresh

If you need to run the database refresh outside the schedule, you can run the following
command:

```sh
kubectl create job --from=cronjob/db-refresh-job db-refresh-job-adhoc --namespace hmpps-community-accommodation-prod
```

The job creates a pod that runs to completion. You can review the command output by 
using `kubectl` to show pod logs, e.g.

```sh
kubectl -n hmpps-community-accommodation-prod logs db-refresh-job-adhoc-{id} -f
```
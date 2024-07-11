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

#### Failure if tables/views exist pre-prod but not in prod

We've observed errors occurring when tables with FKs or views exist in pre-prod that aren't in prod.

The backup/restore errors because it's blocked when trying to remove a table that is referred to by an object not in prod

In this case we've found that removing the 'deltas' from pre-prod and re-running the job can resolve the issue, although pre-prod specific users may be lost, see below for a remedy

#### Pre-Prod users not being re-inserted if column mismatch on user tables

If there are additional columns on the users, user_qualification_assignments or user_role_assignments tables in pre-prod (when compared to prod), the restore of pre-prod specific users/permissions will fail

In this case seed jobs can be used to create the required users (see https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4889608208/Seed+CAS1+Pre-Prod+Team+Users and https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4461134218/Seed+user+roles)

#### Refresh removes columns that are in pre-prod and not prod

If pre-prod is on a newer version of code (with new migrations), you may see errors after running the job.

You can force applying the latest migrations by restarting the API pods in pre-prod:

```kubectl rollout restart deployment hmpps-approved-premises-api -n hmpps-community-accommodation-preprod```

## Run an adhoc database refresh

Note! Due to the Known Issues mentioned above, it's strongly advised that you ensure the pre-prod database is on the same version of migrations as the prod database before running the job. You can do this using the following script in each environment:

```select script, installed_on from flyway_schema_history  order by installed_on desc limit 10;```

If you need to run the database refresh outside the schedule, you can run the following command:

```sh
kubectl create job --from=cronjob/db-refresh-job db-refresh-job-adhoc --namespace hmpps-community-accommodation-prod
```

The job creates a pod that runs to completion. You can review the command output by using `kubectl` to show pod logs

First find out the container name:

```sh
kubectl -n hmpps-community-accommodation-prod get pods | grep db-refresh
```

Then check the logs using the pod name:

```sh
kubectl -n hmpps-community-accommodation-prod logs -f {pod-name}
```

If the container is in an error state and you want to re-run, you may have to stop the job manually first:

```sh
kubectl -n hmpps-community-accommodation-prod delete job db-refresh-job-adhoc
```
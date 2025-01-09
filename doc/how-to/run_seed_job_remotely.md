# How to run a seed job remotely

## Run book

To process a seed file against a non-local environment:

- Ensure nobody is deploying a change (or is going to deploy a change shortly.)
- Change into the 'script' directory in this project
- Upload the file (.csv or .xlsx) to the target environments

  ```shell
  ./pod_upload_seed_file preprod /path/to/my_file(.csv|.xlsx)
  ```

- This will output confirmation and provide the name of the pod used e.g.

  ```shell
  File uploaded to /tmp/seed/my_file(.csv|.xlsx) on pod hmpps-approved-premises-api-69cf9df9b8-g4vp2
  ```

- Connect to the aforementioned pod 

  ```shell
  ./pod_shell preprod hmpps-approved-premises-api-69cf9df9b8-g4vp2
  ```

- Run the helper script from within the container to trigger the seed job:

For CSV files:

  ```shell
  /app/run_seed_job {seed type} {file name}
  ```

- `seed type` is a value from the [`SeedFileType`](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/d8dc87aefa0294289a7bcb08048fbd8679b9954c/src/main/resources/static/_shared.yml#L3240) enum in the OpenAPI spec.  e.g.

  ```shell 
  /app/run_seed_job approved_premises ap_seed_file
  ```

For XLSX files:

  ```shell
  /app/run_seed_from_excel_job {seed type} {file name}
  ```

- `seed type` is a value from the [`SeedFromExcelFileType`](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/9a9ad8df7b8015f63799f191fc986e47900325be/src/main/resources/static/_shared.yml#L3730) enum in the OpenAPI spec.  e.g.

  ```shell 
  /app/run_seed_from_excel_job cas1_import_site_survey_rooms ap_seed_file
  ```

- Check the logs using [Azure Application Insights](https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4154196024/Viewing+and+Tailing+Kubernetes+logs) to see how processing is progressing. The following query will provide seed traces and exceptions only:
  ```
  union traces, exceptions
  | where cloud_RoleName == 'approved-premises-api' and customDimensions contains ("uk.gov.justice.digital.hmpps.approvedpremisesapi.seed")
  | project timestamp, message, outerMessage, severityLevel, customDimensions, problemId, operation_Name, operation_Id
   ```
  
## Local Seeding

For seeding in a local development environment use ./script/run_seed_job

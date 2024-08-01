# How to run a seed job remotely

## Run book

To process a seed CSV against a non-local environment:

- Ensure nobody is deploying a change (or is going to deploy a change shortly.)
- Change into the 'script' directory in this project
- Upload the CSV file to the target environments

  ```shell
  ./pod_upload_seed_file preprod /path/to/my_csv.csv
  ```

- This will output confirmation and provide the name of the pod used e.g.

  ```shell
  File uploaded to /tmp/seed/refresh_nat_users.csv on pod hmpps-approved-premises-api-69cf9df9b8-g4vp2
  ```

- Connect to the aforementioned pod 

  ```shell
  ./pod_shell preprod hmpps-approved-premises-api-69cf9df9b8-g4vp2
  ```

- Run the helper script from within the container to trigger the seed job:

  ```shell
  /app/run_seed_job {seed type} {file name}
  ```

- `seed type` is a value from the [`SeedFileType`](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/d8dc87aefa0294289a7bcb08048fbd8679b9954c/src/main/resources/static/_shared.yml#L3240) enum in the OpenAPI spec.  e.g.

  ```shell 
  /app/run_seed_job approved_premises ap_seed_file
  ```

- Check the logs using [Azure Application Insights](https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4154196024/Viewing+and+Tailing+Kubernetes+logs) to see how processing is progressing. The following query will provide seed traces and exceptions only:
  ```
  union traces, exceptions
  | where cloud_RoleName == 'approved-premises-api' and customDimensions contains ("uk.gov.justice.digital.hmpps.approvedpremisesapi.seed")
  | project timestamp, message, outerMessage, severityLevel, customDimensions, problemId, operation_Name, operation_Id
   ```
  
## Local Seeding

For seeding in a local development environment use ./script/run_seed_job

## CSV reference

### User seed job

Required fields:

- `deliusUsername`
- `roles`
- `qualifications`

[Example CSV](./example_csvs/user_seeding_example.csv)

### Approved Premises seed job

Required fields:

- `apCode`
- `qCode`
- `apArea`
- `pdu`
- `probationRegion`
- `localAuthorityArea`
- `town`
- `addressLine1`
- `addressLine2`
- `postcode`
- `latitude`
- `longitude`
- `maleOrFemale`
- `totalBeds`
- `status`
- `isIAP`
- `isPIPE`
- `isESAP`
- `isSemiSpecialistMentalHealth`
- `isRecoveryFocussed`
- `isSuitableForVulnerable`
- `acceptsSexOffenders`
- `acceptsChildSexOffenders`
- `acceptsNonSexualChildOffenders`
- `acceptsHateCrimeOffenders`
- `isCatered`
- `hasWideStepFreeAccess`
- `hasWideAccessToCommunalAreas`
- `hasStepFreeAccessToCommunalAreas`
- `hasWheelChairAccessibleBathrooms`
- `hasLift`
- `hasTactileFlooring`
- `hasBrailleSignage`
- `hasHearingLoop`
- `notes`
- `emailAddress`

[Example CSV](./example_csvs/approved_premises_seeding_example.csv)

### Approved Premises rooms and beds job

"seed type": `approved_premises_rooms`

Required fields:

- `apCode`
- `bedCode`
- `roomNumber`
- `bedCount`
- `isSingle`
- `isGroundFloor`
- `isFullyFm`
- `hasCrib7Bedding`
- `hasSmokeDetector`
- `isTopFloorVulnerable`
- `isGroundFloorNrOffice`
- `hasNearbySprinkler`
- `isArsonSuitable`
- `isArsonDesignated`
- `hasArsonInsuranceConditions`
- `isSuitedForSexOffenders`
- `hasEnSuite`
- `isWheelchairAccessible`
- `hasWideDoor`
- `hasStepFreeAccess`
- `hasFixedMobilityAids`
- `hasTurningSpace`
- `hasCallForAssistance`
- `isWheelchairDesignated`
- `isStepFreeDesignated`
- `notes`
  
[Example CSV](./example_csvs/approved_premises_rooms_seeding_example.csv)

### Approved Premises AP Area (CRU) Email Address Seed Job

"seed type": `approved_premises_ap_area_email_addresses`

Required fields:

- `ap_area_identifier`
- `email_address`
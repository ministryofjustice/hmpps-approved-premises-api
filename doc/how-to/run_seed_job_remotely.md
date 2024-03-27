# How to run a seed job remotely

## Run book

To process a seed CSV against a non-local environment:

- Ensure nobody is deploying a change (or is going to deploy a change shortly.)

- Setup namespace

  ```
  env=preprod
  namespace=hmpps-community-accommodation-$env
  ```

- Run against the namespace for the environment you wish to run the seed job in, e.g:
  ```
  kubectl get pod -n $namespace
  ```

- Copy the name of one of the running `hmpps-approved-premises-api` pods or set it as a variable, e.g:
  ```
  pod={pod name}
  ```

- Transfer the CSV to the `/tmp/seed` directory in the container via kubectl, e.g.
  ```
  kubectl cp /local/path/ap_seed_file.csv $pod:/tmp/seed/ap_seed_file.csv -n $namespace
  ```

- Log in to the same pod in order to run the script, e.g. 
  ```
  kubectl -n $namespace exec --stdin --tty $pod -- /bin/bash 
  ```

- Run the helper script from within the container to trigger the seed job:
  ```
  /app/run_seed_job {seed type} {file name without extension}
  ```

  Where `seed type` is a value from the [`SeedFileType`](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/d8dc87aefa0294289a7bcb08048fbd8679b9954c/src/main/resources/static/_shared.yml#L3240) enum in the OpenAPI spec.  e.g.
  ```
  kubectl -n $namespace exec --stdin --tty $pod -- /bin/bash
  /app/run_seed_job approved_premises ap_seed_file
  ```

- Check the logs using [Azure Application Insights](https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4154196024/Viewing+and+Tailing+Kubernetes+logs) to see how processing is progressing. The following query will provide seed traces and exceptions only:
  ```
  union traces, exceptions
  | where cloud_RoleName == 'approved-premises-api' and customDimensions contains ("uk.gov.justice.digital.hmpps.approvedpremisesapi.seed")
  | project timestamp, message, outerMessage, severityLevel, customDimensions, problemId, operation_Name, operation_Id
   ```
  
## Run book

For a full seeding, e.g. in a local development environment, you can follow this process:

```sh
# characteristics for premises
./script/run_seed_job characteristics premises_characteristics

# characteristics for rooms
./script/run_seed_job characteristics room_characteristics

# premises and their characteristics
./script/run_seed_job approved_premises premises_with_characteristics

# rooms and their characteristics, plus beds
./script/run_seed_job approved_premises_rooms rooms_with_characteristics
```

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
- `characteristics`
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
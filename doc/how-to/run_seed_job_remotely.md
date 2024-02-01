# How to run a seed job remotely

## Run book

To process a seed CSV against a non-local environment:

- Ensure nobody is deploying a change (or is going to deploy a change shortly.)

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
  kubectl exec --stdin --tty $pod -- /bin/bash -n $namespace
  ```

- Run the helper script from within the container to trigger the seed job:
  ```
  /app/run_seed_job {seed type} {file name without extension}
  ```

  Where `seed type` is a value from the [`SeedFileType`](https://github.com/ministryofjustice/hmpps-approved-premises-api/blob/d8dc87aefa0294289a7bcb08048fbd8679b9954c/src/main/resources/static/_shared.yml#L3240) enum in the OpenAPI spec.  e.g.
  ```
  kubectl exec --stdin --tty $pod -- /bin/bash -n $namespace
  /app/run_seed_job approved_premises ap_seed_file
  ```
  
- Check the logs in Azure to see how processing is progressing.
  - [Dev](https://portal.azure.com#@747381f4-e81f-4a43-bf68-ced6a1e14edf/blade/Microsoft_OperationsManagementSuite_Workspace/Logs.ReactView/resourceId/%2Fsubscriptions%2Fc27cfedb-f5e9-45e6-9642-0fad1a5c94e7%2FresourceGroups%2Fnomisapi-t3-rg%2Fproviders%2Fmicrosoft.insights%2Fcomponents%2Fnomisapi-t3/source/LogsBlade.AnalyticsShareLinkToQuery/q/H4sIAAAAAAAAAw3KwQ3CMAwF0DtTRB3AU3BE7GAlX4kB15bttheGh3d%252BFdyRt2%252B7FgKtH1mmd1HsKbYnPWxOxJMVLYuj8pJabTveNO2k179LBw2ZUvyhpe5J7B52YnhAJZHsQgmM7Qf%252BvOMlbQAAAA%253D%253D/timespan/P1D)
  - [Preprod](https://portal.azure.com#@747381f4-e81f-4a43-bf68-ced6a1e14edf/blade/Microsoft_OperationsManagementSuite_Workspace/Logs.ReactView/resourceId/%2Fsubscriptions%2Fa5ddf257-3b21-4ba9-a28c-ab30f751b383%2FresourceGroups%2Fnomisapi-preprod-rg%2Fproviders%2Fmicrosoft.insights%2Fcomponents%2Fnomisapi-preprod/source/LogsBlade.AnalyticsShareLinkToQuery/q/H4sIAAAAAAAAAw3KwQ3CMAwF0DtTRB3AU3BE7GAlX4kB15bttheGh3d%252BFdyRt2%252B7FgKtH1mmd1HsKbYnPWxOxJMVLYuj8pJabTveNO2k179LBw2ZUvyhpe5J7B52YnhAJZHsQgmM7Qf%252BvOMlbQAAAA%253D%253D/timespan/P1D)
  - [Prod](https://portal.azure.com#@747381f4-e81f-4a43-bf68-ced6a1e14edf/blade/Microsoft_OperationsManagementSuite_Workspace/Logs.ReactView/resourceId/%2Fsubscriptions%2Fa5ddf257-3b21-4ba9-a28c-ab30f751b383%2FresourceGroups%2Fnomisapi-prod-rg%2Fproviders%2Fmicrosoft.insights%2Fcomponents%2Fnomisapi-prod/source/LogsBlade.AnalyticsShareLinkToQuery/q/H4sIAAAAAAAAAw3KwQ3CMAwF0DtTRB3AU3BE7GAlX4kB15bttheGh3d%252BFdyRt2%252B7FgKtH1mmd1HsKbYnPWxOxJMVLYuj8pJabTveNO2k179LBw2ZUvyhpe5J7B52YnhAJZHsQgmM7Qf%252BvOMlbQAAAA%253D%253D/timespan/P1D)

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
# How to run a seed job remotely

## Run book

To process a seed CSV against a non-local environment:

- Ensure nobody is deploying a change (or is going to deploy a change shortly.)

- Run `kubectl get pod` against the namespace for the environment you wish to run the seed job in. 

- Copy the name of one of the running `hmpps-approved-premises-api` pods.

- Transfer the CSV to the `/tmp/seed` directory in the container via kubectl, e.g.
  ```
  kubectl cp /local/path/ap_seed_file.csv {pod name}:/tmp/seed/ap_seed_file.csv
  ```

- Log in to the same pod in order to run the script, e.g. 
  ```
  kubectl exec --stdin --tty {pod name} -- /bin/bash
  ```

- Run the helper script from within the container to trigger the seed job:
  ```
  /app/run_seed_job {seed type} {file name without extension}
  ```

  Where `seed type` is a value from the `SeedFileType` enum in the OpenAPI spec.  e.g.
  ```
  kubectl exec --stdin --tty {pod name} -- /bin/bash
  /app/run_seed_job approved_premises ap_seed_file
  ```
  
- Check the logs via `kubectl logs {pod name}` to see how processing is progressing.

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
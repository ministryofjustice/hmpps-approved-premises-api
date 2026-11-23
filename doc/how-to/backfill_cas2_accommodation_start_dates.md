# Backfill CAS2 accommodation start dates

The `cas2_backfill_accommodation_start_date` seed job copies the answer at
`data["cohort-selection"]["accommodation-start-date"].accommodationStartDate` into
`cas_2_applications.accommodation_start_date` for submitted applications with
origin `other` and a null column. Existing dates are not overwritten and
applications without an answer are unchanged.

The seed framework requires a CSV with a data row. Create a file, for example
`accommodation-start-date.csv`, containing exactly:

```csv
run
true
```

The `run` value must be `true`; it triggers the backfill and does not specify
which applications to process. Run the job as described in the
[remote seed job runbook](run_seed_job_remotely.md), for example:

```shell
script/run_remote_seed_job preprod cas2_backfill_accommodation_start_date /path/to/accommodation-start-date.csv
```

Check the seed logs for the number of updated applications. The job is safe to
rerun. If an answer is not a valid date, the transaction is rolled back and the
error is logged with the application ID; investigate the data before rerunning.

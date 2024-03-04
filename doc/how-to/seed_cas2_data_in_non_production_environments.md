# How to seed CAS2 data in non-production environments

The seeding of `Cas2Application` and `Cas2Assessment` data is mostly done automatically for developers when the API
server is started up on the local, dev and test environments, creating a number of Applications and Assessments that can
be used to test out the API or UI.

This seed data is applied to a fresh version of the database in dev and test, which is cleared out on a
deployment, with migration SQL files defined in `src/main/resources/db/migration/dev+test` directory (
namely `R__6_clear_cas2_applications.sql` in our case, which clears all CAS2 Applications). It's recommended to pair any
new seeded data with a relevant `TRUNCATE` in a SQL migration file to ensure we're tidying up after ourselves when
seeding any new data. Data is not currently cleared in the local environment.

Once the slate has been wiped clean, the seed data is applied in one of the two ways outlined below. This how-to
aims to clarify the use case for each type of seeding due to some recent confusion between these two methods.

## Seed jobs

Seed Jobs like the `Cas2ApplicationsSeedJob` are run on start up with the `SeedService`'s `@PostConstruct` annotation.

They read from a CSV file
e.g. [`6_cas2_applications.csv`](src/main/resources/db/seed/local+dev+test/6__cas2_applications.csv) defined in
the `src/main/resources/db/seed/local+dev+test` directory, and run on the three environments in the directory name.

These seeds incidentally manually set a `created_at` value on an Application, meaning checking your own database for
these values to check the seeds are run (e.g. on today's date) may not be reliable. Instead, checking the logs will show
you whether the seed job has been run or a record skipped.

This is suitable for seed data which is not sensitive or private and which will be committed to our public git repo.

### Why we need an alternative means of seeding

This auto-seeding will not currently be very useful in dev/test environments for a number of reasons:

- In CAS2 we will use this for seeding using real `NomisUser`s and `ExternalUser`s (in dev and test environments)
whose usernames we don't want expose in a seed file.
- `POM_USER` does not exist in those environments so won't be able to log in as the applicant and see the dashboard of in
progress/submitted applications.
- Submitted applications won't be shown correctly to a logged in assessor as the API uses
the `PersonTransformer` (`OffenderDetail` and `InmateDetail`) and the subject of our application, "Aadland Bertrand" (
NOMS `A1234AI`) doesn't exist in dev/test.

## AutoScripting

AutoScripting is a form of seeding which does not use CSV seeding files. i.e. the seeding is entirely defined in a
script. As mentioned above, we use this when we need real users and we don't want to commit the usernames of
real `NomisUser`s and `ExternalUser`s in dev and test environments to a seed file in the git repository.

If AutoScript seeding is enabled (it's a sub-set of Auto-seeding), then
the [`Cas2AutoScript`](src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/seed/cas2/Cas2AutoScript.kt) is
run.

That script creates three `Cas2Applications` for each `NomisUser` found in the test/dev database.

Our applications are for a test/dev user called Bertrand Aadland (NOMS G9542VP / CRN X320741) who exists in both
Community API and the Prison API.

## Further reading

1. [PR introducing AutoSeeding for the local environment](https://github.com/ministryofjustice/hmpps-approved-premises-api/pull/1196)
2. [PR introducing AutoScripting](https://github.com/ministryofjustice/hmpps-approved-premises-api/pull/1207)

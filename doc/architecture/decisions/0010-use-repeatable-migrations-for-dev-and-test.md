# 10. use_repeatable_migrations_for_dev_and_test

Date: 2023-01-17

## Status

Accepted

## Context

We need to ensure that our development and test environments have
predictable data, so we can log in, run tests and carry out usability
testing sessions with users. As we run automated tests on dev, it's also
important that the environment doesn't get clogged with automatically
created data.

## Decision

We will use [repeatable Flyway migrations](https://flywaydb.org/blog/flyway-timestampsandrepeatables)
to tear down and set up the data to a known state.

These migrations will be in the format:

```text
R__$NUM_$NAME_OF_MIGRATION.sql
```

Where `$NUM` is the order we want the migration to be run in, and
`$NAME_OF_MIGRATION` is a description of the migration to be run.

Each migration will also have a comment at the top like so:

```sql
-- ${flyway:timestamp}
```

This changes the checksum for the repeatable migrations, meaning they
get executed each time we migrate.

In dev, we will have SQL migrations manually written to drop the
tables and insert the data previously written in the previous data
migrations. We will also drop the bookings table to remove any
bookings created in the e2e tests.

For test, we have a [Wiremock repo](https://github.com/ministryofjustice/hmpps-community-accommodation-wiremock),
which mocks away external services.
This repo also has a helper script, which uses mock data to generate /
re-generate migrations based on any changes made to the mocks.

When data changes in the mocks, we will the following command in
the Wiremock project:

```bash
script/generate_migrations /path/to/api/directory
```

to generate new migrations. We will then open a PR in the API to
project to commit the new migrations to the repo.

To ensure these migrations are run on deploy, we add the paths to the
`SPRING_FLYWAY_LOCATIONS` environment variable in the appropriate
directories, i.e:

```yaml
SPRING_FLYWAY_LOCATIONS: classpath:db/migration/all,classpath:db/migration/dev+test
```

```yaml
SPRING_FLYWAY_LOCATIONS: classpath:db/migration/all,classpath:db/migration/dev,classpath:db/migration/test
```

## Consequences

This will ensure that all data in both dev and test environments
are cleaned up on every deploy. This means that we should consider
data in both of those environments to be ephemeral, unless it is
specifially added to a repeatable migration.

As these migrations are destructive, it is important that these
migrations are not run in prod or pre-prod. We see the fact that
the running of these migrations is controlled by a environment
variable change, which will be approved by another developer, to
be an appropriate safeguard against this.

We will also need to be aware that in some rare cases, a
deployment may run at the same time as e2e tests running, which
may break the tests. If this happens, we'll have to rerun the
tests.

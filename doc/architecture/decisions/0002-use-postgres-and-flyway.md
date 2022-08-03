# 2. Use Postgres and Flyway

Date: 2022-08-02

## Status

Accepted

## Context

We need to decide on a database technology to use and a way of applying updates to its schema.

## Decision

We will use Postgres as a database, this is the prevalent choice amongst existing services.

We will use Flyway to run schema migrations in a predictable manner at application startup, Flyway is widely 
used and mature.

## Consequences

We will be able to persist data via Postgres and reliably update the database schema via Flyway.

Flyway does sometimes need manual intervention if a migration is attempted that runs into an issue.  
In such cases manual intervention is required then the flyway_migration table (which is automatically created) needs 
to be updated.  This issue should never occur past local development since integration tests will catch it.

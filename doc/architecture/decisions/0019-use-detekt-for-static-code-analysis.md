# 19. use detekt for static code analysis

Date: 2023-10-31

## Status

Accepted

## Context

We have observed performance issues in the API. We think these are mostly down
to the fact applications have a json blob attached to them so queries to load
those could/have led to out of memmory errors.

We have started building out load testing using Gatling but the effort required
to get this set up for more of the service and plumbed into CI requires more
effort.

We asked MoJ if there was a static code analyser tool of choice. References to
both detekt and sonarqube were found in Slack. detekt ended up being recommended
to us and as it's used by at least one other team we're happy to pull towards
any pre-existing standard we can find.

## Decision

We will use detekt as our static code analyser to require to help us improve
code quality and prevent inefficiencies from making it to production.

We will start with detekts default rules and suppress all existing failures.

## Consequences

- adding this retrospectively to a project means we'll have to live with all
  existing issues until time can be made to address each one
- new code should be more reliable
- the team may need to readjust their coding style to conform to detekt
- the team may need to decide on and add rule exceptions or tweaks where it
  makes sense to do so
- this choice does not remove the need for Gatling nor the need to automate that
  as part of CI

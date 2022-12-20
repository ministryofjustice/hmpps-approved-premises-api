# 9. release with continuous deployment

Date: 2022-12-19

## Status

Accepted

## Context

The context around this decision goes beyond this single repository and affects
the whole CAS team, as such we've written [the context in Confluence](https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4247847062/Release+process).

## Decision

The CAS team will use the continuous deployment methodology to release our work
to our three environments.

## Consequences

- redefining our definition of done to include the release all the way to live
  will add overhead to each individual piece of work
- as the author will be responsible for releasing into live before moving on to
  other features, they will be best placed to quickly recover from failure
- as we'll always be in a deployable state we should be able to quickly push
  through hot fixes with low risk
- we will avoid big bang releases that are riskier and can be harder and slower
  to fix
- we want to extend our pull request templates to prompt us to follow the new
  deployment steps
- we will follow the convention found in MoJ which should make it more familiar
  when we exit and hand the project back to MoJ
- all CAS services will head in the same direction that will make it easier to
  for us to work across the set of codebases

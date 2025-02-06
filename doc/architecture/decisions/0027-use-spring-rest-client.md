# 27 Use Spring REST Client

## Status

Accepted

## Context

We currently communicate with several upstream services

There are various configuration required to do this:

* environment specific URLs
* retry on certain error types
* oauth token retrieval and usage

To achieve this our code uses a custom `BaseHMPPSClient` which wraps Spring's Reactive Web Client. 

There are some minor issues with `BaseHMPPSClient`

* We don't leverage the reactive programming model provided by Spring's Reactive Web Client, and instead wrap the client to use it as a synchronous client (which adds complexity in `BaseHMPPSClient`). None of our controllers use WebFlux (e.g. return a `Mono<>` response) which enable the reactive programming model.
* The `BaseHMPPSClient` is a large class, mixing various concerns (retry, caching) into a single class
* The response hierarchy provided by `ClientResult` is complicated and littered with various caching information that is only used by a few clients
* It doesn't support providing a complete URL to call (i.e. the base url is configured at startup). This will be required going forward.

Whilst the `BaseHMPPSClient` works well for us, other projects (e.g .hmpps-probation-integration) using the Spring Rest Client directly which appears to produce more fluent code when used in a synchronous request/response pattern. The `BaseHMPPSClient` is also becoming increasingly complex

## Options

1. Continue using `BaseHMPPSClient`

Pros

* Less initial code change
* Doesn't create 'tech debt' of having to migrate existing clients to a different approach
* We don't lose the opportunity to use reactive programming. Given we don't have reactive controllers, this is unlikely to be required anytime soon

Cons

* `BaseHMPPSClient` really needs refactoring in the near future as it's becoming overly complicated
* Less aligned with other project's best practice
* More complex programming model 'under the hood' and in the return type

2. Start using `RestClient` for new code

Pros

* Better aligned with other HMPPS projects
* Less complicated programming model
* Will provide better seperated configuration, retry logic etc.
* An opportunity to remove complicated caching logic from clients which isn't used in most cases, and simplify response type
* We can re-use existing probation-integration code for retries and OAuth configuration, minimising effort

Cons

* The clients throw exceptions to indicate errors, where-as our `BaseHMPPSClient` returns a Sealed Type hierarchy, providing a more functional 'kotlin-like' programming model. We could add utility functions when using the RestClient to return a similar type hierarchy
* Creates tech debt as we'll want to migrate existing clients to use the new model
* We'll be dual running two different approaches for a while

## Decision

Use `RestClient`

## Caveats

None

## Consequences

This will create tech debt as we should migrate existing clients to use `RestClient`
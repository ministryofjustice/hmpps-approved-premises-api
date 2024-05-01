# 12. Handling feature flags with Flipt

Date: 2024-04-30

## Status

Accepted

## Context

As the service gets used by more people, and communicating new features / changes with
users becomes more essential, it's important that sometimes we hide certain features
behind a feature flag to ensure we can roll out features in a controlled manner, whilst
not slowing down the development process.

Previously we've handled feature flags in an adhoc way, by use of environment variables,
but this causes a couple of issues, most importantly, to roll back or introduce a feature
we need to start a new deploy, which can take time, as we need to go through the whole
deployment process, from PR, through to E2E tests and deployment to various different
environments. We also need to have a lot of code that handles fetching these variables,
adding conditionals etc, which gives us more code to maintain.

## Decision

The integrations team have recently deployed an instance of [Flipt](https://www.flipt.io/),
which is a simple service that allows us to manage feature flags and turn them on and off
at will via a UI, without having to redeploy the service.

There are instances for each environment, Dev, Prod and Prepod, meaning we can have
different values for feature flags in each environment.

To manage feature flags, we will add a new Feature Flag service, which (initially at least)
will accept the name of a feature flag and return a boolean value indicating whether the
feature flag is enabled or not.

In local development, or where we set `flipt.enabled` to false, we will enable all feature
flags by default.

A Feature Flag service will be provided to return the value of a feature flag, given its name

## Consequences

It's important to point out that even now, feature flags have a cost to their maintenance,
and we should only use them as a last resort. We should also consider the risks that occur
if someone within the team enables or disables a feature flag without communicating this
to the team, so we should ensure that when we do anything related to feature flags we are
sure that:

- This has been communicated to the team; and
- We have agreement from the business (SMEs etc) that this feature flag is ready to be enabled

Once a feature is stable, we should remove any associated code and logic to enable/disable
the feature as soon as possible.

Additionally, the Dev instance of the Flipt service can only be accessed via GlobalProtect
or the dxw VPN, so if we're using Flipt locally, we should either be on an MoJ machine or
connected to the dxw VPN.

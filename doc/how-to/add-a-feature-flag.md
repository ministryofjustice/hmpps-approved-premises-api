# Feature flags

Feature flags are managed by [Flipt](https://www.flipt.io). We use hosted Flipt instances
for all of our live environments:

- [Dev / Test](https://feature-flags-dev.hmpps.service.justice.gov.uk)
- [Preprod](https://feature-flags-preprod.hmpps.service.justice.gov.uk)
- [Prod](https://feature-flags-dev.hmpps.service.justice.gov.uk)

## Adding a feature flag

### Step 0: Are you sure you need a feature flag?

Consider if a feature flag is necessary for the feature. If it's a small feature, like
a copy change, or some other small quality of life change, then you probably don't need a
feature flag. If it's a large feature that requires multiple PRs and is likely to need
communicating to users, then a feature flag _might_ be the way to go. It is still worth
considering other alternative routes before going down this one though.

### Step 1: Add a new flag in the Flipt UI

Go to the Flipt UI for each environment, and create a new flag in the
`community-accommodation` namespace for each environment. Currently only boolean flags
are supported:

![KgXgB09MLS](https://github.com/ministryofjustice/hmpps-approved-premises-ui/assets/109774/2365414a-7d45-41b4-8370-625d78285b56)

## Step 2: Inject the FeatureFlagService into your controller

In your controller, add the `FeatureFlagService` to the constructor of the controller
you want to add the feature flag to

## Step 3: Call the `getBooleanFlag` method in your controller action

You can then call the `getBooleanFlag` method to return a boolean value depending on if
the flag is enabled or not, e.g (where `FLAG_NAME` is a string specifying the key of the
flag you created in step 1)

You can then pass that boolean value to wherever it is required in your code.

NOTE: By default, Flipt is disabled in local development environments, and `getBooleanFlag`
will always return true

## Step 5: Enable/disable the flag in the appropriate environment

Once your code is deployed, you can then enable/disable the flag in the appropriate
environment like so:

![cq4ZJ73Z6a](https://github.com/ministryofjustice/hmpps-approved-premises-ui/assets/109774/92b1892c-0fbe-4537-9ef1-11e6ed1c9566)

NOTE: Ensure that when enabling/disabling feature flags this has been clearly discussed
with users / SMEs

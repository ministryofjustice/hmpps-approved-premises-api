# Feature flags

Feature flags are managed by spring configuration, set via environment-specific ENV VARs

## Adding a feature flag

### Step 0: Are you sure you need a feature flag?

Consider if a feature flag is necessary for the feature. If it's a small feature, like
a copy change, or some other small quality of life change, then you probably don't need a
feature flag. If it's a large feature that requires multiple PRs and is likely to need
communicating to users, then a feature flag _might_ be the way to go. It is still worth
considering other alternative routes before going down this one though.

### Step 1: Add a default value flag in spring configuration

Ensure an entry exists as follows in application.yml providing a default value

```
feature-flags:
   my-flag: false
```

You may wish to set a default in application-test.yml too for integration tests

### Step 2: Add the new flag in environment variables

Update the environment-specific helm configurations (see /helm), adding
environment-specific entries to override the spring configuration e.g.

```values-dev.yml:
  env:
     FEATURE-FLAGS_MY-FLAG: true
```

### Step 3: Inject the FeatureFlagService into your controller

In your controller, add the `FeatureFlagService` to the constructor of the controller
you want to add the feature flag to

### Step 4: Call the `getBooleanFlag` method in your controller action

You can then call the `getBooleanFlag` method to return the flag value


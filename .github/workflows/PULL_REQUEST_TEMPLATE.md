# Changes in this PR

# Release checklist

As part of our continuous deployment strategy we must ensure that this work is
ready to be released at any point. Before merging to `main` we must first
confirm:

## Pre merge checklist

- [ ] Have all changes to any dependencies been deployed to both `preprod` and
    `production` in a backwards compatible way? (This includes our API,
    infrastructure, third-party integrations and any secrets or environment variables)
- [ ] Has a required data migration been included in this change or been done in
    advance?

## Post merge checklist

Once we've merged we now need to release this through to production before
considering it done.

[Find the build-and-deploy job in CircleCI](https://app.circleci.com/pipelines/github/ministryofjustice/hmpps-temporary-accommodation-ui)
and work through the following steps:

- [ ] Has the product manager asked to provide approval for this feature?
  - [ ] Has the product manager approved?
- [ ] Manually approve release to preprod
- [ ] Verify change released to preprod
- [ ] Manually approve release to prod
- [ ] Verify change released to production

Should a release fail at any step, you as the author should now lead the work to
fix it as soon as possible.

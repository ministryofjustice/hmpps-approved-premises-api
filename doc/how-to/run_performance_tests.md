# How to run performance tests
Performance tests use [Gatling](https://gatling.io), which provides a Gradle plugin.
This bundles up Gatling in a convenient way so that no additional tools need to be downloaded.

## Running against a local instance
This requires the [ap-tools](https://github.com/ministryofjustice/hmpps-approved-premises-tools)
utility to be running.

Once the API has started locally, just run:
```shell
./gradlew gatlingRun
```

## Running against a remote environment
This requires some configuration, which can be done through environment variables:
```shell
GATLING_BASE_URL="https://approved-premises-api-{env}.hmpps.service.justice.gov.uk" \
GATLING_USERNAME="{your username}" \
GATLING_PASSWORD="{your password}" \
GATLING_HMPPS_AUTH_BASE_URL="https://sign-in-{env}.hmpps.service.justice.gov.uk" \
./gradlew gatlingRun
```

## Results
Once Gatling has finished, it will output reports into the `build/reports/gatling` folder.
It will also provide a link in the terminal to open the report directly in the browser.
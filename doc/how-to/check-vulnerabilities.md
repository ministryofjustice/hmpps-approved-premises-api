# How to check vulnerabilities in the API

There are two dependency analysis tools used in the API: [Trivy](https://trivy.dev/) and
[Dependency-Check-Gradle](https://github.com/dependency-check/dependency-check-gradle).

These are automatically run on the `main` branch in CircleCI once every weekday, at 05:11.
However, these can also be run locally or manually triggered on any branch in CircleCI.

## Check locally
To run a Trivy scan locally, run:
```shell
script/trivy_scan
```

To run the Gradle dependency check locally, run:
```shell
./gradlew dependencyCheckAnalyze
```

## Triggered check on CircleCI
To trigger a check on CircleCI:
- Select your branch on the dropdown on the
[CircleCI dashboard](https://app.circleci.com/pipelines/github/ministryofjustice/hmpps-approved-premises-api).
- Press the `Trigger Pipeline` button.
- Expand the `Add Parameters (optional)` section.
- Set the parameter type to `boolean`, the name to `run-security-workflow-on-branch`, and the value to `true`.
- Press the `Trigger Pipeline` button.

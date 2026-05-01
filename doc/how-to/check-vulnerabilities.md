# How to check vulnerabilities in the API

The following tools are used to analyse dependencies:

* [OWASP Dependency Check](https://github.com/dependency-check/dependency-check-gradle).

These are automatically ran by github actions on every week day

They can also be run locally or manually triggered on any branch

## Check locally

To run the Gradle dependency check locally, run:

```shell
./gradlew dependencyCheckAnalyze
```

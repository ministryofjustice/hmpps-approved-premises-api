# Approved Premises API

This is the backend for the Approved Premises service. Its API is consumed by the corresponding "UI" codebase ([approved-premises-ui](https://github.com/ministryofjustice/approved-premises-ui)).

## Prerequisites

TBC

## Setup

When running the application for the first time, run the following command:

```bash
script/setup # TODO - this script is currently a stub
```

If you're coming back to the application after a certain amount of time, you can run:

```bash
script/bootstrap # TODO - this script is currently a stub
```

## Running the application

To run the server, from the root directory, run:

```bash
script/server
```

This runs the project as a Spring Boot application on `localhost:8080`

### Running/Debugging from IntelliJ

To run from IntelliJ, first start the database:

```bash
script/development_database
```

Then in the "Gradle" panel (`View->Tool Windows->Gradle` if not visible), expand `approved-premises-api`, `Tasks`, 
`application` and right click on `bootRunLocal` and select either Run or Debug.

## Running the tests

To run linting and tests, from the root directory, run:

```bash
script/test
```

### Running/Debugging from IntelliJ

To run from IntelliJ, first start the database:

```bash
script/test_database
```

Then either:
 - Run or Debug the `verification`, `test` Task from the "Gradle" panel
 - Open an individual test class and click the green play icon in the gutter at the class level or on a specific test method

## OpenAPI documentation

The API which is offered to front-end UI apps is documented using Swagger/OpenAPI.
The initial contract covers the migration of certain bed-management functions from Delius into the new service.

This is available in development at [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## Creating a CloudPlatform namespace

When deploying to a new namespace, you may wish to use this template kotlin project namespace as the basis for your new namespace:

<https://github.com/ministryofjustice/cloud-platform-environments/tree/main/namespaces/live.cloud-platform.service.justice.gov.uk/hmpps-template-kotlin>

Copy this folder, update all the existing namespace references, and submit a PR to the CloudPlatform team. Further instructions from the CloudPlatform team can be found here: <https://user-guide.cloud-platform.service.justice.gov.uk/#cloud-platform-user-guide>

## Renaming from HMPPS Template Kotlin - github Actions

Once the new repository is deployed. Navigate to the repository in github, and select the `Actions` tab.
Click the link to `Enable Actions on this repository`.

Find the Action workflow named: `rename-project-create-pr` and click `Run workflow`.  This workflow will
execute the `rename-project.bash` and create Pull Request for you to review.  Review the PR and merge.

Note: ideally this workflow would run automatically however due to a recent change github Actions are not
enabled by default on newly created repos. There is no way to enable Actions other then to click the button in the UI.
If this situation changes we will update this project so that the workflow is triggered during the bootstrap project.
Further reading: <https://github.community/t/workflow-isnt-enabled-in-repos-generated-from-template/136421>


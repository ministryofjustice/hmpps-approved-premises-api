# Subject Access Request

## Overview

* We must implement and maintain endpoints to provide data and a template for SAR reports
* The SAR implementation in CAS (Approved Premises) aggregates data from all services (CAS1, CAS2, CAS2 HDC and CAS3) into a single report.
* Whilst we define a single template (a limitation of the SAR framework we're implementing), there are separate services defined to collate data for each CAS
* For more information on SAR, see the [SAR confluence pages](https://dsdmoj.atlassian.net/wiki/spaces/NDSS/pages/6057492792/Subject+Access+Request+-+HMPPS+Digital)

## Process

* Whenever the SAR is updated we need to follow an 'offline' change control process
* This process is documented on the [SAR confluence page](https://dsdmoj.atlassian.net/wiki/spaces/NDSS/pages/6057492792/Subject+Access+Request+-+HMPPS+Digital)
* Given this, before _any_ changes are merged to main for the SAR template, we must seek approval
* As part of this approval sample PDF report(s) will be requested. These can be generated from test code, see 'PDF Creation' below

## Components

- **Template**: The report is rendered using a single Mustache template located at `src/main/resources/sar/template_hmpps-approved-premises-api.mustache`.
- **API Response**: The raw data returned by the SAR service is a JSON object.
- **Fixtures**: Each service has its own set of "expected" fixtures (JSON and HTML) used for compliance testing. These are located in `src/test/resources/sar/`.
- **PDF Creation**: During test execution (e.g. Cas1SarComplianceTest), a PDF version of the report is generated and saved to `build/test-generated/<service>-sar-generated-report.pdf` (e.g. `cas1-sar-generated-report.pdf`). This allows for manual verification of the final PDF layout and styling. To generate the PDFs for all four services in one go run `script/test_database`) and then run `./gradlew generateSarPdfs`
- **Asserter**: The `CasSarFixtureAsserter` class is used to verify the actual output against these fixtures. The rationale for using this custom asserter instead of the standard library components is documented in its Javadoc.

## Generating Test Fixtures

When you make changes to the SAR data retrieval logic or the Mustache template, the existing tests will likely fail because the actual output no longer matches the stored "expected" fixtures.

You can automatically generate the actual output of the tests by setting the `SAR_GENERATE_ACTUAL` environment variable to `true`.

### 1. Run the tests with the generation flag

note - we could add a gradle task to simplify the following

To generate new fixtures for a specific service (e.g., CAS3), run the following command:

```bash
SAR_GENERATE_ACTUAL=true ./gradlew integrationTest --tests "uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.sar.Cas1SarComplianceTest"
```

Change the provided test class name depending on which template you are trying to generate fixtures for:
- CAS1: `uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.sar.Cas1SarComplianceTest`
- CAS2 HDC: `uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration.sar.Cas2HdcSarComplianceTest`
- CAS2: `uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.sar.Cas2SarComplianceTest`
- CAS3: `uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.sar.Cas3SarComplianceTest`

### 2. Locate the generated files

The tests are configured to save the actual output to `.log` files in the `src/test/resources/` directory (not in the `sar/` subdirectory) to avoid overwriting the source of truth until it's ready e.g.

- `cas1-expected-api-response.json.log`
- `cas1-expected-report.html.log`

### 3. Update the expected fixtures

Once you have verified that the generated `.log` files contain the correct and expected data, copy them over the existing expected fixtures in `src/test/resources/sar/`.

**Example for CAS1:**

```bash
cp src/test/resources/cas1-expected-api-response.json.log src/test/resources/sar/cas1-expected-api-response.json
cp src/test/resources/cas1-expected-report.html.log src/test/resources/sar/cas1-expected-report.html
```

## Template Modifications

The `template_hmpps-approved-premises-api.mustache` file is shared across all services. 

- Use conditional sections like `{{#CAS1}}...{{/CAS1}}` or `{{#CAS3}}...{{/CAS3}}` to isolate service-specific sections.
- If you add new data to the API response, you must update the template to display it.
- After updating the template, you **must** regenerate the HTML fixtures for **all** services that might be affected to ensure their compliance tests still pass.
- Changes to the template require an offline process to be completed before merging to `main`. The `SAR Template Change` workflow (`.github/workflows/sar_template_change.yml`) posts a checklist comment on any PR that modifies the template.

## Schema and Integration Tests

The `SarIntegrationTest` class contains tests for verifying the SAR template endpoints and ensuring the database schema matches expectations.

### Flyway Schema Test

The `FlywaySchemaTest` verifies that the Flyway migrations are up to date and match the version expected by the SAR library.

```bash
./gradlew integrationTest --tests "uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.SarIntegrationTest"
```

The intentions of these tests are to 'catch' changes to the database schema, prompting the developer to consider if a change to SAR is required

### JPA Entities Test (Entity Schema)

The `JpaEntitiesTest` ensures that the JPA entities in the project correctly map to the database schema and match the expected JSON representation used by the SAR service.

If you add or modify JPA entities, you may need to update the `cas-entities-schema.json` fixture.

The intentions of these tests are to 'catch' changes to the database schema, prompting the developer to consider if a change to SAR is required

#### 1. Generate the new entity schema

```bash
SAR_GENERATE_ACTUAL=true ./gradlew integrationTest --tests "uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.SarIntegrationTest"
```

#### 2. Update the fixture

The generated schema will be saved to `src/test/resources/entity-schema.json.log`.(The SAR test library stores the file in this location.)

#### 3.Copy it to the correct location:

```bash
cp src/test/resources/entity-schema.json.log src/test/resources/sar/cas-entities-schema.json
```

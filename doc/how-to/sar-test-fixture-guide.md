# Subject Access Request (SAR) Test Fixtures

This document explains how the SAR (Subject Access Request) test fixtures are structured, how the HTML report is generated using a Mustache template, and how to update the expected test fixtures when changes are made.

## Overview

The SAR implementation in CAS (Approved Premises) aggregates data from all services (CAS1, CAS2, CAS2v2, and CAS3) into a single report. 

- **Template**: The report is rendered using a single Mustache template located at `src/main/resources/sar/template_hmpps-approved-premises-api.mustache`.
- **API Response**: The raw data returned by the SAR service is a JSON object.
- **Fixtures**: Each service has its own set of "expected" fixtures (JSON and HTML) used for compliance testing. These are located in `src/test/resources/sar/`.
- **PDF Creation**: During test execution, a PDF version of the report is generated and saved to `build/test-generated/sar-generated-report.pdf`. This allows for manual verification of the final PDF layout and styling.
- **Asserter**: The `CasSarFixtureAsserter` class is used to verify the actual output against these fixtures. The rationale for using this custom asserter instead of the standard library components is documented in its Javadoc.

## Generating Test Fixtures

When you make changes to the SAR data retrieval logic or the Mustache template, the existing tests will likely fail because the actual output no longer matches the stored "expected" fixtures.

You can automatically generate the actual output of the tests by setting the `SAR_GENERATE_ACTUAL` environment variable to `true`.

### 1. Run the tests with the generation flag

To generate new fixtures for a specific service (e.g., CAS3), run the following command:

```bash
SAR_GENERATE_ACTUAL=true ./gradlew integrationTest --tests "uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.sar.Cas3SarComplianceTest"
```

For other services, change the test class name:
- CAS1: `uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.sar.Cas1SarComplianceTest`
- CAS2: `uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.sar.Cas2SarComplianceTest`
- CAS2v2: `uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration.sar.Cas2v2SarComplianceTest`

### 2. Locate the generated files

The tests are configured to save the actual output to `.log` files in the `src/test/resources/` directory (not in the `sar/` subdirectory) to avoid overwriting the source of truth until it's ready.

Commonly generated filenames include:
- `cas1-sar-api-response.json.log`
- `cas1-sar-report.html.log`
- `cas2-sar-api-response.json.log`
- `cas2-sar-report.html.log`
- `cas3-sar-api-response.json.log`
- `cas3-sar-report.html.log` 

### 3. Update the expected fixtures

Once you have verified that the generated `.log` files contain the correct and expected data, copy them over the existing expected fixtures in `src/test/resources/sar/`.

**Example for CAS3:**

```bash
cp src/test/resources/cas3-sar-api-response.json.log src/test/resources/sar/cas3-expected-api-response.json
cp src/test/resources/cas3-sar-report.html.log src/test/resources/sar/cas3-expected-report.html
```

**Example for CAS1:**

```bash
cp src/test/resources/cas1-sar-api-response.json.log src/test/resources/sar/cas1-expected-api-response.json
cp src/test/resources/cas1-sar-report.html.log src/test/resources/sar/cas1-expected-report.html
```

## Template Modifications

The `template_hmpps-approved-premises-api.mustache` file is shared across all services. 

- Use conditional sections like `{{#ApprovedPremises}}...{{/ApprovedPremises}}` or `{{#TemporaryAccommodation}}...{{/TemporaryAccommodation}}` to isolate service-specific sections.
- If you add new data to the API response, you must update the template to display it.
- After updating the template, you **must** regenerate the HTML fixtures for **all** services that might be affected to ensure their compliance tests still pass.

## Schema and Integration Tests

The `SarIntegrationTest` class contains tests for verifying the SAR template endpoints and ensuring the database schema matches expectations.

### Flyway Schema Test

The `FlywaySchemaTest` verifies that the Flyway migrations are up to date and match the version expected by the SAR library.

```bash
./gradlew integrationTest --tests "uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.SarIntegrationTest"
```

### JPA Entities Test (Entity Schema)

The `JpaEntitiesTest` ensures that the JPA entities in the project correctly map to the database schema and match the expected JSON representation used by the SAR service.

If you add or modify JPA entities, you may need to update the `cas-entities-schema.json` fixture.

#### 1. Generate the new entity schema

```bash
SAR_GENERATE_ACTUAL=true ./gradlew integrationTest --tests "uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.SarIntegrationTest"
```

#### 2. Update the fixture

The generated schema will be saved to `src/test/resources/entity-schema.json.log`. Copy it to the correct location:

```bash
cp src/test/resources/entity-schema.json.log src/test/resources/sar/cas-entities-schema.json
```

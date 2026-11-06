package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASarClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import javax.sql.DataSource

class SarIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var entityManager: EntityManager

  /**
   * High level basic tests of the SAR endpoints
   *
   * Substantial endpoints tests that verifying data and template responses are defined per CAS:
   *
   * * [uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.sar.Cas1SarComplianceTest]
   * * [uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.sar.Cas2SarComplianceTest]
   * * [uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration.sar.Cas2HdcSarComplianceTest]
   * * [uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.sar.Cas3SarComplianceTest]
   */
  @Nested
  inner class TemplateTest {

    @Test
    fun `returns 400 when neither PRN nor CRN is provided`() {
      givenASarClientCredentialsApiCall { jwt ->
        webTestClient.get()
          .uri("/subject-access-request")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Test
    fun `returns 200 OK with template content for authorised user`() {
      givenASarClientCredentialsApiCall { jwt ->
        webTestClient.get()
          .uri("/subject-access-request/template")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus().isOk
          .expectHeader().contentType("text/plain")
          .expectBody<String>().value { content ->
            assert(content!!.contains("{{#CAS1}}"))
            assert(content.contains("{{#CAS3}}"))
            assert(content.contains("{{#CAS2-HDC}}"))
            assert(content.contains("{{#CAS2}}"))
          }
      }
    }

    @Test
    fun `returns 403 Forbidden for user without required role`() {
      givenASarClientCredentialsApiCall(role = "OTHER") { jwt ->
        webTestClient.get()
          .uri("/subject-access-request/template")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  /**
   * Fails if the latest flyway schema version doesn't match the 'expected-flyway-schema-version'
   * value in application-test-sar.yml
   *
   * The intention of this test is to make you think about whether an update to SAR template
   * is required when the database schema changes
   *
   * Once that decision has been made, fix this test by running `./gradlew updateSarSchema`
   *
   * For more information see docs/how-to/subject-access-requests.md
   */
  @Nested
  inner class FlywaySchemaTest : SarFlywaySchemaTest {
    override fun getDataSourceInstance() = dataSource
    override fun getSarHelper() = sarIntegrationTestHelper
  }

  /**
   * Fails if the persisted snapshot of the entity model match the value in sar/cas-entities-schema.json
   *
   * The intention of this test is to make you think about whether an update to SAR template
   * is required when the database schema changes
   *
   * For more information see docs/how-to/subject-access-requests.md
   *
   * Once that decision has been made, fix this test by running `./gradlew updateSarSchema`
   */
  @Nested
  inner class JpaEntitiesTest : SarJpaEntitiesTest {
    override fun getSarHelper() = sarIntegrationTestHelper
    override fun getEntityManagerInstance() = entityManager
  }
}

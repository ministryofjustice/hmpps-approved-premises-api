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

/**
 * For information on SAR testing, see doc/how-to/sar-test-fixture-guide.md
 */
class SarIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var entityManager: EntityManager

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
            assert(content!!.contains("{{#ApprovedPremises}}"))
            assert(content.contains("{{#TemporaryAccommodation}}"))
            assert(content.contains("{{#ShortTermAccommodation}}"))
            assert(content.contains("{{#BailAccommodation}}"))
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

  @Nested
  inner class FlywaySchemaTest : SarFlywaySchemaTest {
    override fun getDataSourceInstance() = dataSource
    override fun getSarHelper() = sarIntegrationTestHelper
  }

  @Nested
  inner class JpaEntitiesTest : SarJpaEntitiesTest {
    override fun getSarHelper() = sarIntegrationTestHelper
    override fun getEntityManagerInstance() = entityManager
  }
}

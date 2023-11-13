package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 Assessor`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import java.time.OffsetDateTime

class Cas2StatusUpdateTest : IntegrationTestBase() {

  @SpykBean
  lateinit var realStatusUpdateRepository: Cas2StatusUpdateRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realStatusUpdateRepository)
  }

  @Nested
  inner class ControlsOnInternalUsers {
    @Test
    fun `creating a status update is forbidden to internal users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_PRISON"),
      )

      webTestClient.post()
        .uri("/cas2/submissions/de6512fc-a225-4109-bdcd-86c6307a5237/status-updates")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `creating a status update without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas2/submissions/de6512fc-a225-4109-bdcd-86c6307a5237/status-updates")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class PostToCreate {
    @Test
    fun `Create status update returns 201 and creates StatusUpdate when given status is valid`() {
      `Given a CAS2 Assessor`() { _, jwt ->
        `Given a CAS2 User` { applicant, _ ->
          val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
          val application = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(applicant)
            withApplicationSchema(jsonSchema)
            withSubmittedAt(OffsetDateTime.now())
          }

          Assertions.assertThat(realStatusUpdateRepository.count()).isEqualTo(0)

          webTestClient.post()
            .uri("/cas2/submissions/${application.id}/status-updates")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas2ApplicationStatusUpdate(newStatus = "moreInfoRequested"),
            )
            .exchange()
            .expectStatus()
            .isCreated

          Assertions.assertThat(realStatusUpdateRepository.count()).isEqualTo(1)

          val persistedStatusUpdate = realStatusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id)
          Assertions.assertThat(persistedStatusUpdate).isNotNull

          val appliedStatus = Cas2ApplicationStatusSeeding.statusList()
            .find { status -> status.id == persistedStatusUpdate?.statusId ?: fail("The expected StatusUpdate was not persisted") }

          Assertions.assertThat(appliedStatus!!.name).isEqualTo("moreInfoRequested")
        }
      }
    }

    @Test
    fun `Create status update returns 404 when application not found`() {
      `Given a CAS2 Assessor`() { _, jwt ->
        webTestClient.post()
          .uri("/cas2/submissions/66f7127a-fe03-4b66-8378-5c0b048490f8/status-updates")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas2ApplicationStatusUpdate(newStatus = "moreInfoRequested"),
          )
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Create status update returns 400 when new status NOT valid`() {
      `Given a CAS2 Assessor`() { _, jwt ->
        `Given a CAS2 User` { applicant, _ ->
          val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
          val application = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(applicant)
            withApplicationSchema(jsonSchema)
            withSubmittedAt(OffsetDateTime.now())
          }

          webTestClient.post()
            .uri("/cas2/submissions/${application.id}/status-updates")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas2ApplicationStatusUpdate(newStatus = "invalidStatus"),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.detail").isEqualTo("The status invalidStatus is not valid")
        }
      }
    }
  }
}

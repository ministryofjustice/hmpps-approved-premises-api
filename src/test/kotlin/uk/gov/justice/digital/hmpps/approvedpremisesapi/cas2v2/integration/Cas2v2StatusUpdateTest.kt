package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2StatusUpdateTest(
  @Value("\${url-templates.frontend.cas2v2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2v2.application-overview}") private val applicationOverviewUrlTemplate: String,
) : Cas2v2IntegrationTestBase() {

  @SpykBean
  lateinit var realCas2v2StatusUpdateRepository: Cas2v2StatusUpdateRepository

  @SpykBean
  lateinit var realCas2v2StatusUpdateDetailRepository: Cas2v2StatusUpdateDetailRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realCas2v2StatusUpdateRepository)
    clearMocks(realCas2v2StatusUpdateDetailRepository)
  }

  @Nested
  inner class ControlsOnInternalUsers {
    @Test
    fun `creating a cas2v2 status update is forbidden to internal users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_POM"),
      )

      webTestClient.post()
        .uri("/cas2v2/assessments/de6512fc-a225-4109-b2cd-86c6307a5237/status-updates")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `creating a cas2v2 status update without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas2v2/assessments/de6512fc-a225-4109-b2cd-86c6307a5237/status-updates")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class PostToCreate {
    @Test
    fun `Create cas2v2 status update returns 201 and creates StatusUpdate when given status is valid`() {
      val assessmentId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

      givenACas2v2Assessor { _, jwt ->
        givenACas2v2PomUser { applicant, _ ->
          val application = cas2v2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(applicant)
            withSubmittedAt(OffsetDateTime.now())
          }

          cas2v2AssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withId(assessmentId)
          }

          assertThat(realCas2v2StatusUpdateRepository.count()).isEqualTo(0)
          assertThat(realCas2v2StatusUpdateDetailRepository.count()).isEqualTo(0)

          webTestClient.post()
            .uri("/cas2v2/assessments/$assessmentId/status-updates")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas2AssessmentStatusUpdate(newStatus = "moreInfoRequested"),
            )
            .exchange()
            .expectStatus()
            .isCreated

          assertThat(realCas2v2StatusUpdateRepository.count()).isEqualTo(1)

          val persistedStatusUpdate = realCas2v2StatusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id)
          assertThat(persistedStatusUpdate!!.assessment!!.id).isEqualTo(assessmentId)

          val appliedStatus = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2)
            .find { status ->
              status.id == persistedStatusUpdate.statusId
            }
          assertThat(appliedStatus!!.name).isEqualTo("moreInfoRequested")

          // verify that generated 'application.status-updated' domain event links
          // to the CAS2 domain
          val expectedFrontEndUrl = applicationUrlTemplate.replace("#id", application.id.toString())
          val persistedDomainEvent = domainEventRepository.findFirstByOrderByCreatedAtDesc()
          val domainEventFromJson = objectMapper.readValue(
            persistedDomainEvent!!.data,
            Cas2ApplicationStatusUpdatedEvent::class.java,
          )
          assertThat(domainEventFromJson.eventDetails.applicationUrl)
            .isEqualTo(expectedFrontEndUrl)
        }
      }
    }

    @Test
    fun `Create cas2v2 status update returns 404 when assessment not found`() {
      givenACas2v2Assessor { _, jwt ->
        webTestClient.post()
          .uri("/cas2v2/assessments/66f7127a-fe03-4b66-8378-5c0b048490f8/status-updates")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas2AssessmentStatusUpdate(newStatus = "moreInfoRequested"),
          )
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Create cas2v2 status update returns 400 when new status NOT valid`() {
      givenACas2v2Assessor { _, jwt ->
        givenACas2v2PomUser { applicant, _ ->
          val application = cas2v2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(applicant)
            withSubmittedAt(OffsetDateTime.now())
          }

          val assessment = cas2v2AssessmentEntityFactory.produceAndPersist {
            withApplication(application)
          }

          webTestClient.post()
            .uri("/cas2v2/assessments/${assessment.id}/status-updates")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas2AssessmentStatusUpdate(newStatus = "invalidStatus"),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.detail").isEqualTo("The status invalidStatus is not valid")
        }
      }
    }

    @Nested
    inner class WithStatusDetail {
      @Test
      fun `Create cas2v2 status update returns 201 and creates StatusUpdate when given status and detail are valid, and sends an email to the referrer`() {
        val assessmentId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
        val submittedAt = OffsetDateTime.now()

        try {
          givenACas2v2Assessor { _, jwt ->
            givenACas2v2PomUser { applicant, _ ->
              val application = cas2v2ApplicationEntityFactory.produceAndPersist {
                withCreatedByUser(applicant)
                withSubmittedAt(submittedAt)
                withNomsNumber("123NOMS")
              }

              cas2v2AssessmentEntityFactory.produceAndPersist {
                withId(assessmentId)
                withApplication(application)
              }

              assertThat(realCas2v2StatusUpdateRepository.count()).isEqualTo(0)

              val now = OffsetDateTime.now()
              mockkStatic(OffsetDateTime::class)
              every { OffsetDateTime.now() } returns now

              webTestClient.post()
                .uri("/cas2v2/assessments/$assessmentId/status-updates")
                .header("Authorization", "Bearer $jwt")
                .bodyValue(
                  Cas2AssessmentStatusUpdate(
                    newStatus = "offerDeclined",
                    newStatusDetails = listOf("changeOfCircumstances"),
                  ),
                )
                .exchange()
                .expectStatus()
                .isCreated

              assertThat(realCas2v2StatusUpdateRepository.count()).isEqualTo(1)
              assertThat(realCas2v2StatusUpdateDetailRepository.count()).isEqualTo(1)

              val persistedStatusUpdate =
                realCas2v2StatusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id)
              assertThat(persistedStatusUpdate!!.assessment!!.id).isEqualTo(assessmentId)

              val persistedStatusDetailUpdate =
                realCas2v2StatusUpdateDetailRepository.findFirstByStatusUpdateIdOrderByCreatedAtDesc(persistedStatusUpdate.id)
              assertThat(persistedStatusDetailUpdate).isNotNull

              val appliedStatus = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2)
                .find { status ->
                  status.id == persistedStatusUpdate.statusId
                }

              assertThat(appliedStatus!!.name).isEqualTo("offerDeclined")
              assertThat(appliedStatus.statusDetails?.find { detail -> detail.id == persistedStatusDetailUpdate?.statusDetailId })
                .isNotNull()

              emailAsserter.assertEmailsRequestedCount(1)

              emailAsserter.assertEmailRequested(
                toEmailAddress = applicant.email!!,
                templateId = "ef4dc5e3-b1f1-4448-a545-7a936c50fc3a",
                personalisation = mapOf(
                  "applicationStatus" to "Offer declined or withdrawn",
                  "dateStatusChanged" to now.toLocalDate().toCas2UiFormat(),
                  "timeStatusChanged" to now.toCas2UiFormattedHourOfDay(),
                  "nomsNumber" to "123NOMS",
                  "applicationType" to "Home Detention Curfew (HDC)",
                  "applicationUrl" to applicationOverviewUrlTemplate.replace("#id", application.id.toString()),
                ),
                replyToEmailId = notifyConfig.emailAddresses.cas2ReplyToId,
              )

              // verify that generated 'application.status-updated' domain event links
              // to the CAS2 domain
              val expectedFrontEndUrl = applicationUrlTemplate.replace("#id", application.id.toString())
              val persistedDomainEvent = domainEventRepository.findFirstByOrderByCreatedAtDesc()

              val domainEventFromJson = objectMapper.readValue(
                persistedDomainEvent!!.data,
                Cas2ApplicationStatusUpdatedEvent::class.java,
              )
              assertThat(domainEventFromJson.eventDetails.applicationUrl)
                .isEqualTo(expectedFrontEndUrl)

              // verify that the persisted domain event contains the expected status details
              val expected = listOf(Cas2StatusDetail("changeOfCircumstances", "Change of circumstances"))
              assertThat(domainEventFromJson.eventDetails.newStatus.statusDetails).isEqualTo(expected)
            }
          }
        } finally {
          unmockkAll()
        }
      }
    }
  }
}

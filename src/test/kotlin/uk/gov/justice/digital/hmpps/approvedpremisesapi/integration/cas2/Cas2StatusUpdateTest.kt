package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

class Cas2StatusUpdateTest(
  @Value("\${url-templates.frontend.cas2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationOverviewUrlTemplate: String,
) : IntegrationTestBase() {

  @SpykBean
  lateinit var realStatusUpdateRepository: Cas2StatusUpdateRepository

  @SpykBean
  lateinit var realStatusUpdateDetailRepository: Cas2StatusUpdateDetailRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realStatusUpdateRepository)
    clearMocks(realStatusUpdateDetailRepository)
  }

  @Nested
  inner class ControlsOnInternalUsers {
    @Test
    fun `creating a status update is forbidden to internal users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_POM"),
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
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

      givenACas2Assessor { _, jwt ->
        givenACas2PomUser { applicant, _ ->
          val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
          val application = cas2ApplicationEntityFactory.produceAndPersist {
            withId(applicationId)
            withCreatedByUser(applicant)
            withApplicationSchema(jsonSchema)
            withSubmittedAt(OffsetDateTime.now())
          }

          val assessment = cas2AssessmentEntityFactory.produceAndPersist {
            withApplication(application)
          }

          Assertions.assertThat(realStatusUpdateRepository.count()).isEqualTo(0)
          Assertions.assertThat(realStatusUpdateDetailRepository.count()).isEqualTo(0)

          webTestClient.post()
            .uri("/cas2/submissions/${application.id}/status-updates")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              Cas2AssessmentStatusUpdate(newStatus = "moreInfoRequested"),
            )
            .exchange()
            .expectStatus()
            .isCreated

          Assertions.assertThat(realStatusUpdateRepository.count()).isEqualTo(1)

          val persistedStatusUpdate = realStatusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id)
          Assertions.assertThat(persistedStatusUpdate!!.assessment!!.id).isEqualTo(assessment.id)

          val appliedStatus = Cas2ApplicationStatusSeeding.statusList()
            .find { status -> status.id == persistedStatusUpdate.statusId }

          Assertions.assertThat(appliedStatus!!.name).isEqualTo("moreInfoRequested")
        }

        // verify that generated 'application.status-updated' domain event links
        // to the CAS2 domain
        val expectedFrontEndUrl = applicationUrlTemplate.replace("#id", applicationId.toString())
        val persistedDomainEvent = domainEventRepository.findFirstByOrderByCreatedAtDesc()
        val domainEventFromJson = objectMapper.readValue(
          persistedDomainEvent!!.data,
          Cas2ApplicationStatusUpdatedEvent::class.java,
        )
        Assertions.assertThat(domainEventFromJson.eventDetails.applicationUrl)
          .isEqualTo(expectedFrontEndUrl)
      }
    }

    @Test
    fun `Create status update returns 404 when application not found`() {
      givenACas2Assessor { _, jwt ->
        webTestClient.post()
          .uri("/cas2/submissions/66f7127a-fe03-4b66-8378-5c0b048490f8/status-updates")
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
    fun `Create status update returns 400 when new status NOT valid`() {
      givenACas2Assessor { _, jwt ->
        givenACas2PomUser { applicant, _ ->
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
      fun `Create status update returns 201 and creates StatusUpdate when given status and detail are valid, and sends an email to the referrer`() {
        val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
        val submittedAt = OffsetDateTime.now()

        givenACas2Assessor { _, jwt ->
          givenACas2PomUser { applicant, _ ->
            val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
            val application = cas2ApplicationEntityFactory.produceAndPersist {
              withId(applicationId)
              withCreatedByUser(applicant)
              withApplicationSchema(jsonSchema)
              withSubmittedAt(submittedAt)
              withNomsNumber("123NOMS")
            }

            val assessment = cas2AssessmentEntityFactory.produceAndPersist {
              withApplication(application)
            }

            Assertions.assertThat(realStatusUpdateRepository.count()).isEqualTo(0)

            webTestClient.post()
              .uri("/cas2/submissions/${application.id}/status-updates")
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

            Assertions.assertThat(realStatusUpdateRepository.count()).isEqualTo(1)
            Assertions.assertThat(realStatusUpdateDetailRepository.count()).isEqualTo(1)

            val persistedStatusUpdate =
              realStatusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id)
            Assertions.assertThat(persistedStatusUpdate!!.assessment!!.id).isEqualTo(assessment.id)

            val persistedStatusDetailUpdate =
              realStatusUpdateDetailRepository.findFirstByStatusUpdateIdOrderByCreatedAtDesc(persistedStatusUpdate!!.id)
            Assertions.assertThat(persistedStatusDetailUpdate).isNotNull

            val appliedStatus = Cas2ApplicationStatusSeeding.statusList()
              .find { status ->
                status.id == persistedStatusUpdate.statusId
              }

            Assertions.assertThat(appliedStatus!!.name).isEqualTo("offerDeclined")
            Assertions.assertThat(appliedStatus.statusDetails?.find { detail -> detail.id == persistedStatusDetailUpdate?.statusDetailId })
              .isNotNull()

            emailAsserter.assertEmailsRequestedCount(1)
            emailAsserter.assertEmailRequested(
              toEmailAddress = applicant.email!!,
              templateId = "ef4dc5e3-b1f1-4448-a545-7a936c50fc3a",
              personalisation = mapOf(
                "applicationStatus" to "Offer declined or withdrawn",
                // This needs to be something else
                "dateStatusChanged" to submittedAt.toLocalDate().toCas2UiFormat(),
                "timeStatusChanged" to submittedAt.toCas2UiFormattedHourOfDay(),
                "nomsNumber" to "123NOMS",
                "applicationType" to "Home Detention Curfew (HDC)",
                "applicationUrl" to applicationOverviewUrlTemplate.replace("#id", application.id.toString()),
              ),
              replyToEmailId = notifyConfig.emailAddresses.cas2ReplyToId,
            )
          }
        }

        // verify that generated 'application.status-updated' domain event links
        // to the CAS2 domain
        val expectedFrontEndUrl = applicationUrlTemplate.replace("#id", applicationId.toString())
        val persistedDomainEvent = domainEventRepository.findFirstByOrderByCreatedAtDesc()
        val domainEventFromJson = objectMapper.readValue(
          persistedDomainEvent!!.data,
          Cas2ApplicationStatusUpdatedEvent::class.java,
        )
        Assertions.assertThat(domainEventFromJson.eventDetails.applicationUrl)
          .isEqualTo(expectedFrontEndUrl)

        // verify that the persisted domain event contains the expected status details
        val expected = listOf(Cas2StatusDetail("changeOfCircumstances", "Change of circumstances"))
        Assertions.assertThat(domainEventFromJson.eventDetails.newStatus.statusDetails).isEqualTo(expected)
      }
    }
  }

  @Nested
  inner class OnAssessments {
    @Nested
    inner class ControlsOnInternalUsers {
      @Test
      fun `creating a status update is forbidden to internal users based on role`() {
        val jwt = jwtAuthHelper.createClientCredentialsJwt(
          username = "username",
          authSource = "nomis",
          roles = listOf("ROLE_POM"),
        )

        webTestClient.post()
          .uri("/cas2/assessments/de6512fc-a225-4109-bdcd-86c6307a5237/status-updates")
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
          .uri("/cas2/assessments/de6512fc-a225-4109-bdcd-86c6307a5237/status-updates")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }

    @Nested
    inner class PostToCreate {
      @Test
      fun `Create status update returns 201 and creates StatusUpdate when given status is valid`() {
        val assessmentId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

        givenACas2Assessor { _, jwt ->
          givenACas2PomUser { applicant, _ ->
            val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
            val application = cas2ApplicationEntityFactory.produceAndPersist {
              withCreatedByUser(applicant)
              withApplicationSchema(jsonSchema)
              withSubmittedAt(OffsetDateTime.now())
            }

            cas2AssessmentEntityFactory.produceAndPersist {
              withApplication(application)
              withId(assessmentId)
            }

            Assertions.assertThat(realStatusUpdateRepository.count()).isEqualTo(0)
            Assertions.assertThat(realStatusUpdateDetailRepository.count()).isEqualTo(0)

            webTestClient.post()
              .uri("/cas2/assessments/$assessmentId/status-updates")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                Cas2AssessmentStatusUpdate(newStatus = "moreInfoRequested"),
              )
              .exchange()
              .expectStatus()
              .isCreated

            Assertions.assertThat(realStatusUpdateRepository.count()).isEqualTo(1)

            val persistedStatusUpdate = realStatusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id)
            Assertions.assertThat(persistedStatusUpdate!!.assessment!!.id).isEqualTo(assessmentId)

            val appliedStatus = Cas2ApplicationStatusSeeding.statusList()
              .find { status ->
                status.id == persistedStatusUpdate.statusId
              }
            Assertions.assertThat(appliedStatus!!.name).isEqualTo("moreInfoRequested")

            // verify that generated 'application.status-updated' domain event links
            // to the CAS2 domain
            val expectedFrontEndUrl = applicationUrlTemplate.replace("#id", application.id.toString())
            val persistedDomainEvent = domainEventRepository.findFirstByOrderByCreatedAtDesc()
            val domainEventFromJson = objectMapper.readValue(
              persistedDomainEvent!!.data,
              Cas2ApplicationStatusUpdatedEvent::class.java,
            )
            Assertions.assertThat(domainEventFromJson.eventDetails.applicationUrl)
              .isEqualTo(expectedFrontEndUrl)
          }
        }
      }

      @Test
      fun `Create status update returns 404 when assessment not found`() {
        givenACas2Assessor { _, jwt ->
          webTestClient.post()
            .uri("/cas2/assessments/66f7127a-fe03-4b66-8378-5c0b048490f8/status-updates")
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
      fun `Create status update returns 400 when new status NOT valid`() {
        givenACas2Assessor { _, jwt ->
          givenACas2PomUser { applicant, _ ->
            val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
            val application = cas2ApplicationEntityFactory.produceAndPersist {
              withCreatedByUser(applicant)
              withApplicationSchema(jsonSchema)
              withSubmittedAt(OffsetDateTime.now())
            }

            val assessmemt = cas2AssessmentEntityFactory.produceAndPersist {
              withApplication(application)
            }

            webTestClient.post()
              .uri("/cas2/assessments/${assessmemt.id}/status-updates")
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
        fun `Create status update returns 201 and creates StatusUpdate when given status and detail are valid, and sends an email to the referrer`() {
          val assessmentId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")
          val submittedAt = OffsetDateTime.now()

          givenACas2Assessor { _, jwt ->
            givenACas2PomUser { applicant, _ ->
              val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
              val application = cas2ApplicationEntityFactory.produceAndPersist {
                withCreatedByUser(applicant)
                withApplicationSchema(jsonSchema)
                withSubmittedAt(submittedAt)
                withNomsNumber("123NOMS")
              }

              val assessment = cas2AssessmentEntityFactory.produceAndPersist {
                withId(assessmentId)
                withApplication(application)
              }

              Assertions.assertThat(realStatusUpdateRepository.count()).isEqualTo(0)

              webTestClient.post()
                .uri("/cas2/assessments/$assessmentId/status-updates")
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

              Assertions.assertThat(realStatusUpdateRepository.count()).isEqualTo(1)
              Assertions.assertThat(realStatusUpdateDetailRepository.count()).isEqualTo(1)

              val persistedStatusUpdate =
                realStatusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id)
              Assertions.assertThat(persistedStatusUpdate!!.assessment!!.id).isEqualTo(assessmentId)

              val persistedStatusDetailUpdate =
                realStatusUpdateDetailRepository.findFirstByStatusUpdateIdOrderByCreatedAtDesc(persistedStatusUpdate!!.id)
              Assertions.assertThat(persistedStatusDetailUpdate).isNotNull

              val appliedStatus = Cas2ApplicationStatusSeeding.statusList()
                .find { status ->
                  status.id == persistedStatusUpdate.statusId
                }

              Assertions.assertThat(appliedStatus!!.name).isEqualTo("offerDeclined")
              Assertions.assertThat(appliedStatus.statusDetails?.find { detail -> detail.id == persistedStatusDetailUpdate?.statusDetailId })
                .isNotNull()

              emailAsserter.assertEmailsRequestedCount(1)
              emailAsserter.assertEmailRequested(
                toEmailAddress = applicant.email!!,
                templateId = "ef4dc5e3-b1f1-4448-a545-7a936c50fc3a",
                personalisation = mapOf(
                  "applicationStatus" to "Offer declined or withdrawn",
                  // This needs to be something else
                  "dateStatusChanged" to submittedAt.toLocalDate().toCas2UiFormat(),
                  "timeStatusChanged" to submittedAt.toCas2UiFormattedHourOfDay(),
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
              Assertions.assertThat(domainEventFromJson.eventDetails.applicationUrl)
                .isEqualTo(expectedFrontEndUrl)

              // verify that the persisted domain event contains the expected status details
              val expected = listOf(Cas2StatusDetail("changeOfCircumstances", "Change of circumstances"))
              Assertions.assertThat(domainEventFromJson.eventDetails.newStatus.statusDetails).isEqualTo(expected)
            }
          }
        }
      }
    }
  }
}

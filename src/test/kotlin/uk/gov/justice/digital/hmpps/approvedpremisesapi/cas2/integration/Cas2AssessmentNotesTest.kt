package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import com.fasterxml.jackson.core.type.TypeReference
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2AssessmentNotesTest(
  @Value("\${url-templates.frontend.cas2v2.application-overview}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.cas2v2.submitted-application-overview}") private val assessmentUrlTemplate: UrlTemplate,
) : IntegrationTestBase() {

  @AfterEach
  fun afterEach() {
    cas2NoteRepository.deleteAll()
    mockFeatureFlagService.reset()
  }

  @Test
  fun `creating a note without JWT returns 401`() {
    webTestClient.post()
      .uri("/cas2/assessments/${UUID.randomUUID()}/notes")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `creating a note is forbidden to external users who are not Assessors`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "auth",
      roles = listOf("ROLE_CAS2_ADMIN"),
    )

    webTestClient.post()
      .uri("/cas2/assessments/${UUID.randomUUID()}/notes")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Nested
  inner class ExternalUser {
    @Test
    fun `when external user adds note then application creator is notified by email`() {
      givenACas2v2PomUser { referrer, _ ->
        givenACas2v2Assessor { _, jwt ->
          val application = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(referrer)
            withSubmittedAt(OffsetDateTime.now())
            withCrn("CRN123")
            withApplicationOrigin(ApplicationOrigin.prisonBail)
            withServiceOrigin(Cas2ServiceOrigin.BAIL)
            withCohort(Cas2Cohort.PRISON_BAIL)
          }

          val assessment = cas2AssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withNacroReferralId("OH789")
            withServiceOrigin(Cas2ServiceOrigin.BAIL)
          }

          val rawResponseBody = webTestClient.post()
            .uri("/cas2/assessments/${assessment.id}/notes")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2v2.value)
            .bodyValue(NewCas2ApplicationNote(note = "Assessor note"))
            .exchange()
            .expectStatus()
            .isCreated()
            .returnResult<String>()
            .responseBody
            .blockFirst()

          jsonMapper.readValue(rawResponseBody, object : TypeReference<Cas2ApplicationNote>() {})

          emailAsserter.assertEmailRequested(
            toEmailAddress = referrer.email!!,
            templateId = "0919a001-7b83-44c0-b0d0-a617d84012bb", // CAS2_BAIL_APPLICATION_ASSESSOR_NOTE_ADDED
            personalisation = mapOf(
              "cohort" to "Prison Bail",
              "crn" to "CRN123",
              "nacroReferenceId" to "OH789",
              "viewSubmittedApplicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
            ),
          )
        }
      }
    }
  }

  @Nested
  inner class InternalUser {
    @Test
    fun `when internal user adds note then assessors are notified by email`() {
      givenACas2v2PomUser { referrer, jwt ->
        val application = cas2ApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(referrer)
          withSubmittedAt(OffsetDateTime.now())
          withCrn("CRN123")
          withApplicationOrigin(ApplicationOrigin.prisonBail)
          withServiceOrigin(Cas2ServiceOrigin.BAIL)
          withCohort(Cas2Cohort.PRISON_BAIL)
        }

        application.createApplicationAssignment(referrer.activeNomisCaseloadId!!, referrer)
        cas2ApplicationRepository.save(application)

        val assessment = cas2AssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withNacroReferralId("OH789")
          withServiceOrigin(Cas2ServiceOrigin.BAIL)
        }

        val rawResponseBody = webTestClient.post()
          .uri("/cas2/assessments/${assessment.id}/notes")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.cas2v2.value)
          .bodyValue(NewCas2ApplicationNote(note = "Referrer note"))
          .exchange()
          .expectStatus()
          .isCreated()
          .returnResult<String>()
          .responseBody
          .blockFirst()

        jsonMapper.readValue(rawResponseBody, object : TypeReference<Cas2ApplicationNote>() {})

        emailAsserter.assertEmailRequested(
          toEmailAddress = "assessors@example.com",
          templateId = "71fd8007-87a0-49b8-9099-7919b6ca4716", // CAS2_BAIL_APPLICATION_REFERRER_NOTE_ADDED
          personalisation = mapOf(
            "cohort" to "Prison Bail",
            "crn" to "CRN123",
            "nacroReferenceId" to "OH789",
            "nacroReferenceIdInSubject" to "(OH789)",
            "viewSubmittedApplicationUrl" to assessmentUrlTemplate.resolve("applicationId", application.id.toString()),
          ),
        )
      }
    }
  }

  @Test
  fun `returns 403 Forbidden when user is not authorized to add a note`() {
    givenACas2v2PomUser { _, jwt ->
      val otherUser = cas2UserEntityFactory.produceAndPersist {
        withActiveNomisCaseloadId("another-prison")
      }

      val application = cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(otherUser)
        withSubmittedAt(OffsetDateTime.now())
        withReferringPrisonCode("another-prison")
        withApplicationOrigin(ApplicationOrigin.homeDetentionCurfew)
        withServiceOrigin(Cas2ServiceOrigin.BAIL)
      }

      val assessment = cas2AssessmentEntityFactory.produceAndPersist {
        withApplication(application)
        withServiceOrigin(Cas2ServiceOrigin.BAIL)
      }

      // The POM user (referrer) from givenACas2v2PomUser is NOT the creator
      // and is in a different prison, and the application is NOT prisonBail origin.
      // So userCanAddNote should be false for a NOMIS user.

      webTestClient.post()
        .uri("/cas2/assessments/${assessment.id}/notes")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.cas2v2.value)
        .bodyValue(NewCas2ApplicationNote(note = "Unauthorised note"))
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }
}

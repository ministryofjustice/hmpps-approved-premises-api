package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.sentry.Sentry
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationNoteServiceTest {
  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockAssessmentRepository = mockk<Cas2AssessmentRepository>()
  private val mockApplicationNoteRepository = mockk<Cas2ApplicationNoteRepository>()
  private val mockUserService = mockk<NomisUserService>()
  private val mockExternalUserService = mockk<ExternalUserService>()
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockEmailNotificationService = mockk<EmailNotificationService>()
  private val mockUserAccessService = mockk<Cas2UserAccessService>()
  private val mockNotifyConfig = mockk<NotifyConfig>()

  private val assessmentNoteService = uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2ApplicationNoteService(
    mockApplicationRepository,
    mockAssessmentRepository,
    mockApplicationNoteRepository,
    mockUserService,
    mockExternalUserService,
    mockHttpAuthService,
    mockEmailNotificationService,
    mockUserAccessService,
    mockNotifyConfig,
    "http://frontend/applications/#id/overview",
    "http://frontend/assess/applications/#applicationId/overview",
  )

  @Nested
  inner class CreateAssessmentNote {

    private val referrer = NomisUserEntityFactory().withActiveCaseloadId("my-prison").produce()

    @Nested
    inner class WhenApplicationIsNotFound {
      @Test
      fun `returns Not Found`() {
        every { mockAssessmentRepository.findByIdOrNull(any()) } returns Cas2AssessmentEntityFactory().produce()
        every { mockApplicationRepository.findByIdOrNull(any()) } returns null

        Assertions.assertThat(
          assessmentNoteService.createAssessmentNote(
            assessmentId = UUID.randomUUID(),
            note = NewCas2ApplicationNote(note = "note for missing app"),
          ) is AuthorisableActionResult.NotFound,
        ).isTrue
      }
    }

    @Nested
    inner class WhenAssessmentIsNotFound {
      @Test
      fun `returns Not Found`() {
        every { mockAssessmentRepository.findByIdOrNull(any()) } returns null

        Assertions.assertThat(
          assessmentNoteService.createAssessmentNote(
            assessmentId = UUID.randomUUID(),
            note = NewCas2ApplicationNote(note = "note for missing app"),
          ) is AuthorisableActionResult.NotFound,
        ).isTrue
      }
    }

    @Nested
    inner class WhenApplicationIsNotSubmitted {
      @Test
      fun `returns Validation error when application is not submitted`() {
        val assessment = Cas2AssessmentEntityFactory().produce()

        val applicationNotSubmitted = Cas2ApplicationEntityFactory()
          .withCreatedByUser(referrer)
          .withCrn("CRN123")
          .withNomsNumber("NOMSABC")
          .withAssessment(assessment)
          .produce()

        every { mockAssessmentRepository.findByIdOrNull(any()) } returns assessment
        every { mockApplicationRepository.findByIdOrNull(any()) } returns applicationNotSubmitted

        every { mockUserService.getUserForRequest() } returns referrer

        val result = assessmentNoteService.createAssessmentNote(
          assessmentId = applicationNotSubmitted.id,
          note = NewCas2ApplicationNote(note = "note for in progress app"),
        )
        Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
        result as AuthorisableActionResult.Success

        Assertions.assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
        val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

        Assertions.assertThat(validatableActionResult.message).isEqualTo("This application has not been submitted")
      }
    }

    @Nested
    inner class AsNomisUser {

      @BeforeEach
      fun setup() {
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getCas2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.isExternalUser() } returns false
        every { mockNotifyConfig.emailAddresses.cas2Assessors } returns "assessors@example.com"
        every { mockNotifyConfig.templates.cas2NoteAddedForAssessor } returns "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1"
        every { mockUserService.getUserForRequest() } returns referrer
        every { mockApplicationNoteRepository.save(any()) } answers
          {
            noteEntity
          }
      }

      private val assessment = Cas2AssessmentEntityFactory()
        .withAssessorName("Anne Assessor")
        .withNacroReferralId("OH123").produce()
      private val submittedApplication = Cas2ApplicationEntityFactory()
        .withId(assessment.application.id)
        .withCreatedByUser(referrer)
        .withCrn("CRN123")
        .withNomsNumber("NOMSABC")
        .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
        .withAssessment(assessment)
        .produce()
      private val applicationId = submittedApplication.id
      private val noteEntity = Cas2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdByUser = referrer,
        application = submittedApplication,
        body = "new note",
        createdAt = OffsetDateTime.now().randomDateTimeBefore(1),
        assessment = assessment,
      )

      @Nested
      inner class WhenApplicationCreatedByUser {
        @Test
        fun `returns Success result with entity from db`() {
          every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
          every { mockApplicationRepository.findByIdOrNull(applicationId) } returns submittedApplication

          every {
            mockEmailNotificationService.sendCas2Email(
              recipientEmailAddress = "assessors@example.com",
              templateId = "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1",
              personalisation = mapOf(
                "nacroReferenceId" to "OH123",
                "nacroReferenceIdInSubject" to "(OH123)",
                "dateNoteAdded" to noteEntity.createdAt.toLocalDate().toCas2UiFormat(),
                "timeNoteAdded" to noteEntity.createdAt.toCas2UiFormattedHourOfDay(),
                "assessorName" to "Anne Assessor",
                "applicationType" to "Home Detention Curfew (HDC)",
                "applicationUrl" to "http://frontend/assess/applications/$applicationId/overview",
              ),
            )
          } just Runs

          val result = assessmentNoteService.createAssessmentNote(
            assessmentId = assessment!!.id,
            NewCas2ApplicationNote(note = "new note"),
          )

          verify(exactly = 1) { mockApplicationNoteRepository.save(any()) }

          Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
          result as AuthorisableActionResult.Success

          Assertions.assertThat(result.entity is ValidatableActionResult.Success).isTrue
          val validatableActionResult = result.entity as ValidatableActionResult.Success

          val createdNote = validatableActionResult.entity

          Assertions.assertThat(createdNote).isEqualTo(noteEntity)

          verify(exactly = 1) { mockEmailNotificationService.sendCas2Email(any(), any(), any()) }
        }

        @Nested
        inner class WhenThereAreNoAssessorDetails {
          @Test
          fun `passes placeholder copy to email template`() {
            val assessment = Cas2AssessmentEntityFactory().produce()
            val submittedApplicationWithoutAssessorDetails = Cas2ApplicationEntityFactory()
              .withId(assessment.application.id)
              .withCreatedByUser(referrer)
              .withAssessment(assessment)
              .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
              .produce()

            every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
            every { mockApplicationRepository.findByIdOrNull(submittedApplicationWithoutAssessorDetails.id) } returns submittedApplicationWithoutAssessorDetails

            every {
              mockEmailNotificationService.sendCas2Email(
                recipientEmailAddress = "assessors@example.com",
                templateId = "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1",
                personalisation = mapOf(
                  "nacroReferenceId" to "Unknown. " +
                    "The Nacro CAS-2 reference number has not been added to the application yet.",
                  "nacroReferenceIdInSubject" to "",
                  "dateNoteAdded" to noteEntity.createdAt.toLocalDate().toCas2UiFormat(),
                  "timeNoteAdded" to noteEntity.createdAt.toCas2UiFormattedHourOfDay(),
                  "assessorName" to "Unknown. " +
                    "The assessor has not added their name to the application yet.",
                  "applicationType" to "Home Detention Curfew (HDC)",
                  "applicationUrl" to "http://frontend/assess/applications/${submittedApplicationWithoutAssessorDetails.id}/overview",
                ),
              )
            } just Runs

            val result = assessmentNoteService.createAssessmentNote(
              assessmentId = assessment.id,
              NewCas2ApplicationNote(note = "new note"),
            )

            Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
            verify(exactly = 1) { mockEmailNotificationService.sendCas2Email(any(), any(), any()) }
          }
        }
      }

      @Nested
      inner class WhenApplicationCreatedByOtherUser {
        val assessment = Cas2AssessmentEntityFactory()
          .withAssessorName("Anne Prison Assessor").withNacroReferralId("OH456").produce()
        val applicationCreatedByOtherUser = Cas2ApplicationEntityFactory()
          .withId(assessment.application.id)
          .withCreatedByUser(NomisUserEntityFactory().produce())
          .withCrn("CRN123")
          .withNomsNumber("NOMSABC")
          .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
          .withReferringPrisonCode("other-prison")
          .withAssessment(assessment)
          .produce()

        @Nested
        inner class WhenDifferentPrison {
          @Test
          fun `returns Not Authorised`() {
            every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
            every { mockApplicationRepository.findByIdOrNull(applicationCreatedByOtherUser.id) } returns applicationCreatedByOtherUser
            every {
              mockUserAccessService.offenderIsFromSamePrisonAsUser("other-prison", "my-prison")
            } returns false

            Assertions.assertThat(
              assessmentNoteService.createAssessmentNote(
                assessmentId = assessment.id,
                note = NewCas2ApplicationNote(note = "note for unauthorised app"),
              ) is AuthorisableActionResult.Unauthorised,
            ).isTrue
          }
        }

        @Nested
        inner class WhenSamePrison {
          @Test
          fun `returns Success result with entity from db`() {
            every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
            every { mockApplicationRepository.findByIdOrNull(applicationCreatedByOtherUser.id) } returns applicationCreatedByOtherUser
            every {
              mockUserAccessService.offenderIsFromSamePrisonAsUser("other-prison", "my-prison")
            } returns true

            every {
              mockEmailNotificationService.sendCas2Email(
                recipientEmailAddress = "assessors@example.com",
                templateId = "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1",
                personalisation = mapOf(
                  "nacroReferenceId" to "OH456",
                  "nacroReferenceIdInSubject" to "(OH456)",
                  "dateNoteAdded" to noteEntity.createdAt.toLocalDate().toCas2UiFormat(),
                  "timeNoteAdded" to noteEntity.createdAt.toCas2UiFormattedHourOfDay(),
                  "assessorName" to "Anne Prison Assessor",
                  "applicationType" to "Home Detention Curfew (HDC)",
                  "applicationUrl" to "http://frontend/assess/applications/${applicationCreatedByOtherUser.id}/overview",
                ),
              )
            } just Runs

            val result = assessmentNoteService.createAssessmentNote(
              assessmentId = assessment.id,
              NewCas2ApplicationNote(note = "new note"),
            )

            verify(exactly = 1) { mockApplicationNoteRepository.save(any()) }

            Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
            result as AuthorisableActionResult.Success

            Assertions.assertThat(result.entity is ValidatableActionResult.Success).isTrue
            val validatableActionResult = result.entity as ValidatableActionResult.Success

            val createdNote = validatableActionResult.entity

            Assertions.assertThat(createdNote).isEqualTo(noteEntity)

            verify(exactly = 1) { mockEmailNotificationService.sendCas2Email(any(), any(), any()) }
          }
        }
      }
    }

    @Nested
    inner class AsExternalUser {
      private val externalUser = ExternalUserEntityFactory().produce()

      @BeforeEach
      fun setup() {
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getCas2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.isExternalUser() } returns true
        every { mockNotifyConfig.templates.cas2NoteAddedForReferrer } returns "abc123"
        every { mockNotifyConfig.emailAddresses.cas2Assessors } returns "assessors@example.com"
        every { mockApplicationNoteRepository.save(any()) } answers
          {
            noteEntity
          }
      }

      val assessment = Cas2AssessmentEntityFactory().produce()
      private val submittedApplication = Cas2ApplicationEntityFactory()
        .withId(assessment.application.id)
        .withCreatedByUser(referrer)
        .withCrn("CRN123")
        .withNomsNumber("NOMSABC")
        .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
        .withAssessment(assessment)
        .produce()
      private val applicationId = submittedApplication.id
      private val noteEntity = Cas2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdByUser = externalUser,
        application = submittedApplication,
        body = "new note",
        createdAt = OffsetDateTime.now().randomDateTimeBefore(1),
        assessment = assessment,
      )

      @Test
      fun `returns Success result with entity from db`() {
        every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
        every { mockApplicationRepository.findByIdOrNull(applicationId) } returns submittedApplication
        every { mockExternalUserService.getUserForRequest() } returns externalUser

        every {
          mockEmailNotificationService.sendCas2Email(
            recipientEmailAddress = referrer.email!!,
            templateId = "abc123",
            personalisation = mapOf(
              "dateNoteAdded" to noteEntity.createdAt.toLocalDate().toCas2UiFormat(),
              "timeNoteAdded" to noteEntity.createdAt.toCas2UiFormattedHourOfDay(),
              "nomsNumber" to "NOMSABC",
              "applicationType" to "Home Detention Curfew (HDC)",
              "applicationUrl" to "http://frontend/applications/$applicationId/overview",
            ),
          )
        } just Runs

        val result = assessmentNoteService.createAssessmentNote(
          assessmentId = assessment.id,
          NewCas2ApplicationNote(note = "new note"),
        )

        verify(exactly = 1) { mockApplicationNoteRepository.save(any()) }

        Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
        result as AuthorisableActionResult.Success

        Assertions.assertThat(result.entity is ValidatableActionResult.Success).isTrue
        val validatableActionResult = result.entity as ValidatableActionResult.Success

        val createdNote = validatableActionResult.entity

        Assertions.assertThat(createdNote).isEqualTo(noteEntity)
      }

      @Test
      fun `alerts Sentry when the Referrer does not have an email`() {
        val submittedApplicationWithNoReferrerEmail = Cas2ApplicationEntityFactory()
          .withId(assessment.application.id)
          .withCreatedByUser(NomisUserEntityFactory().withEmail(null).produce())
          .withCrn("CRN123")
          .withNomsNumber("NOMSABC")
          .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
          .withAssessment(assessment)
          .produce()

        every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
        every { mockApplicationRepository.findByIdOrNull(applicationId) } answers
          {
            submittedApplicationWithNoReferrerEmail
          }
        every { mockExternalUserService.getUserForRequest() } returns externalUser
        mockkStatic(Sentry::class)

        assessmentNoteService.createAssessmentNote(
          assessmentId = assessment.id,
          NewCas2ApplicationNote(note = "new note"),
        )

        verify(exactly = 1) {
          Sentry.captureMessage(
            any(),
          )
        }
      }
    }
  }
}

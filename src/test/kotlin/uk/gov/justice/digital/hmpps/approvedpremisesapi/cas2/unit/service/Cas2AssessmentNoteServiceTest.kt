package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2AssessmentNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2EmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

class Cas2AssessmentNoteServiceTest {
  private val mockAssessmentRepository = mockk<Cas2AssessmentRepository>()
  private val mockApplicationNoteRepository = mockk<Cas2ApplicationNoteRepository>()
  private val mockUserService = mockk<Cas2UserService>()
  private val mockExternalUserService = mockk<ExternalUserService>()
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockEmailNotificationService = mockk<EmailNotificationService>()
  private val mockUserAccessService = mockk<Cas2UserAccessService>()
  private val mockNotifyConfig = mockk<NotifyConfig>()
  private val cas2EmailService = mockk<Cas2EmailService>()

  private val assessmentNoteService = Cas2AssessmentNoteService(
    mockAssessmentRepository,
    mockApplicationNoteRepository,
    mockUserService,
    mockExternalUserService,
    mockHttpAuthService,
    mockEmailNotificationService,
    mockUserAccessService,
    mockNotifyConfig,
    cas2EmailService,
    "http://frontend/applications/#id/overview",
    "http://frontend/assess/applications/#applicationId/overview",
  )

  @Nested
  inner class CreateAssessmentNote {

    private val assessorName = "Anne Prison Assessor"
    private val assessorEmail = "assessors@example.com"
    private val myPrisonCode = "my-prison"
    private val cas2UserReferrer = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).withActiveNomisCaseloadId(myPrisonCode).produce()

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
    inner class AsNomisUser {
      @BeforeEach
      fun setup() {
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getCas2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.isExternalUser() } returns false
        every { mockNotifyConfig.emailAddresses.cas2Assessors } returns assessorEmail
        every { mockUserService.getUserForRequest() } returns cas2UserReferrer
      }

      @Nested
      inner class WhenApplicationCreatedByUser {
        @Test
        fun `returns Success result with entity from db`() {
          val baseAssessment = Cas2AssessmentEntityFactory()
            .withAssessorName(assessorName)
            .withNacroReferralId("OH123").produce()
          val submittedApplication = Cas2ApplicationEntityFactory()
            .withCreatedByUser(cas2UserReferrer)
            .withCrn("CRN123")
            .withNomsNumber("NOMSABC")
            .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
            .withAssessment(baseAssessment)
            .withApplicationAssignments(myPrisonCode, cas2UserReferrer)
            .produce()
          val assessment = Cas2AssessmentEntityFactory()
            .withId(baseAssessment.id)
            .withApplication(submittedApplication)
            .withAssessorName(baseAssessment.assessorName!!)
            .withNacroReferralId(baseAssessment.nacroReferralId!!).produce()
          val applicationId = submittedApplication.id
          val noteEntity = Cas2ApplicationNoteEntity(
            id = UUID.randomUUID(),
            createdByUser = cas2UserReferrer,
            application = submittedApplication,
            body = "new note",
            createdAt = OffsetDateTime.now().randomDateTimeBefore(1),
            assessment = assessment,
          )

          every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
          every { mockUserAccessService.offenderIsFromSamePrisonAsUser(myPrisonCode, myPrisonCode) } returns true
          every { mockApplicationNoteRepository.save(any()) } answers
            {
              noteEntity
            }
          every {
            mockEmailNotificationService.sendCas2Email(
              recipientEmailAddress = assessorEmail,
              templateId = Cas2NotifyTemplates.CAS2_NOTE_ADDED_FOR_ASSESSOR,
              personalisation = mapOf(
                "nacroReferenceId" to "OH123",
                "nacroReferenceIdInSubject" to "(OH123)",
                "dateNoteAdded" to noteEntity.createdAt.toLocalDate().toCas2UiFormat(),
                "timeNoteAdded" to noteEntity.createdAt.toCas2UiFormattedHourOfDay(),
                "assessorName" to assessorName,
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

        @Test
        fun `When There Are No Assessor Details passes placeholder copy to email template`() {
          val baseAssessment = Cas2AssessmentEntityFactory().produce()
          val submittedApplicationWithoutAssessorDetails = Cas2ApplicationEntityFactory()
            .withCreatedByUser(cas2UserReferrer)
            .withAssessment(baseAssessment)
            .withApplicationAssignments(myPrisonCode, cas2UserReferrer)
            .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
            .produce()
          val assessment = Cas2AssessmentEntityFactory()
            .withId(baseAssessment.id).withApplication(submittedApplicationWithoutAssessorDetails).produce()
          val noteEntity = Cas2ApplicationNoteEntity(
            id = UUID.randomUUID(),
            createdByUser = cas2UserReferrer,
            application = submittedApplicationWithoutAssessorDetails,
            body = "new note",
            createdAt = OffsetDateTime.now().randomDateTimeBefore(1),
            assessment = assessment,
          )
          every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
          every { mockUserAccessService.offenderIsFromSamePrisonAsUser(myPrisonCode, myPrisonCode) } returns true
          every { mockApplicationNoteRepository.save(any()) } answers
            {
              noteEntity
            }
          every {
            mockEmailNotificationService.sendCas2Email(
              recipientEmailAddress = assessorEmail,
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

      @Nested
      inner class WhenApplicationCreatedByOtherUserButTransferredToUsersPrison {
        @Test
        fun `returns Success result with entity from db`() {
          val oldPrisonCode = "old-prison"
          val oldPom = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS)
            .withActiveNomisCaseloadId(oldPrisonCode)
            .produce()
          val application = Cas2ApplicationEntityFactory()
            .withCreatedByUser(oldPom)
            .withSubmittedAt(OffsetDateTime.now())
            .produce()
          val firstApplicationAssignment = Cas2ApplicationAssignmentEntity(
            id = UUID.randomUUID(),
            createdAt = OffsetDateTime.now().minusDays(70),
            prisonCode = oldPrisonCode,
            allocatedPomUser = oldPom,
            application = application,
          )
          val locationApplicationAssignment = Cas2ApplicationAssignmentEntity(
            id = UUID.randomUUID(),
            createdAt = OffsetDateTime.now().minusDays(50),
            prisonCode = myPrisonCode,
            allocatedPomUser = null,
            application = application,
          )
          val allocationApplicationAssignment = Cas2ApplicationAssignmentEntity(
            id = UUID.randomUUID(),
            createdAt = OffsetDateTime.now().minusDays(20),
            prisonCode = myPrisonCode,
            allocatedPomUser = cas2UserReferrer,
            application = application,
          )

          val applicationAssignments = mutableListOf(
            firstApplicationAssignment,
            locationApplicationAssignment,
            allocationApplicationAssignment,
          )
          val baseAssessment = Cas2AssessmentEntityFactory()
            .withAssessorName(assessorName)
            .withNacroReferralId("OH123").produce()
          val applicationCreatedByOtherUser = Cas2ApplicationEntityFactory()
            .withCreatedByUser(oldPom)
            .withCrn("CRN123")
            .withNomsNumber("NOMSABC")
            .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
            .withReferringPrisonCode(oldPrisonCode)
            .withAssessment(baseAssessment)
            .withApplicationAssignments(applicationAssignments)
            .produce()
          val assessment = Cas2AssessmentEntityFactory()
            .withId(baseAssessment.id)
            .withApplication(applicationCreatedByOtherUser)
            .withAssessorName(baseAssessment.assessorName!!)
            .withNacroReferralId(baseAssessment.nacroReferralId!!).produce()
          val noteEntity = Cas2ApplicationNoteEntity(
            id = UUID.randomUUID(),
            createdByUser = cas2UserReferrer,
            application = applicationCreatedByOtherUser,
            body = "new note",
            createdAt = OffsetDateTime.now().randomDateTimeBefore(1),
            assessment = assessment,
          )
          every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
          every {
            mockUserAccessService.offenderIsFromSamePrisonAsUser(myPrisonCode, myPrisonCode)
          } returns true
          every {
            mockEmailNotificationService.sendCas2Email(
              recipientEmailAddress = assessorEmail,
              templateId = Cas2NotifyTemplates.CAS2_NOTE_ADDED_FOR_ASSESSOR,
              personalisation = mapOf(
                "nacroReferenceId" to "OH123",
                "nacroReferenceIdInSubject" to "(OH123)",
                "dateNoteAdded" to noteEntity.createdAt.toLocalDate().toCas2UiFormat(),
                "timeNoteAdded" to noteEntity.createdAt.toCas2UiFormattedHourOfDay(),
                "assessorName" to assessorName,
                "applicationType" to "Home Detention Curfew (HDC)",
                "applicationUrl" to "http://frontend/assess/applications/${applicationCreatedByOtherUser.id}/overview",
              ),
            )
          } just Runs
          every { mockApplicationNoteRepository.save(any()) } answers
            {
              noteEntity
            }
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
      }

      @Nested
      inner class WhenApplicationCreatedByOtherUser {
        val otherUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
        val prisonCode = "other-prison"

        private val baseApplication = Cas2ApplicationEntityFactory()
          .withCreatedByUser(otherUser)
          .withSubmittedAt(OffsetDateTime.now())
          .produce()
        private val firstApplicationAssignment = Cas2ApplicationAssignmentEntity(
          id = UUID.randomUUID(),
          createdAt = OffsetDateTime.now().minusDays(70),
          prisonCode = prisonCode,
          allocatedPomUser = otherUser,
          application = baseApplication,
        )
        private val baseAssessment = Cas2AssessmentEntityFactory()
          .withAssessorName(assessorName).withNacroReferralId("OH456").produce()
        private val applicationCreatedByOtherUser = Cas2ApplicationEntityFactory()
          .withCreatedByUser(Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce())
          .withCrn("CRN123")
          .withNomsNumber("NOMSABC")
          .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
          .withReferringPrisonCode(prisonCode)
          .withAssessment(baseAssessment)
          .withApplicationAssignments(
            mutableListOf(
              firstApplicationAssignment,
            ),
          )
          .produce()
        val assessment = Cas2AssessmentEntityFactory()
          .withId(baseAssessment.id)
          .withApplication(applicationCreatedByOtherUser)
          .withAssessorName(baseAssessment.assessorName!!)
          .withNacroReferralId(baseAssessment.nacroReferralId!!)
          .produce()

        @Test
        fun `When Different Prison returns Not Authorised`() {
          every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
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

        @Test
        fun `When Same Prison returns Success result with entity from db`() {
          val noteEntity = Cas2ApplicationNoteEntity(
            id = UUID.randomUUID(),
            createdByUser = cas2UserReferrer,
            application = applicationCreatedByOtherUser,
            body = "new note",
            createdAt = OffsetDateTime.now().randomDateTimeBefore(1),
            assessment = assessment,
          )
          every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
          every {
            mockUserAccessService.offenderIsFromSamePrisonAsUser("other-prison", "my-prison")
          } returns true
          every { mockApplicationNoteRepository.save(any()) } answers
            {
              noteEntity
            }
          every {
            mockEmailNotificationService.sendCas2Email(
              recipientEmailAddress = assessorEmail,
              templateId = "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1",
              personalisation = mapOf(
                "nacroReferenceId" to "OH456",
                "nacroReferenceIdInSubject" to "(OH456)",
                "dateNoteAdded" to noteEntity.createdAt.toLocalDate().toCas2UiFormat(),
                "timeNoteAdded" to noteEntity.createdAt.toCas2UiFormattedHourOfDay(),
                "assessorName" to assessorName,
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

    @Nested
    inner class AsExternalUser {
      private val externalUser = ExternalUserEntityFactory().produce()

      @BeforeEach
      fun setup() {
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getCas2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.isExternalUser() } returns true
        every { mockNotifyConfig.emailAddresses.cas2Assessors } returns assessorEmail
      }

      @Test
      fun `returns Success result with entity from db`() {
        val assessment = Cas2AssessmentEntityFactory().produce()
        val submittedApplication = Cas2ApplicationEntityFactory()
          .withId(assessment.application.id)
          .withCreatedByUser(cas2UserReferrer)
          .withCrn("CRN123")
          .withNomsNumber("NOMSABC")
          .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
          .withAssessment(assessment)
          .produce()
        val applicationId = submittedApplication.id
        val noteEntity = Cas2ApplicationNoteEntity(
          id = UUID.randomUUID(),
          createdByUser = externalUser,
          application = submittedApplication,
          body = "new note",
          createdAt = OffsetDateTime.now().randomDateTimeBefore(1),
          assessment = assessment,
        )
        every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
        every { mockExternalUserService.getUserForRequest() } returns externalUser
        every { cas2EmailService.getReferrerEmail(any()) } returns "email"
        every { mockApplicationNoteRepository.save(any()) } answers
          {
            noteEntity
          }
        every {
          mockEmailNotificationService.sendCas2Email(
            recipientEmailAddress = "email",
            templateId = Cas2NotifyTemplates.CAS2_NOTE_ADDED_FOR_REFERRER,
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
        val baseAssessment = Cas2AssessmentEntityFactory().produce()
        val submittedApplicationWithNoReferrerEmail = Cas2ApplicationEntityFactory()
          .withCreatedByUser(Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).withEmail(null).produce())
          .withCrn("CRN123")
          .withNomsNumber("NOMSABC")
          .withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(2))
          .withAssessment(baseAssessment)
          .produce()
        val assessment = Cas2AssessmentEntityFactory()
          .withId(submittedApplicationWithNoReferrerEmail.id)
          .produce()
        val noteEntity = Cas2ApplicationNoteEntity(
          id = UUID.randomUUID(),
          createdByUser = externalUser,
          application = submittedApplicationWithNoReferrerEmail,
          body = "new note",
          createdAt = OffsetDateTime.now().randomDateTimeBefore(1),
          assessment = assessment,
        )

        every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
        every { mockExternalUserService.getUserForRequest() } returns externalUser
        every { cas2EmailService.getReferrerEmail(any()) } answers { callOriginal() }
        every { mockApplicationNoteRepository.save(any()) } answers
          {
            noteEntity
          }
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

    @Nested
    inner class AsCas2User {
      private val submittedDate = OffsetDateTime.now().minusDays(2)
      private val createdAt = OffsetDateTime.now().minusDays(1)

      @BeforeEach
      fun setup() {
        val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
        every { mockHttpAuthService.getCas2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
        every { mockPrincipal.isExternalUser() } returns false
        every { mockNotifyConfig.emailAddresses.cas2Assessors } returns assessorEmail
        every { mockUserService.getUserForRequest() } returns cas2UserReferrer
      }

      @Nested
      inner class WhenApplicationCreatedByUser {
        @Test
        fun `returns Success result with entity from db`() {
          val baseAssessment = Cas2AssessmentEntityFactory()
            .withAssessorName(assessorName)
            .withNacroReferralId("OH123")
            .produce()
          val submittedApplication = Cas2ApplicationEntityFactory()
            .withCreatedByUser(cas2UserReferrer)
            .withCrn("CRN123")
            .withNomsNumber("NOMSABC")
            .withSubmittedAt(submittedDate)
            .withApplicationAssignments(myPrisonCode, cas2UserReferrer)
            .withAssessment(baseAssessment)
            .produce()
          val assessment = Cas2AssessmentEntityFactory()
            .withId(baseAssessment.id)
            .withAssessorName(baseAssessment.assessorName!!)
            .withNacroReferralId(baseAssessment.nacroReferralId!!)
            .withApplication(submittedApplication)
            .produce()

          val applicationId = submittedApplication.id
          val noteEntity = Cas2ApplicationNoteEntity(
            id = UUID.randomUUID(),
            createdByUser = cas2UserReferrer,
            application = submittedApplication,
            body = "new note",
            createdAt = createdAt,
            assessment = assessment,
          )
          every { mockAssessmentRepository.findByIdOrNull(assessment.id) } returns assessment
          every { mockUserAccessService.offenderIsFromSamePrisonAsUser(myPrisonCode, myPrisonCode) } returns true
          every { mockApplicationNoteRepository.save(any()) } answers
            {
              noteEntity
            }

          every {
            mockEmailNotificationService.sendCas2Email(
              recipientEmailAddress = assessorEmail,
              templateId = Cas2NotifyTemplates.CAS2_NOTE_ADDED_FOR_ASSESSOR,
              personalisation = mapOf(
                "nacroReferenceId" to "OH123",
                "nacroReferenceIdInSubject" to "(OH123)",
                "dateNoteAdded" to noteEntity.createdAt.toLocalDate().toCas2UiFormat(),
                "timeNoteAdded" to noteEntity.createdAt.toCas2UiFormattedHourOfDay(),
                "assessorName" to assessorName,
                "applicationType" to "Home Detention Curfew (HDC)",
                "applicationUrl" to "http://frontend/assess/applications/$applicationId/overview",
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

        @Test
        fun `When There Are No Assessor Details passes placeholder copy to email template`() {
          val base2Assessment = Cas2AssessmentEntityFactory().produce()
          val submittedApplicationWithoutAssessorDetails = Cas2ApplicationEntityFactory()
            .withCreatedByUser(cas2UserReferrer)
            .withAssessment(base2Assessment)
            .withSubmittedAt(submittedDate)
            .withApplicationAssignments(myPrisonCode, cas2UserReferrer)
            .produce()
          val assessmentWithoutAssessor = Cas2AssessmentEntityFactory()
            .withId(base2Assessment.id)
            .withApplication(submittedApplicationWithoutAssessorDetails)
            .produce()
          val noteEntityWithoutAssessor = Cas2ApplicationNoteEntity(
            id = UUID.randomUUID(),
            createdByUser = cas2UserReferrer,
            application = submittedApplicationWithoutAssessorDetails,
            body = "new note",
            createdAt = createdAt,
            assessment = assessmentWithoutAssessor,
          )

          every { mockAssessmentRepository.findByIdOrNull(assessmentWithoutAssessor.id) } returns assessmentWithoutAssessor
          every { mockUserAccessService.offenderIsFromSamePrisonAsUser(myPrisonCode, myPrisonCode) } returns true
          every { mockApplicationNoteRepository.save(any()) } answers
            {
              noteEntityWithoutAssessor
            }
          every {
            mockEmailNotificationService.sendCas2Email(
              recipientEmailAddress = assessorEmail,
              templateId = "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1",
              personalisation = mapOf(
                "nacroReferenceId" to "Unknown. " +
                  "The Nacro CAS-2 reference number has not been added to the application yet.",
                "nacroReferenceIdInSubject" to "",
                "dateNoteAdded" to noteEntityWithoutAssessor.createdAt.toLocalDate().toCas2UiFormat(),
                "timeNoteAdded" to noteEntityWithoutAssessor.createdAt.toCas2UiFormattedHourOfDay(),
                "assessorName" to "Unknown. " +
                  "The assessor has not added their name to the application yet.",
                "applicationType" to "Home Detention Curfew (HDC)",
                "applicationUrl" to "http://frontend/assess/applications/${submittedApplicationWithoutAssessorDetails.id}/overview",
              ),
            )
          } just Runs

          val result = assessmentNoteService.createAssessmentNote(
            assessmentId = assessmentWithoutAssessor.id,
            NewCas2ApplicationNote(note = "new note"),
          )

          Assertions.assertThat(result is AuthorisableActionResult.Success).isTrue
          verify(exactly = 1) { mockEmailNotificationService.sendCas2Email(any(), any(), any()) }
        }
      }
    }
  }
}

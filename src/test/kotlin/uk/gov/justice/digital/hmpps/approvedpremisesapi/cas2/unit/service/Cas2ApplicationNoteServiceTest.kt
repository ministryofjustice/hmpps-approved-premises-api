package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationNoteEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.EmailAddressConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationNoteServiceTest {
  private val mockCas2ApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockCas2AssessmentRepository = mockk<Cas2AssessmentRepository>()
  private val mockCas2ApplicationNoteRepository = mockk<Cas2ApplicationNoteRepository>()
  private val mockUserService = mockk<Cas2UserService>()
  private val mockUserAccessService = mockk<Cas2UserAccessService>()
  private val mockEmailNotificationService = mockk<EmailNotificationService>(relaxed = true)
  private val mockNotifyConfig = mockk<NotifyConfig>()
  private val mockCas2ApplicationNoteEmailService = mockk<Cas2ApplicationNoteEmailService>(relaxed = true)
  private val mockFeatureFlagService = mockk<FeatureFlagService>()

  private val applicationUrlTemplate = "http://frontend/applications/#id"
  private val assessmentUrlTemplate = "http://frontend/assessments/#applicationId"

  private val service = Cas2ApplicationNoteService(
    mockCas2ApplicationRepository,
    mockCas2AssessmentRepository,
    mockCas2ApplicationNoteRepository,
    mockUserService,
    mockUserAccessService,
    mockEmailNotificationService,
    mockNotifyConfig,
    mockCas2ApplicationNoteEmailService,
    mockFeatureFlagService,
    applicationUrlTemplate,
    assessmentUrlTemplate,
  )

  private val user = Cas2UserEntityFactory()
    .withEmail("test@example.com")
    .produce()

  private val application = Cas2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.parse("2024-06-24T16:07:00Z"))
    .withCrn("CRN123")
    .withNomsNumber("NOMS123")
    .withApplicationOrigin(ApplicationOrigin.prisonBail)
    .produce()

  private val assessment = Cas2AssessmentEntityFactory()
    .withApplication(application)
    .withNacroReferralId("NACRO-ID")
    .withAssessorName("Assessor Name")
    .produce()

  private val assessmentIdForTest = UUID.randomUUID()

  @BeforeEach
  fun setup() {
    val emailConfig = EmailAddressConfig()
    emailConfig.cas2Assessors = "assessors@example.com"
    every { mockNotifyConfig.emailAddresses } returns emailConfig
    every { mockFeatureFlagService.getBooleanFlag("isr-email-changes-enabled") } returns false
  }

  @Nested
  inner class CreateAssessmentNote {
    @Test
    fun `returns NotFound when assessment does not exist`() {
      val assessmentId = UUID.randomUUID()
      every { mockCas2AssessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.BAIL) } returns null

      val result = service.createAssessmentNote(assessmentId, NewCas2ApplicationNote(note = "some note"))

      assertThat(result is CasResult.NotFound).isTrue
    }

    @Test
    fun `returns NotFound when application does not exist`() {
      val assessmentId = UUID.randomUUID()
      every { mockCas2AssessmentRepository.findByIdAndServiceOrigin(assessmentId, Cas2ServiceOrigin.BAIL) } returns assessment
      every { mockCas2ApplicationRepository.findByIdAndServiceOrigin(application.id, assessment.serviceOrigin) } returns null

      val result = service.createAssessmentNote(assessmentId, NewCas2ApplicationNote(note = "some note"))

      assertThat(result is CasResult.NotFound).isTrue
    }

    @Test
    fun `returns GeneralValidationError when application is not submitted`() {
      val unsubmittedApplication = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
      val assessmentWithUnsubmittedApp = Cas2AssessmentEntityFactory()
        .withApplication(unsubmittedApplication)
        .produce()

      every { mockCas2AssessmentRepository.findByIdAndServiceOrigin(assessmentWithUnsubmittedApp.id, Cas2ServiceOrigin.BAIL) } returns assessmentWithUnsubmittedApp
      every { mockCas2ApplicationRepository.findByIdAndServiceOrigin(unsubmittedApplication.id, assessmentWithUnsubmittedApp.serviceOrigin) } returns unsubmittedApplication

      val result = service.createAssessmentNote(assessmentWithUnsubmittedApp.id, NewCas2ApplicationNote(note = "some note"))

      assertThat(result is CasResult.GeneralValidationError).isTrue
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("This application has not been submitted")
    }

    @Test
    fun `returns Unauthorised when user cannot add note`() {
      every { mockCas2AssessmentRepository.findByIdAndServiceOrigin(assessment.id, Cas2ServiceOrigin.BAIL) } returns assessment
      every { mockCas2ApplicationRepository.findByIdAndServiceOrigin(application.id, assessment.serviceOrigin) } returns application
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanAddNote(user, application) } returns false

      val result = service.createAssessmentNote(assessment.id, NewCas2ApplicationNote(note = "some note"))

      assertThat(result is CasResult.Unauthorised).isTrue
    }

    @Test
    fun `saves note and sends email when successful`() {
      val assessmentForApp = Cas2AssessmentEntityFactory()
        .withNacroReferralId("NACRO-ID")
        .produce()

      val applicationForNote = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.parse("2024-06-24T16:07:00Z"))
        .withCrn("CRN123")
        .withNomsNumber("NOMS123")
        .withApplicationOrigin(ApplicationOrigin.prisonBail)
        .withAssessment(assessmentForApp)
        .produce()

      val anyAssessmentId = UUID.randomUUID()
      every { mockCas2AssessmentRepository.findByIdAndServiceOrigin(anyAssessmentId, any()) } returns assessmentForApp
      every { mockCas2ApplicationRepository.findByIdAndServiceOrigin(any(), any()) } returns applicationForNote
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanAddNote(any(), any()) } returns true
      every { mockCas2ApplicationNoteRepository.save(any()) } answers { it.invocation.args[0] as Cas2ApplicationNoteEntity }

      val result = service.createAssessmentNote(anyAssessmentId, NewCas2ApplicationNote(note = "some note"))

      assertThat(result is CasResult.Success).isTrue
      val savedNote = (result as CasResult.Success).value
      assertThat(savedNote.body).isEqualTo("some note")

      verify(exactly = 1) { mockCas2ApplicationNoteRepository.save(any()) }
    }
  }

  @Nested
  inner class SendEmail {
    @Test
    fun `sends assessor note added email via Cas2ApplicationNoteEmailService when flag is enabled and user is external`() {
      val externalUser = Cas2UserEntityFactory().withUserType(Cas2UserType.EXTERNAL).produce()
      every { mockFeatureFlagService.getBooleanFlag("isr-email-changes-enabled") } returns true
      every { mockCas2AssessmentRepository.findByIdAndServiceOrigin(assessment.id, Cas2ServiceOrigin.BAIL) } returns assessment
      every { mockCas2ApplicationRepository.findByIdAndServiceOrigin(application.id, assessment.serviceOrigin) } returns application
      every { mockUserService.getUserForRequest() } returns externalUser
      every { mockUserAccessService.userCanAddNote(externalUser, application) } returns true
      every { mockCas2ApplicationNoteRepository.save(any()) } answers { it.invocation.args[0] as Cas2ApplicationNoteEntity }

      service.createAssessmentNote(assessment.id, NewCas2ApplicationNote(note = "some note"))

      verify(exactly = 1) { mockCas2ApplicationNoteEmailService.assessorNoteAdded(application, assessment, any()) }
    }

    @Test
    fun `sends referrer note added email via Cas2ApplicationNoteEmailService when flag is enabled and user is internal`() {
      val nomisUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
      every { mockFeatureFlagService.getBooleanFlag("isr-email-changes-enabled") } returns true
      every { mockCas2AssessmentRepository.findByIdAndServiceOrigin(assessment.id, Cas2ServiceOrigin.BAIL) } returns assessment
      every { mockCas2ApplicationRepository.findByIdAndServiceOrigin(application.id, assessment.serviceOrigin) } returns application
      every { mockUserService.getUserForRequest() } returns nomisUser
      every { mockUserAccessService.userCanAddNote(nomisUser, application) } returns true
      every { mockCas2ApplicationNoteRepository.save(any()) } answers { it.invocation.args[0] as Cas2ApplicationNoteEntity }

      service.createAssessmentNote(assessment.id, NewCas2ApplicationNote(note = "some note"))

      verify(exactly = 1) { mockCas2ApplicationNoteEmailService.refererNoteAdded(application, assessment, any()) }
    }

    @Test
    fun `sends email to referrer via EmailNotificationService when flag is disabled and user is external`() {
      val externalUser = Cas2UserEntityFactory().withUserType(Cas2UserType.EXTERNAL).produce()
      val applicationWithOrigin = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withApplicationOrigin(ApplicationOrigin.prisonBail)
        .produce()
      val assessmentWithOrigin = Cas2AssessmentEntityFactory().withApplication(applicationWithOrigin).produce()

      every { mockFeatureFlagService.getBooleanFlag("isr-email-changes-enabled") } returns false
      every { mockCas2AssessmentRepository.findByIdAndServiceOrigin(assessmentWithOrigin.id, Cas2ServiceOrigin.BAIL) } returns assessmentWithOrigin
      every { mockCas2ApplicationRepository.findByIdAndServiceOrigin(applicationWithOrigin.id, assessmentWithOrigin.serviceOrigin) } returns applicationWithOrigin
      every { mockUserService.getUserForRequest() } returns externalUser
      every { mockUserAccessService.userCanAddNote(externalUser, applicationWithOrigin) } returns true
      every { mockCas2ApplicationNoteRepository.save(any()) } answers { it.invocation.args[0] as Cas2ApplicationNoteEntity }

      service.createAssessmentNote(assessmentWithOrigin.id, NewCas2ApplicationNote(note = "some note"))

      verify(exactly = 1) {
        mockEmailNotificationService.sendCas2Email(
          recipientEmailAddress = user.email!!,
          templateId = Cas2NotifyTemplates.CAS2_V2_NOTE_ADDED_FOR_REFERRER_PRISON_BAIL,
          personalisation = any(),
        )
      }
    }

    @Test
    fun `sends email to assessors via EmailNotificationService when flag is disabled and user is internal`() {
      val nomisUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()

      val assessmentForEmail = Cas2AssessmentEntityFactory()
        .withNacroReferralId("NACRO-ID")
        .withAssessorName("Assessor Name")
        .withServiceOrigin(Cas2ServiceOrigin.BAIL)
        .produce()

      val applicationForEmail = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withApplicationOrigin(ApplicationOrigin.courtBail)
        .withAssessment(assessmentForEmail)
        .produce()

      every { mockFeatureFlagService.getBooleanFlag("isr-email-changes-enabled") } returns false
      val anyAssessmentId = UUID.randomUUID()
      every { mockCas2AssessmentRepository.findByIdAndServiceOrigin(anyAssessmentId, any()) } returns assessmentForEmail
      every { mockCas2ApplicationRepository.findByIdAndServiceOrigin(any(), any()) } returns applicationForEmail
      every { mockUserService.getUserForRequest() } returns nomisUser
      every { mockUserAccessService.userCanAddNote(any(), any()) } returns true
      every { mockCas2ApplicationNoteRepository.save(any()) } answers { it.invocation.args[0] as Cas2ApplicationNoteEntity }

      service.createAssessmentNote(anyAssessmentId, NewCas2ApplicationNote(note = "some note"))

      verify(exactly = 1) {
        mockEmailNotificationService.sendCas2Email(
          recipientEmailAddress = "assessors@example.com",
          templateId = Cas2NotifyTemplates.CAS2_V2_NOTE_ADDED_FOR_ASSESSOR_COURT_BAIL,
          personalisation = match {
            it["nacroReferenceId"] == "NACRO-ID" &&
              it["assessorName"] == "Assessor Name" &&
              it["applicationUrl"] == assessmentUrlTemplate.replace("#applicationId", applicationForEmail.id.toString())
          },
        )
      }
    }
  }
}

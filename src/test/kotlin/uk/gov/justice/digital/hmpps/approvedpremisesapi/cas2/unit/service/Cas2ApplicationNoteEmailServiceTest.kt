package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationNoteEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2EmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2NoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.EmailAddressConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationNoteEmailServiceTest {
  private val mockCas2EmailService = mockk<Cas2EmailService>(relaxed = true)
  private val mockNotifyConfig = mockk<NotifyConfig>()
  private val applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id")
  private val assessmentUrlTemplate = UrlTemplate("http://frontend/assessments/#applicationId")

  private val service = Cas2ApplicationNoteEmailService(
    cas2EmailService = mockCas2EmailService,
    notifyConfig = mockNotifyConfig,
    applicationUrlTemplate = applicationUrlTemplate,
    assessmentUrlTemplate = assessmentUrlTemplate,
  )

  private val user = Cas2UserEntityFactory()
    .withEmail("test@example.com")
    .produce()

  private val application = Cas2ApplicationEntityFactory()
    .withId(UUID.randomUUID())
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.parse("2024-06-24T16:07:00Z"))
    .withCrn("CRN123")
    .withCohort(Cas2Cohort.PRISON_BAIL)
    .produce()

  private val note = Cas2NoteEntityFactory()
    .withApplication(application)
    .withCreatedByUser(user)
    .withCreatedAt(OffsetDateTime.parse("2024-06-25T10:30:00Z"))
    .produce()

  @BeforeEach
  fun setup() {
    val emailConfig = EmailAddressConfig()
    emailConfig.cas2Assessors = "assessors@example.com"
    every { mockNotifyConfig.emailAddresses } returns emailConfig
  }

  @Nested
  inner class Assessor {
    @Test
    fun `when assessor note is added then assessors are notified`() {
      service.assessorNoteAdded(application, note)

      verify(exactly = 1) {
        mockCas2EmailService.sendEmail(
          recipientEmailAddress = "assessors@example.com",
          templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_ASSESSOR_NOTE_ADDED,
          personalisation = mapOf(
            "dateNoteAdded" to "25 June 2024",
            "timeNoteAdded" to "10:30am",
            "cohort" to "Prison Bail",
            "crn" to "CRN123",
            "timeApplicationReceived" to "16:07",
            "dateApplicationReceived" to "24/06/2024",
            "nacroReferenceId" to application.id.toString(),
            "viewSubmittedApplicationUrl" to "http://frontend/assessments/${application.id}",
          ),
          cas2Application = application,
        )
      }
    }
  }

  @Nested
  inner class Referrer {
    @Test
    fun `when referer note is added then application creator is notified`() {
      service.refererNoteAdded(application, note)

      verify(exactly = 1) {
        mockCas2EmailService.sendEmail(
          recipientEmailAddress = user.email!!,
          templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_REFERRER_NOTE_ADDED,
          personalisation = mapOf(
            "dateNoteAdded" to "25 June 2024",
            "timeNoteAdded" to "10:30am",
            "cohort" to "Prison Bail",
            "crn" to "CRN123",
            "timeApplicationReceived" to "16:07",
            "dateApplicationReceived" to "24/06/2024",
            "nacroReferenceId" to application.id.toString(),
            "viewSubmittedApplicationUrl" to "http://frontend/applications/${application.id}",
          ),
          cas2Application = application,
        )
      }
    }

    @Test
    fun `when referer note is added but creator has no email then no email is sent`() {
      val userNoEmail = Cas2UserEntityFactory()
        .withEmail(null)
        .produce()
      val applicationNoEmail = Cas2ApplicationEntityFactory()
        .withId(UUID.randomUUID())
        .withCreatedByUser(userNoEmail)
        .withSubmittedAt(OffsetDateTime.now())
        .withCohort(Cas2Cohort.PRISON_BAIL)
        .produce()

      service.refererNoteAdded(applicationNoEmail, note)

      verify(exactly = 0) {
        mockCas2EmailService.sendEmail(any(), any(), any(), any())
      }
    }
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2EmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.EmailAddressConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationEmailServiceTest {
  private val mockCas2EmailService = mockk<Cas2EmailService>(relaxed = true)
  private val mockNotifyConfig = mockk<NotifyConfig>()
  private val submittedApplicationUrlTemplate = UrlTemplate("http://frontend/applications/#applicationId")

  private val service = Cas2ApplicationEmailService(
    cas2EmailService = mockCas2EmailService,
    notifyConfig = mockNotifyConfig,
    submittedApplicationUrlTemplate = submittedApplicationUrlTemplate,
  )

  private val user = Cas2UserEntityFactory()
    .withEmail("test@example.com")
    .produce()

  @Test
  fun `applicationSubmitted sends submitted and assessed emails`() {
    val emailConfig = EmailAddressConfig()
    emailConfig.cas2Assessors = "assessors@example.com"
    every { mockNotifyConfig.emailAddresses } returns emailConfig

    val applicationId = UUID.randomUUID()
    val submittedAt = OffsetDateTime.parse("2024-06-24T16:07:00Z")
    val application = Cas2ApplicationEntityFactory()
      .withId(applicationId)
      .withCreatedByUser(user)
      .withSubmittedAt(submittedAt)
      .withCrn("CRN123")
      .withCohort(Cas2Cohort.HDC)
      .produce()

    service.applicationSubmitted(application)

    verify(exactly = 1) {
      mockCas2EmailService.sendEmail(
        recipientEmailAddress = user.email!!,
        templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_SUBMITTED,
        personalisation = mapOf(
          "cohort" to "HDC",
          "crn" to "CRN123",
          "timeApplicationReceived" to "16:07",
          "dateApplicationReceived" to "24/06/2024",
          "nacroReferenceId" to applicationId.toString(),
          "viewSubmittedApplicationUrl" to "http://frontend/applications/$applicationId",
        ),
        cas2Application = application,
      )
    }

    verify(exactly = 1) {
      mockCas2EmailService.sendEmail(
        recipientEmailAddress = "assessors@example.com",
        templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_TO_ASSESS,
        personalisation = mapOf(
          "cohort" to "HDC",
          "crn" to "CRN123",
          "timeApplicationReceived" to "16:07",
          "dateApplicationReceived" to "24/06/2024",
          "nacroReferenceId" to applicationId.toString(),
          "viewSubmittedApplicationUrl" to "http://frontend/applications/$applicationId",
          "sla" to "3 working days",
          "referrerName" to application.createdByUser.name,
          "referrerEmail" to application.createdByUser.email,
          "referrerTelephoneNumber" to application.telephoneNumber,
        ),
        cas2Application = application,
      )
    }
  }
}

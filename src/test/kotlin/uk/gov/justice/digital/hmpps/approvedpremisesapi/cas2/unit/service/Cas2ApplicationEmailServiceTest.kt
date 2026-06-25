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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationEmailServiceTest {
  private val mockFeatureFlagService = mockk<FeatureFlagService>()
  private val mockCas2EmailService = mockk<Cas2EmailService>(relaxed = true)

  private val submittedApplicationUrlTemplate = UrlTemplate("http://frontend/applications/#applicationId")

  private val service = Cas2ApplicationEmailService(
    featureFlagService = mockFeatureFlagService,
    cas2EmailService = mockCas2EmailService,
    submittedApplicationUrlTemplate = submittedApplicationUrlTemplate,
  )

  private val user = Cas2UserEntityFactory()
    .withEmail("test@example.com")
    .produce()

  @Test
  fun `applicationSubmitted does not send an email if the feature flag is disabled`() {
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()

    every { mockFeatureFlagService.getBooleanFlag("isr-email-changes-enabled") } returns false

    service.applicationSubmitted(application)

    verify(exactly = 0) {
      mockCas2EmailService.sendEmail(any(), any(), any(), any())
    }
  }

  @Test
  fun `applicationSubmitted sends an email if the feature flag is enabled`() {
    val applicationId = UUID.randomUUID()
    val submittedAt = OffsetDateTime.parse("2024-06-24T16:07:00Z")
    val application = Cas2ApplicationEntityFactory()
      .withId(applicationId)
      .withCreatedByUser(user)
      .withSubmittedAt(submittedAt)
      .withCrn("CRN123")
      .withCohort(Cas2Cohort.HDC)
      .produce()

    every { mockFeatureFlagService.getBooleanFlag("isr-email-changes-enabled") } returns true

    service.applicationSubmitted(application)

    verify(exactly = 1) {
      mockCas2EmailService.sendEmail(
        recipientEmailAddress = user.email!!,
        templateId = Cas2NotifyTemplates.CAS2_APPLICATION_SUBMITTED,
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
  }
}

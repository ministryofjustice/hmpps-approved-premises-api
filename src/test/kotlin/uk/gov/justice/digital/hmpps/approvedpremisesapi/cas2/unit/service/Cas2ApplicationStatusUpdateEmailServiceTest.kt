package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2ApplicationStatusUpdateEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2EmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationStatusUpdateEmailServiceTest {
  private val mockCas2EmailService = mockk<Cas2EmailService>(relaxed = true)
  private val applicationOverviewUrlTemplate = UrlTemplate("http://frontend/applications/#applicationId")

  private val service = Cas2ApplicationStatusUpdateEmailService(
    cas2EmailService = mockCas2EmailService,
    applicationOverviewUrlTemplate = applicationOverviewUrlTemplate,
  )

  private val user = Cas2UserEntityFactory()
    .withEmail("test@example.com")
    .produce()

  @Test
  fun `statusUpdate sends email with correct personalisation`() {
    val applicationId = UUID.randomUUID()
    val submittedAt = OffsetDateTime.parse("2024-06-24T16:07:00Z")
    val statusCreatedAt = OffsetDateTime.parse("2024-06-25T10:30:00Z")

    val application = Cas2ApplicationEntityFactory()
      .withId(applicationId)
      .withCreatedByUser(user)
      .withSubmittedAt(submittedAt)
      .withCrn("CRN123")
      .withCohort(Cas2Cohort.PRISON_BAIL)
      .produce()

    val status = Cas2StatusUpdateEntityFactory()
      .withApplication(application)
      .withAssessor(user)
      .withLabel("More information requested")
      .withCreatedAt(statusCreatedAt)
      .produce()

    service.statusUpdate(application, status)

    verify(exactly = 1) {
      mockCas2EmailService.sendEmail(
        recipientEmailAddress = user.email!!,
        templateId = Cas2NotifyTemplates.CAS2_BAIL_APPLICATION_STATUS_UPDATE,
        personalisation = mapOf(
          "applicationStatusChange" to "More information requested",
          "dateStatusChanged" to "25 June 2024",
          "timeStatusChanged" to "10:30am",
          "cohort" to "Prison Bail",
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

  @Test
  fun `statusUpdate does not send email if user has no email address`() {
    val userNoEmail = Cas2UserEntityFactory()
      .withEmail(null)
      .produce()

    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(userNoEmail)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    val status = Cas2StatusUpdateEntityFactory()
      .withApplication(application)
      .withAssessor(user)
      .produce()

    service.statusUpdate(application, status)

    verify(exactly = 0) {
      mockCas2EmailService.sendEmail(any(), any(), any(), any())
    }
  }

  @Test
  fun `statusUpdate does not send email if application is not submitted`() {
    val application = Cas2ApplicationEntityFactory()
      .withCreatedByUser(user)
      .withSubmittedAt(null)
      .produce()

    val status = Cas2StatusUpdateEntityFactory()
      .withApplication(application)
      .withAssessor(user)
      .produce()

    assertThatThrownBy {
      service.statusUpdate(application, status)
    }.isInstanceOf(IllegalArgumentException::class.java)

    verify(exactly = 0) {
      mockCas2EmailService.sendEmail(any(), any(), any(), any())
    }
  }
}

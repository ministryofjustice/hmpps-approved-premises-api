package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockEmailNotificationService
import java.time.OffsetDateTime

class Cas1ApplicationEmailServiceTest {

  private object TestConstants {
    const val APPLICANT_EMAIL = "applicantEmail@test.com"
    const val CRN = "CRN123"
  }

  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockEmailNotificationService()

  val service = Cas1ApplicationEmailService(
    emailNotifier = mockEmailNotificationService,
    notifyConfig = notifyConfig,
  )

  @Test
  fun `applicationWithdrawn doesnt send email to applicant if no email addresses defined`() {
    val applicant = createUser(emailAddress = null)

    val application = createApplicationForApplicant(applicant)

    service.applicationWithdrawn(application)

    mockEmailNotificationService.assertNoEmailsRequested()
  }

  @Test
  fun `applicationWithdrawn sends an email to applicant if email addresses defined`() {
    val applicant = createUser(emailAddress = TestConstants.APPLICANT_EMAIL)

    val application = createApplicationForApplicant(applicant)

    service.applicationWithdrawn(application)

    mockEmailNotificationService.assertEmailRequestCount(1)

    val personalisation = mapOf(
      "crn" to TestConstants.CRN,
    )

    mockEmailNotificationService.assertEmailRequested(
      TestConstants.APPLICANT_EMAIL,
      notifyConfig.templates.applicationWithdrawn,
      personalisation,
    )
  }

  private fun createUser(emailAddress: String?) = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .withEmail(emailAddress)
    .produce()

  private fun createApplicationForApplicant(applicant: UserEntity) = ApprovedPremisesApplicationEntityFactory()
    .withCrn(TestConstants.CRN)
    .withCreatedByUser(applicant)
    .withSubmittedAt(OffsetDateTime.now())
    .withApArea(ApAreaEntityFactory().withName("test area").produce())
    .produce()
}

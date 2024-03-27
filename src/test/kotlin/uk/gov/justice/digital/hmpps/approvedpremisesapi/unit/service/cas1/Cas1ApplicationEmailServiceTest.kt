package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockEmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime

class Cas1ApplicationEmailServiceTest {

  private object TestConstants {
    const val APPLICANT_EMAIL = "applicantEmail@test.com"
    const val APPLICANT_NAME = "the applicant name"
    const val CRN = "CRN123"
  }

  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockEmailNotificationService()

  private val service = createService(aps530WithdrawalEmailImprovements = false)
  private val serviceUsingAps530Improvements = createService(aps530WithdrawalEmailImprovements = true)

  private fun createService(aps530WithdrawalEmailImprovements: Boolean) = Cas1ApplicationEmailService(
    emailNotifier = mockEmailNotificationService,
    notifyConfig = notifyConfig,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
    applicationTimelineUrlTemplate = UrlTemplate("http://frontend/applications/#applicationId?tab=timeline"),
    aps530WithdrawalEmailImprovements = aps530WithdrawalEmailImprovements,
  )

  @Nested
  inner class ApplicationSubmitted {

    @Test
    fun `applicationSubmitted doesnt send email to applicant if no email addresses defined`() {
      val applicant = createUser(emailAddress = null)

      val application = createApplicationForApplicant(applicant)

      service.applicationSubmitted(application)

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `applicationSubmitted sends an email to applicant if email addresses defined`() {
      val applicant = createUser(
        name = TestConstants.APPLICANT_NAME,
        emailAddress = TestConstants.APPLICANT_EMAIL,
      )

      val application = createApplicationForApplicant(applicant)

      service.applicationSubmitted(application)

      mockEmailNotificationService.assertEmailRequestCount(1)

      val personalisation = mapOf(
        "name" to TestConstants.APPLICANT_NAME,
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "crn" to TestConstants.CRN,
      )

      mockEmailNotificationService.assertEmailRequested(
        TestConstants.APPLICANT_EMAIL,
        notifyConfig.templates.applicationSubmitted,
        personalisation,
      )
    }
  }

  @Test
  fun `applicationWithdrawn doesnt send email to applicant if no email addresses defined`() {
    val applicant = createUser(emailAddress = null)
    val withdrawingUser = createUser(emailAddress = null)

    val application = createApplicationForApplicant(applicant)

    service.applicationWithdrawn(application, withdrawingUser)

    mockEmailNotificationService.assertNoEmailsRequested()
  }

  @Test
  fun `applicationWithdrawn sends an email to applicant if email addresses defined`() {
    val applicant = createUser(emailAddress = TestConstants.APPLICANT_EMAIL)
    val withdrawingUser = createUser(emailAddress = null)

    val application = createApplicationForApplicant(applicant)

    service.applicationWithdrawn(application, withdrawingUser)

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

  @Test
  fun `applicationWithdrawn sends a V2 email to applicant if email addresses defined`() {
    val applicant = createUser(emailAddress = TestConstants.APPLICANT_EMAIL)
    val withdrawingUser = createUser(name = "the withdrawing user")

    val application = createApplicationForApplicant(applicant)

    serviceUsingAps530Improvements.applicationWithdrawn(application, withdrawingUser)

    mockEmailNotificationService.assertEmailRequestCount(1)

    val personalisation = mapOf(
      "crn" to TestConstants.CRN,
      "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
      "withdrawnBy" to "the withdrawing user",
    )

    mockEmailNotificationService.assertEmailRequested(
      TestConstants.APPLICANT_EMAIL,
      notifyConfig.templates.applicationWithdrawnV2,
      personalisation,
    )
  }

  private fun createUser(
    emailAddress: String? = null,
    name: String = "default name",
  ) = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .withEmail(emailAddress)
    .withName(name)
    .produce()

  private fun createApplicationForApplicant(applicant: UserEntity) = ApprovedPremisesApplicationEntityFactory()
    .withCrn(TestConstants.CRN)
    .withCreatedByUser(applicant)
    .withSubmittedAt(OffsetDateTime.now())
    .withApArea(ApAreaEntityFactory().withName("test area").produce())
    .produce()
}

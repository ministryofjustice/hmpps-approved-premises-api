package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationEmailServiceTest.TestConstants.APPLICANT_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationEmailServiceTest.TestConstants.ASSESSOR_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockEmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime

class Cas1PlacementApplicationEmailServiceTest {

  private object TestConstants {
    const val APPLICANT_EMAIL = "applicantEmail@test.com"
    const val CRN = "CRN123"
    const val ASSESSOR_EMAIL = "matcherEmail@test.com"
  }

  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockEmailNotificationService()

  val service = Cas1PlacementApplicationEmailService(
    mockEmailNotificationService,
    notifyConfig = notifyConfig,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
    sendNewWithdrawalNotifications = true,
  )

  @Test
  fun `placementApplicationWithdrawn doesnt send email to applicant or assessor if no email addresses defined`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(null)
      .produce()

    val assessor = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(null)
      .produce()

    val application = createApplicationForApplicant(applicant)

    val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(applicant)
      .withAllocatedToUser(assessor)
      .produce()

    service.placementApplicationWithdrawn(placementApplication, wasBeingAssessedBy = assessor)

    mockEmailNotificationService.assertNoEmailsRequested()
  }

  @Test
  fun `placementApplicationWithdrawn sends an email to applicant if email addresses defined`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(APPLICANT_EMAIL)
      .produce()

    val application = createApplicationForApplicant(applicant)

    val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(applicant)
      .produce()

    service.placementApplicationWithdrawn(placementApplication, wasBeingAssessedBy = null)

    mockEmailNotificationService.assertEmailRequestCount(1)

    val personalisation = mapOf(
      "applicationUrl" to "http://frontend/applications/${application.id}",
      "crn" to TestConstants.CRN,
    )

    mockEmailNotificationService.assertEmailRequested(
      APPLICANT_EMAIL,
      notifyConfig.templates.placementRequestWithdrawn,
      personalisation,
    )
  }

  @Test
  fun `placementApplicationWithdrawn sends an email to assessor if email address defined`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(null)
      .produce()

    val assessor = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(ASSESSOR_EMAIL)
      .produce()

    val application = createApplicationForApplicant(applicant)

    val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(applicant)
      .withAllocatedToUser(assessor)
      .produce()

    service.placementApplicationWithdrawn(placementApplication, wasBeingAssessedBy = assessor)

    mockEmailNotificationService.assertEmailRequestCount(1)

    val personalisation = mapOf(
      "applicationUrl" to "http://frontend/applications/${application.id}",
      "crn" to TestConstants.CRN,
    )

    mockEmailNotificationService.assertEmailRequested(
      ASSESSOR_EMAIL,
      notifyConfig.templates.placementRequestWithdrawn,
      personalisation,
    )
  }

  private fun createApplicationForApplicant(applicant: UserEntity) = ApprovedPremisesApplicationEntityFactory()
    .withCrn(TestConstants.CRN)
    .withCreatedByUser(applicant)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementDateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationEmailServiceTest.TestConstants.APPLICANT_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationEmailServiceTest.TestConstants.AREA_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationEmailServiceTest.TestConstants.ASSESSOR_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockEmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1PlacementApplicationEmailServiceTest {
  private object TestConstants {
    const val APPLICANT_EMAIL = "applicantEmail@test.com"
    const val AREA_NAME = "theAreaName"
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
    sendPlacementRequestNotifications = true,
  )

  @Test
  fun `placementApplicationSubmitted doesnt sent email to applicant if no email address defined`() {
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

    service.placementApplicationSubmitted(placementApplication)

    mockEmailNotificationService.assertNoEmailsRequested()
  }

  @Test
  fun `placementApplicationSubmitted sends email to applicant if email address defined`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(APPLICANT_EMAIL)
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

    service.placementApplicationSubmitted(placementApplication)

    mockEmailNotificationService.assertEmailRequestCount(1)

    val personalisation = mapOf(
      "crn" to TestConstants.CRN,
    )

    mockEmailNotificationService.assertEmailRequested(
      APPLICANT_EMAIL,
      notifyConfig.templates.placementRequestSubmitted,
      personalisation,
    )
  }

  @Test
  fun `placementApplicationAllocated doesnt sent email to applicant if no email address defined`() {
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

    service.placementApplicationAllocated(placementApplication)

    mockEmailNotificationService.assertNoEmailsRequested()
  }

  @Test
  fun `placementApplicationAllocated sends email to applicant if email address defined`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(APPLICANT_EMAIL)
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

    service.placementApplicationAllocated(placementApplication)

    mockEmailNotificationService.assertEmailRequestCount(1)

    val personalisation = mapOf(
      "crn" to TestConstants.CRN,
    )

    mockEmailNotificationService.assertEmailRequested(
      APPLICANT_EMAIL,
      notifyConfig.templates.placementRequestAllocated,
      personalisation,
    )
  }

  @Test
  fun `placementApplicationWithdrawn doesnt send email to applicant or assessor if no email addresses defined`() {
    val applicant = createUser(emailAddress = null)
    val assessor = createUser(emailAddress = null)

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
    val applicant = createUser(emailAddress = APPLICANT_EMAIL)

    val application = createApplicationForApplicant(applicant)

    val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(applicant)
      .produce()

    placementApplication.placementDates = mutableListOf(
      PlacementDateEntityFactory()
        .withExpectedArrival(LocalDate.of(2020, 3, 12))
        .withDuration(10)
        .withPlacementApplication(placementApplication)
        .produce(),
    )

    service.placementApplicationWithdrawn(placementApplication, wasBeingAssessedBy = null)

    mockEmailNotificationService.assertEmailRequestCount(1)

    val personalisation = mapOf(
      "applicationUrl" to "http://frontend/applications/${application.id}",
      "crn" to TestConstants.CRN,
      "applicationArea" to AREA_NAME,
      "startDate" to "2020-03-12",
      "endDate" to "2020-03-22",
      "additionalDatesSet" to "no",
    )

    mockEmailNotificationService.assertEmailRequested(
      APPLICANT_EMAIL,
      notifyConfig.templates.placementRequestWithdrawn,
      personalisation,
    )
  }

  @Test
  fun `placementApplicationWithdrawn sends an email to assessor if email address defined`() {
    val applicant = createUser(emailAddress = null)
    val assessor = createUser(emailAddress = ASSESSOR_EMAIL)

    val application = createApplicationForApplicant(applicant)

    val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(applicant)
      .withAllocatedToUser(assessor)
      .produce()

    placementApplication.placementDates = mutableListOf(
      PlacementDateEntityFactory()
        .withExpectedArrival(LocalDate.of(2020, 3, 12))
        .withDuration(10)
        .withPlacementApplication(placementApplication)
        .produce(),
    )

    service.placementApplicationWithdrawn(placementApplication, wasBeingAssessedBy = assessor)

    mockEmailNotificationService.assertEmailRequestCount(1)

    val personalisation = mapOf(
      "applicationUrl" to "http://frontend/applications/${application.id}",
      "crn" to TestConstants.CRN,
      "applicationArea" to AREA_NAME,
      "startDate" to "2020-03-12",
      "endDate" to "2020-03-22",
      "additionalDatesSet" to "no",
    )

    mockEmailNotificationService.assertEmailRequested(
      ASSESSOR_EMAIL,
      notifyConfig.templates.placementRequestWithdrawn,
      personalisation,
    )
  }

  @Test
  fun `placementApplicationWithdrawn sends an email with 'additionalDates' if multiple dates defined due to a legacy placement application`() {
    val applicant = createUser(emailAddress = null)
    val assessor = createUser(emailAddress = ASSESSOR_EMAIL)

    val application = createApplicationForApplicant(applicant)

    val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(applicant)
      .withAllocatedToUser(assessor)
      .produce()

    placementApplication.placementDates = mutableListOf(
      PlacementDateEntityFactory()
        .withExpectedArrival(LocalDate.of(2020, 3, 12))
        .withDuration(10)
        .withPlacementApplication(placementApplication)
        .produce(),
      PlacementDateEntityFactory()
        .withExpectedArrival(LocalDate.of(2021, 3, 12))
        .withDuration(10)
        .withPlacementApplication(placementApplication)
        .produce(),
    )

    service.placementApplicationWithdrawn(placementApplication, wasBeingAssessedBy = assessor)

    mockEmailNotificationService.assertEmailRequestCount(1)

    val personalisation = mapOf(
      "additionalDatesSet" to "yes",
    )

    mockEmailNotificationService.assertEmailRequested(
      ASSESSOR_EMAIL,
      notifyConfig.templates.placementRequestWithdrawn,
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
    .withApArea(ApAreaEntityFactory().withName(AREA_NAME).produce())
    .produce()
}

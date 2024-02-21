package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.CRU_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockEmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime

class Cas1PlacementRequestEmailServiceTest {

  private object TestConstants {
    const val CRN = "CRN123"
    const val CRU_EMAIL = "cruEmail@test.com"
  }

  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockEmailNotificationService()

  val service = Cas1PlacementRequestEmailService(
    mockEmailNotificationService,
    notifyConfig = notifyConfig,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
    sendNewWithdrawalNotifications = true,
  )

  @Test
  fun `placementRequestWithdrawn doesnt send email to CRU if no email addresses defined`() {
    val application = createApplication(apAreaEmail = null)
    val placementRequest = createPlacementRequest(application, booking = null)

    service.placementRequestWithdrawn(placementRequest)

    mockEmailNotificationService.assertNoEmailsRequested()
  }

  @Test
  fun `placementRequestWithdrawn doesnt send email to CRU if email addresses defined and active booking`() {
    val application = createApplication(apAreaEmail = CRU_EMAIL)
    val booking = BookingEntityFactory()
      .withApplication(application)
      .withDefaultPremises()
      .produce()
    val placementRequest = createPlacementRequest(application, booking)

    service.placementRequestWithdrawn(placementRequest)

    mockEmailNotificationService.assertNoEmailsRequested()
  }

  @Test
  fun `placementRequestWithdrawn sends email to CRU if email addresses defined and no booking`() {
    val application = createApplication(apAreaEmail = CRU_EMAIL)
    val placementRequest = createPlacementRequest(application, booking = null)

    service.placementRequestWithdrawn(placementRequest)

    mockEmailNotificationService.assertEmailRequestCount(1)

    val personalisation = mapOf(
      "applicationUrl" to "http://frontend/applications/${application.id}",
      "crn" to TestConstants.CRN,
    )

    mockEmailNotificationService.assertEmailRequested(
      CRU_EMAIL,
      notifyConfig.templates.matchRequestWithdrawn,
      personalisation,
    )
  }

  private fun createApplication(apAreaEmail: String?): ApprovedPremisesApplicationEntity {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    return ApprovedPremisesApplicationEntityFactory()
      .withCrn(TestConstants.CRN)
      .withCreatedByUser(applicant)
      .withSubmittedAt(OffsetDateTime.now())
      .withApArea(ApAreaEntityFactory()
        .withEmailAddress(apAreaEmail)
        .produce())
      .produce()
  }

  private fun createPlacementRequest(application: ApprovedPremisesApplicationEntity,
                                     booking: BookingEntity?): PlacementRequestEntity {
    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    return PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements( PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce())
      .withBooking(booking)
      .produce()
  }
}

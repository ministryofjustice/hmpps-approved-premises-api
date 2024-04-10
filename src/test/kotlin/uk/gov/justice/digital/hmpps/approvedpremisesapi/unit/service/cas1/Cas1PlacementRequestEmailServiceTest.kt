package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.APPLICANT_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.AREA_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.CASE_MANAGER_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.CRU_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.WITHDRAWING_USER_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockEmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime

class Cas1PlacementRequestEmailServiceTest {

  private object TestConstants {
    const val APPLICANT_EMAIL = "application@test.com"
    const val AREA_NAME = "theAreaName"
    const val CRN = "CRN123"
    const val CRU_EMAIL = "cruEmail@test.com"
    const val WITHDRAWING_USER_NAME = "the withdrawing user"
    const val CASE_MANAGER_EMAIL = "caseManager@test.com"
  }

  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockEmailNotificationService()

  private val service = buildService(aps530WithdrawalEmailImprovements = false)
  private val serviceUsingAps530Improvements = buildService(aps530WithdrawalEmailImprovements = true)

  private val withdrawingUser = UserEntityFactory()
    .withDefaults()
    .withName(WITHDRAWING_USER_NAME)
    .produce()

  fun buildService(aps530WithdrawalEmailImprovements: Boolean) = Cas1PlacementRequestEmailService(
    mockEmailNotificationService,
    notifyConfig = notifyConfig,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
    applicationTimelineUrlTemplate = UrlTemplate("http://frontend/applications/#applicationId?tab=timeline"),
    aps530WithdrawalEmailImprovements = aps530WithdrawalEmailImprovements,
  )

  @Test
  fun `placementRequestWithdrawn doesnt send email to CRU if no email addresses defined`() {
    val application = createApplication(apAreaEmail = null)
    val placementRequest = createPlacementRequest(application, booking = null)

    service.placementRequestWithdrawn(placementRequest, withdrawingUser)

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

    service.placementRequestWithdrawn(placementRequest, withdrawingUser)

    mockEmailNotificationService.assertNoEmailsRequested()
  }

  @Test
  fun `placementRequestWithdrawn sends match request withdrawn email to CRU if email addresses defined and no booking`() {
    val application = createApplication(apAreaEmail = CRU_EMAIL)
    val placementRequest = createPlacementRequest(application, booking = null)

    service.placementRequestWithdrawn(placementRequest, withdrawingUser)

    mockEmailNotificationService.assertEmailRequestCount(1)

    mockEmailNotificationService.assertEmailRequested(
      CRU_EMAIL,
      notifyConfig.templates.matchRequestWithdrawn,
      mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to placementRequest.expectedArrival.toString(),
        "endDate" to placementRequest.expectedDeparture().toString(),
      ),
    )
  }

  @Test
  fun `placementRequestWithdrawn sends match request withdrawn email V2 to CRU if email addresses defined and no booking`() {
    val application = createApplication(apAreaEmail = CRU_EMAIL)
    val placementRequest = createPlacementRequest(application, booking = null)

    serviceUsingAps530Improvements.placementRequestWithdrawn(placementRequest, withdrawingUser)

    mockEmailNotificationService.assertEmailRequestCount(1)

    mockEmailNotificationService.assertEmailRequested(
      CRU_EMAIL,
      notifyConfig.templates.matchRequestWithdrawnV2,
      mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to placementRequest.expectedArrival.toString(),
        "endDate" to placementRequest.expectedDeparture().toString(),
        "withdrawnBy" to WITHDRAWING_USER_NAME,
      ),
    )
  }

  @Test
  fun `placementRequestWithdrawn does not send email to applicant if placement request not linked to placement application but no email addresses defined`() {
    val application = createApplication(
      applicantEmail = null,
    )
    val booking = BookingEntityFactory()
      .withApplication(application)
      .withDefaultPremises()
      .produce()

    val placementRequest = createPlacementRequest(
      application,
      booking,
      hasPlacementApplication = false,
    )

    service.placementRequestWithdrawn(placementRequest, withdrawingUser)

    mockEmailNotificationService.assertNoEmailsRequested()
  }

  @SuppressWarnings("MaxLineLength")
  @Test
  fun `placementRequestWithdrawn does not send email to applicant if placement request linked to placement application because this is a cascaded withdrawal from placement application and they would be informed of placement application being withdrawn instead`() {
    val application = createApplication(
      applicantEmail = null,
    )
    val booking = BookingEntityFactory()
      .withApplication(application)
      .withDefaultPremises()
      .produce()

    val placementRequest = createPlacementRequest(
      application,
      booking,
      hasPlacementApplication = true,
    )

    service.placementRequestWithdrawn(placementRequest, withdrawingUser)

    mockEmailNotificationService.assertNoEmailsRequested()
  }

  @Test
  fun `placementRequestWithdrawn sends placement request withdrawn email to applicant if placement request not linked to placement application `() {
    val application = createApplication(
      applicantEmail = APPLICANT_EMAIL,
    )
    val booking = BookingEntityFactory()
      .withApplication(application)
      .withDefaultPremises()
      .produce()

    val placementRequest = createPlacementRequest(
      application,
      booking,
      hasPlacementApplication = false,
    )

    service.placementRequestWithdrawn(placementRequest, withdrawingUser)

    mockEmailNotificationService.assertEmailRequestCount(1)
    mockEmailNotificationService.assertEmailRequested(
      APPLICANT_EMAIL,
      notifyConfig.templates.placementRequestWithdrawn,
      mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to placementRequest.expectedArrival.toString(),
        "endDate" to placementRequest.expectedDeparture().toString(),
        "additionalDatesSet" to "no",
      ),
    )
  }

  @Test
  fun `placementRequestWithdrawn sends placement request withdrawn email V2 to applicant if placement request not linked to placement application `() {
    val application = createApplication(
      applicantEmail = APPLICANT_EMAIL,
    )
    val booking = BookingEntityFactory()
      .withApplication(application)
      .withDefaultPremises()
      .produce()

    val placementRequest = createPlacementRequest(
      application,
      booking,
      hasPlacementApplication = false,
    )

    serviceUsingAps530Improvements.placementRequestWithdrawn(placementRequest, withdrawingUser)

    mockEmailNotificationService.assertEmailRequestCount(1)
    mockEmailNotificationService.assertEmailRequested(
      APPLICANT_EMAIL,
      notifyConfig.templates.placementRequestWithdrawnV2,
      mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to placementRequest.expectedArrival.toString(),
        "endDate" to placementRequest.expectedDeparture().toString(),
        "withdrawnBy" to WITHDRAWING_USER_NAME,
        "additionalDatesSet" to "no",
      ),
    )
  }

  @Test
  fun `placementRequestWithdrawn sends placement request withdrawn email to applicant and case manager if case manager not applicant`() {
    val application = createApplication(
      applicantEmail = APPLICANT_EMAIL,
      caseManagerNotApplicant = true,
    )
    val booking = BookingEntityFactory()
      .withApplication(application)
      .withDefaultPremises()
      .produce()

    val placementRequest = createPlacementRequest(
      application,
      booking,
      hasPlacementApplication = false,
    )

    serviceUsingAps530Improvements.placementRequestWithdrawn(placementRequest, withdrawingUser)

    mockEmailNotificationService.assertEmailRequestCount(2)
    mockEmailNotificationService.assertEmailRequested(
      APPLICANT_EMAIL,
      notifyConfig.templates.placementRequestWithdrawnV2,
      mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to placementRequest.expectedArrival.toString(),
        "endDate" to placementRequest.expectedDeparture().toString(),
        "withdrawnBy" to WITHDRAWING_USER_NAME,
        "additionalDatesSet" to "no",
      ),
    )
    mockEmailNotificationService.assertEmailRequested(
      CASE_MANAGER_EMAIL,
      notifyConfig.templates.placementRequestWithdrawnV2,
      mapOf(
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to TestConstants.CRN,
        "applicationArea" to AREA_NAME,
        "startDate" to placementRequest.expectedArrival.toString(),
        "endDate" to placementRequest.expectedDeparture().toString(),
        "withdrawnBy" to WITHDRAWING_USER_NAME,
        "additionalDatesSet" to "no",
      ),
    )
  }

  private fun createApplication(
    applicantEmail: String? = null,
    apAreaEmail: String? = null,
    caseManagerNotApplicant: Boolean = false,
  ): ApprovedPremisesApplicationEntity {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(applicantEmail)
      .produce()

    return ApprovedPremisesApplicationEntityFactory()
      .withCrn(TestConstants.CRN)
      .withCreatedByUser(applicant)
      .withSubmittedAt(OffsetDateTime.now())
      .withApArea(
        ApAreaEntityFactory()
          .withName(AREA_NAME)
          .withEmailAddress(apAreaEmail)
          .produce(),
      )
      .withCaseManagerIsNotApplicant(caseManagerNotApplicant)
      .withCaseManagerUserDetails(
        if (caseManagerNotApplicant) {
          Cas1ApplicationUserDetailsEntityFactory().withEmailAddress(CASE_MANAGER_EMAIL).produce()
        } else {
          null
        },
      )
      .produce()
  }

  private fun createPlacementRequest(
    application: ApprovedPremisesApplicationEntity,
    booking: BookingEntity?,
    hasPlacementApplication: Boolean = false,
  ): PlacementRequestEntity {
    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    val placementApplication = PlacementApplicationEntityFactory()
      .withApplication(application)
      .withCreatedByUser(application.createdByUser)
      .produce()

    val placementRequirements = PlacementRequirementsEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .produce()

    return PlacementRequestEntityFactory()
      .withApplication(application)
      .withAssessment(assessment)
      .withPlacementRequirements(placementRequirements)
      .withPlacementApplication(if (hasPlacementApplication) placementApplication else null)
      .withBooking(booking)
      .produce()
  }
}

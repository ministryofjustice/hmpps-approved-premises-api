package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementRequestEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.APPLICANT_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.AREA_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.CASE_MANAGER_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.CRU_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementRequestEmailServiceTest.TestConstants.WITHDRAWING_USER_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockCas1EmailNotificationService
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

  private val mockEmailNotificationService = MockCas1EmailNotificationService()

  private val service = Cas1PlacementRequestEmailService(
    mockEmailNotificationService,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
    applicationTimelineUrlTemplate = UrlTemplate("http://frontend/applications/#applicationId?tab=timeline"),
  )

  @Nested
  inner class PlacementRequestCreated {

    @Test
    fun `placementRequestCreates doesnt send an email if not defined`() {
      val application = createApplication(applicantEmail = null)

      service.placementRequestSubmitted(application = application)

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `placementRequestCreates sends an email if address defined`() {
      val application = createApplication(applicantEmail = APPLICANT_EMAIL)

      service.placementRequestSubmitted(application = application)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        APPLICANT_EMAIL,
        Cas1NotifyTemplates.PLACEMENT_REQUEST_SUBMITTED,
        mapOf(
          "crn" to CRN,
        ),
        application,
      )
    }
  }

  @Nested
  inner class PlacementRequestWithdrawn {

    private val withdrawingUser = UserEntityFactory()
      .withDefaults()
      .withName(WITHDRAWING_USER_NAME)
      .produce()

    @Test
    fun `placementRequestWithdrawn doesnt send email to CRU if no email addresses defined`() {
      val application = createApplication(cruManagementAreaEmail = null)
      val placementRequest = createPlacementRequest(application, spaceBooking = null)

      service.placementRequestWithdrawn(placementRequest, WithdrawalTriggeredByUser(withdrawingUser))

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `placementRequestWithdrawn doesnt send email to CRU if email addresses defined and active booking`() {
      val application = createApplication(cruManagementAreaEmail = CRU_EMAIL)
      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .produce()
      val placementRequest = createPlacementRequest(application, spaceBooking)

      service.placementRequestWithdrawn(placementRequest, WithdrawalTriggeredByUser(withdrawingUser))

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `placementRequestWithdrawn sends match request withdrawn email to CRU if email addresses defined and no booking`() {
      val application = createApplication(cruManagementAreaEmail = CRU_EMAIL)
      val placementRequest = createPlacementRequest(application, spaceBooking = null)

      service.placementRequestWithdrawn(placementRequest, WithdrawalTriggeredByUser(withdrawingUser))

      mockEmailNotificationService.assertEmailRequestCount(1)

      mockEmailNotificationService.assertEmailRequested(
        CRU_EMAIL,
        Cas1NotifyTemplates.MATCH_REQUEST_WITHDRAWN_V2,
        mapOf(
          "applicationUrl" to "http://frontend/applications/${application.id}",
          "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
          "crn" to CRN,
          "applicationArea" to AREA_NAME,
          "startDate" to placementRequest.expectedArrival.toString(),
          "endDate" to placementRequest.expectedDeparture().toString(),
          "withdrawnBy" to WITHDRAWING_USER_NAME,
        ),
        application,
      )
    }

    @Test
    fun `placementRequestWithdrawn does not send email to applicant if placement request not linked to placement application but no email addresses defined`() {
      val application = createApplication(
        applicantEmail = null,
      )
      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .produce()

      val placementRequest = createPlacementRequest(
        application,
        spaceBooking,
        hasPlacementApplication = false,
      )

      service.placementRequestWithdrawn(placementRequest, WithdrawalTriggeredByUser(withdrawingUser))

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @SuppressWarnings("MaxLineLength")
    @Test
    fun `placementRequestWithdrawn does not send email to applicant if placement request linked to placement application because this is a cascaded withdrawal from placement application and they would be informed of placement application being withdrawn instead`() {
      val application = createApplication(
        applicantEmail = null,
      )
      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .produce()

      val placementRequest = createPlacementRequest(
        application,
        spaceBooking,
        hasPlacementApplication = true,
      )

      service.placementRequestWithdrawn(placementRequest, WithdrawalTriggeredByUser(withdrawingUser))

      mockEmailNotificationService.assertNoEmailsRequested()
    }
  }

  private fun createApplication(
    applicantEmail: String? = null,
    cruManagementAreaEmail: String? = null,
    caseManagerNotApplicant: Boolean = false,
  ): ApprovedPremisesApplicationEntity {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(applicantEmail)
      .produce()

    return ApprovedPremisesApplicationEntityFactory()
      .withCrn(CRN)
      .withCreatedByUser(applicant)
      .withSubmittedAt(OffsetDateTime.now())
      .withApArea(
        ApAreaEntityFactory()
          .withName(AREA_NAME)
          .produce(),
      )
      .withCruManagementArea(
        Cas1CruManagementAreaEntityFactory()
          .withEmailAddress(cruManagementAreaEmail)
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
    spaceBooking: Cas1SpaceBookingEntity?,
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
      .withSpaceBookings(listOfNotNull(spaceBooking).toMutableList())
      .produce()
  }
}

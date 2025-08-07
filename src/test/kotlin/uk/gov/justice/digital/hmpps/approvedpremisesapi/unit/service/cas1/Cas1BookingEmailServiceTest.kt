package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredBySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.APPLICANT_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.CASE_MANAGER_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.CRU_MANAGEMENT_AREA_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.PLACEMENT_APPLICATION_CREATOR_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.PREMISES_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.PREMISES_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.REGION_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockCas1EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1BookingEmailServiceTest {

  private object TestConstants {
    const val CRU_MANAGEMENT_AREA_EMAIL = "apAreaEmail@test.com"
    const val APPLICANT_EMAIL = "applicantEmail@test.com"
    const val PLACEMENT_APPLICATION_CREATOR_EMAIL = "placementAppCreatorEmail@test.com"
    const val CRN = "CRN123"
    const val PREMISES_EMAIL = "premisesEmail@test.com"
    const val PREMISES_NAME = "The Premises Name"
    const val REGION_NAME = "The Region Name"
    const val WITHDRAWING_USER_NAME = "the withdrawing user"
    const val CASE_MANAGER_EMAIL = "caseManager@test.com"
  }

  private val mockEmailNotificationService = MockCas1EmailNotificationService()

  private val service = Cas1BookingEmailService(
    mockEmailNotificationService,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
    applicationTimelineUrlTemplate = UrlTemplate("http://frontend/applications/#applicationId?tab=timeline"),
    spaceBookingUrlTemplate = UrlTemplate("http://frontend/manage/premises/#premisesId/placements/#bookingId"),
  )

  private val withdrawingUser = UserEntityFactory()
    .withDefaults()
    .withName(TestConstants.WITHDRAWING_USER_NAME)
    .produce()

  val premises = ApprovedPremisesEntityFactory()
    .withDefaults()
    .withEmailAddress(PREMISES_EMAIL)
    .withName(PREMISES_NAME)
    .withProbationRegion(ProbationRegionEntityFactory().withDefaults().withName(REGION_NAME).produce())
    .produce()

  @BeforeEach
  fun beforeEach() {
    mockEmailNotificationService.reset()
  }

  @Nested
  inner class SpaceBookingWithdrawn {

    @Test
    fun `spaceBookingWithdrawn sends email to applicant, premises, case manager and CRU if emails are defined`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(APPLICANT_EMAIL)
        .produce()

      val application = createApplication(
        applicant = applicant,
        caseManagerNotApplicant = true,
        cruManagementArea = Cas1CruManagementAreaEntityFactory()
          .withEmailAddress(CRU_MANAGEMENT_AREA_EMAIL)
          .produce(),
      )

      val booking = createSpaceBooking(
        application,
        premises,
        arrivalDate = LocalDate.of(2023, 2, 1),
        departureDate = LocalDate.of(2023, 2, 14),
      )

      service.spaceBookingWithdrawn(
        spaceBooking = booking,
        application = application,
        withdrawalTriggeredBy = WithdrawalTriggeredByUser(withdrawingUser),
      )

      val expectedPersonalisation = mapOf(
        "apName" to PREMISES_NAME,
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to CRN,
        "startDate" to "2023-02-01",
        "endDate" to "2023-02-14",
        "region" to REGION_NAME,
        "withdrawnBy" to TestConstants.WITHDRAWING_USER_NAME,
      )

      mockEmailNotificationService.assertEmailRequestCount(4)
      mockEmailNotificationService.assertEmailRequested(
        APPLICANT_EMAIL,
        Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CASE_MANAGER_EMAIL,
        Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CRU_MANAGEMENT_AREA_EMAIL,
        Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
        expectedPersonalisation,
        application,
      )
    }

    @Test
    fun `spaceBookingWithdrawn doesn't send email to applicant, premises, case manager or CRU if email not defined`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withEmailAddress(null)
        .withName(PREMISES_NAME)
        .withProbationRegion(ProbationRegionEntityFactory().withDefaults().withName(REGION_NAME).produce())
        .produce()

      val application = createApplication(
        applicant = applicant,
        cruManagementArea = Cas1CruManagementAreaEntityFactory()
          .withEmailAddress(null)
          .produce(),
      )

      val booking = createSpaceBooking(
        application,
        premises,
        arrivalDate = LocalDate.of(2023, 2, 1),
        departureDate = LocalDate.of(2023, 2, 14),
      )

      service.spaceBookingWithdrawn(
        spaceBooking = booking,
        application = application,
        withdrawalTriggeredBy = WithdrawalTriggeredByUser(withdrawingUser),
      )

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `spaceBookingWithdrawn sends email when triggered by seed job`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(APPLICANT_EMAIL)
        .produce()

      val application = createApplication(
        applicant = applicant,
        caseManagerNotApplicant = true,
        cruManagementArea = Cas1CruManagementAreaEntityFactory()
          .withEmailAddress(CRU_MANAGEMENT_AREA_EMAIL)
          .produce(),
      )

      val booking = createSpaceBooking(
        application,
        premises,
        arrivalDate = LocalDate.of(2023, 2, 1),
        departureDate = LocalDate.of(2023, 2, 14),
      )

      service.spaceBookingWithdrawn(
        spaceBooking = booking,
        application = application,
        withdrawalTriggeredBy = WithdrawalTriggeredBySeedJob,
      )

      val expectedPersonalisation = mapOf(
        "apName" to PREMISES_NAME,
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to CRN,
        "startDate" to "2023-02-01",
        "endDate" to "2023-02-14",
        "region" to REGION_NAME,
        "withdrawnBy" to "Application Support",
      )

      mockEmailNotificationService.assertEmailRequestCount(4)
      mockEmailNotificationService.assertEmailRequested(
        APPLICANT_EMAIL,
        Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CASE_MANAGER_EMAIL,
        Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CRU_MANAGEMENT_AREA_EMAIL,
        Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
        expectedPersonalisation,
        application,
      )
    }
  }

  @Nested
  inner class SpaceBookingAmended {
    val applicant = UserEntityFactory()
      .withDefaults()
      .withEmail(APPLICANT_EMAIL)
      .withCruManagementArea(Cas1CruManagementAreaEntityFactory().withEmailAddress(CRU_MANAGEMENT_AREA_EMAIL).produce())
      .produce()

    val application = createApplication(
      applicant = applicant,
      caseManagerNotApplicant = true,
      cruManagementArea = Cas1CruManagementAreaEntityFactory()
        .withEmailAddress(CRU_MANAGEMENT_AREA_EMAIL)
        .produce(),
    )

    val booking = Cas1SpaceBookingEntityFactory()
      .withApplication(application)
      .withPremises(premises)
      .withCanonicalArrivalDate(LocalDate.of(2023, 2, 1))
      .withCanonicalDepartureDate(LocalDate.of(2023, 2, 14))
      .withPlacementRequest(
        PlacementRequestEntityFactory()
          .withPlacementApplication(
            PlacementApplicationEntityFactory()
              .withDefaults()
              .withCreatedByUser(UserEntityFactory().withDefaults().withEmail(PLACEMENT_APPLICATION_CREATOR_EMAIL).produce())
              .produce(),
          )
          .withDefaults()
          .produce(),
      )
      .produce()

    @Test
    fun `spaceBookingAmended sends email to applicant(s), premises, case manager when emails are defined`() {
      service.spaceBookingAmended(
        spaceBooking = booking,
        application = application,
        updateType = Cas1SpaceBookingService.UpdateType.AMENDMENT,
      )

      val expectedPersonalisation = mapOf(
        "apName" to PREMISES_NAME,
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to CRN,
        "startDate" to "2023-02-01",
        "endDate" to "2023-02-14",
        "lengthStay" to 2,
        "lengthStayUnit" to "weeks",
      )

      mockEmailNotificationService.assertEmailRequestCount(4)
      mockEmailNotificationService.assertEmailsRequested(
        setOf(
          APPLICANT_EMAIL,
          CASE_MANAGER_EMAIL,
          PLACEMENT_APPLICATION_CREATOR_EMAIL,
          PREMISES_EMAIL,
        ),
        Cas1NotifyTemplates.BOOKING_AMENDED,
        expectedPersonalisation,
        application,
      )
    }

    @Test
    fun `spaceBookingAmended for shortened space booking sends email to applicant(s), premises, case manager and CRU when emails are defined`() {
      service.spaceBookingAmended(
        spaceBooking = booking,
        application = application,
        updateType = Cas1SpaceBookingService.UpdateType.SHORTENING,
      )

      val expectedPersonalisation = mapOf(
        "apName" to PREMISES_NAME,
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "applicationTimelineUrl" to "http://frontend/applications/${application.id}?tab=timeline",
        "crn" to CRN,
        "startDate" to "2023-02-01",
        "endDate" to "2023-02-14",
        "lengthStay" to 2,
        "lengthStayUnit" to "weeks",
      )

      mockEmailNotificationService.assertEmailRequestCount(5)
      mockEmailNotificationService.assertEmailsRequested(
        setOf(
          APPLICANT_EMAIL,
          CASE_MANAGER_EMAIL,
          PLACEMENT_APPLICATION_CREATOR_EMAIL,
          PREMISES_EMAIL,
          CRU_MANAGEMENT_AREA_EMAIL,
        ),
        Cas1NotifyTemplates.BOOKING_AMENDED,
        expectedPersonalisation,
        application,
      )
    }
  }

  @SuppressWarnings("LongParameterList")
  private fun createSpaceBooking(
    application: ApprovedPremisesApplicationEntity,
    premises: ApprovedPremisesEntity,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
  ) = Cas1SpaceBookingEntityFactory()
    .withApplication(application)
    .withPremises(premises)
    .withCanonicalArrivalDate(arrivalDate)
    .withCanonicalDepartureDate(departureDate)
    .produce()

  private fun createApplication(
    applicant: UserEntity,
    apArea: ApAreaEntity = ApAreaEntityFactory().produce(),
    caseManagerNotApplicant: Boolean = false,
    cruManagementArea: Cas1CruManagementAreaEntity? = null,
  ) = ApprovedPremisesApplicationEntityFactory()
    .withCrn(CRN)
    .withCreatedByUser(applicant)
    .withSubmittedAt(OffsetDateTime.now())
    .withApArea(apArea)
    .withCaseManagerIsNotApplicant(caseManagerNotApplicant)
    .withCaseManagerUserDetails(
      if (caseManagerNotApplicant) {
        Cas1ApplicationUserDetailsEntityFactory().withEmailAddress(CASE_MANAGER_EMAIL).produce()
      } else {
        null
      },
    )
    .withCruManagementArea(cruManagementArea)
    .produce()
}

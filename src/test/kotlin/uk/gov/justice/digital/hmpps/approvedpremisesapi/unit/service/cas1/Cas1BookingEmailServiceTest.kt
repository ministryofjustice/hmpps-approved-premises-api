package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
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

  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockCas1EmailNotificationService()

  private val service = Cas1BookingEmailService(
    mockEmailNotificationService,
    notifyConfig = notifyConfig,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
    applicationTimelineUrlTemplate = UrlTemplate("http://frontend/applications/#applicationId?tab=timeline"),
    bookingUrlTemplate = UrlTemplate("http://frontend/premises/#premisesId/bookings/#bookingId"),
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
  inner class BookingMade {

    @Test
    fun `bookingMade doesnt send email to applicant if no email address defined`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(null)
        .produce()

      val (application, booking) = createApplicationAndBooking(
        applicant,
        premises,
        arrivalDate = LocalDate.of(2023, 2, 1),
        departureDate = LocalDate.of(2023, 2, 14),
      )

      service.bookingMade(
        application = application,
        booking = booking,
        placementApplication = null,
      )

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingMadePremises,
        mapOf(
          "apName" to PREMISES_NAME,
          "applicationUrl" to "http://frontend/applications/${application.id}",
          "bookingUrl" to "http://frontend/premises/${premises.id}/bookings/${booking.id}",
          "crn" to CRN,
          "startDate" to "2023-02-01",
          "endDate" to "2023-02-14",
          "lengthStay" to 2,
          "lengthStayUnit" to "weeks",
        ),
        application,
      )
    }

    @SuppressWarnings("CyclomaticComplexMethod")
    @Test
    fun `bookingMade sends email to applicant, premises email addresses when defined, when length of stay whole number of weeks`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(APPLICANT_EMAIL)
        .produce()

      val (application, booking) = createApplicationAndBooking(
        applicant,
        premises,
        arrivalDate = LocalDate.of(2023, 2, 1),
        departureDate = LocalDate.of(2023, 2, 14),
      )

      service.bookingMade(
        application = application,
        booking = booking,
        placementApplication = null,
      )

      mockEmailNotificationService.assertEmailRequestCount(2)

      val personalisation = mapOf(
        "apName" to PREMISES_NAME,
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "bookingUrl" to "http://frontend/premises/${premises.id}/bookings/${booking.id}",
        "crn" to CRN,
        "startDate" to "2023-02-01",
        "endDate" to "2023-02-14",
        "lengthStay" to 2,
        "lengthStayUnit" to "weeks",
      )

      mockEmailNotificationService.assertEmailRequested(
        APPLICANT_EMAIL,
        notifyConfig.templates.bookingMade,
        personalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingMadePremises,
        personalisation,
        application,
      )
    }

    @Test
    fun `bookingMade sends email to applicant and premises email addresses when defined, when length of stay not whole number of weeks`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(APPLICANT_EMAIL)
        .produce()

      val (application, booking) = createApplicationAndBooking(
        applicant,
        premises,
        arrivalDate = LocalDate.of(2023, 2, 22),
        departureDate = LocalDate.of(2023, 2, 27),
      )

      service.bookingMade(
        application = application,
        booking = booking,
        placementApplication = null,
      )

      mockEmailNotificationService.assertEmailRequestCount(2)

      val expectedPersonalisation = mapOf(
        "lengthStay" to 6,
        "lengthStayUnit" to "days",
      )

      mockEmailNotificationService.assertEmailRequested(
        APPLICANT_EMAIL,
        notifyConfig.templates.bookingMade,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingMadePremises,
        expectedPersonalisation,
        application,
      )
    }

    @SuppressWarnings("CyclomaticComplexMethod")
    @Test
    fun `bookingMade sends email to applicant, placement application creator and premises email addresses for request for placements`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(APPLICANT_EMAIL)
        .produce()

      val (application, booking) = createApplicationAndBooking(
        applicant,
        premises,
        arrivalDate = LocalDate.of(2023, 2, 1),
        departureDate = LocalDate.of(2023, 2, 14),
      )

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withCreatedByUser(
          UserEntityFactory()
            .withUnitTestControlProbationRegion()
            .withEmail(PLACEMENT_APPLICATION_CREATOR_EMAIL)
            .produce(),
        )
        .produce()

      booking.placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .produce()

      service.bookingMade(
        application = application,
        booking = booking,
        placementApplication = placementApplication,
      )

      mockEmailNotificationService.assertEmailRequestCount(3)

      val personalisation = mapOf(
        "apName" to PREMISES_NAME,
        "applicationUrl" to "http://frontend/applications/${application.id}",
        "bookingUrl" to "http://frontend/premises/${premises.id}/bookings/${booking.id}",
        "crn" to CRN,
        "startDate" to "2023-02-01",
        "endDate" to "2023-02-14",
        "lengthStay" to 2,
        "lengthStayUnit" to "weeks",
      )

      mockEmailNotificationService.assertEmailRequested(
        APPLICANT_EMAIL,
        notifyConfig.templates.bookingMade,
        personalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        PLACEMENT_APPLICATION_CREATOR_EMAIL,
        notifyConfig.templates.bookingMade,
        personalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingMadePremises,
        personalisation,
        application,
      )
    }
  }

  @Nested
  inner class BookingWithdrawn {

    @Test
    fun `bookingWithdrawn sends email to applicant, premises, case manager and CRU if emails are defined`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(APPLICANT_EMAIL)
        .produce()

      val (application, booking) = createApplicationAndBooking(
        applicant,
        premises,
        arrivalDate = LocalDate.of(2023, 2, 1),
        departureDate = LocalDate.of(2023, 2, 14),
        caseManagerNotApplicant = true,
        cruManagementArea = Cas1CruManagementAreaEntityFactory()
          .withEmailAddress(CRU_MANAGEMENT_AREA_EMAIL)
          .produce(),
      )

      service.bookingWithdrawn(
        application = application,
        booking = booking,
        placementApplication = null,
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
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CASE_MANAGER_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CRU_MANAGEMENT_AREA_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )
    }

    @Test
    fun `bookingWithdrawn doesn't send email to applicant, premises, case manager or CRU if email not defined`() {
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

      val (application, booking) = createApplicationAndBooking(
        applicant,
        premises,
        apArea = ApAreaEntityFactory().produce(),
        arrivalDate = LocalDate.of(2023, 2, 1),
        departureDate = LocalDate.of(2023, 2, 14),
        cruManagementArea = Cas1CruManagementAreaEntityFactory()
          .withEmailAddress(null)
          .produce(),
      )

      service.bookingWithdrawn(
        application = application,
        booking = booking,
        placementApplication = null,
        withdrawalTriggeredBy = WithdrawalTriggeredByUser(withdrawingUser),
      )

      mockEmailNotificationService.assertNoEmailsRequested()
    }

    @Test
    fun `bookingWithdrawn sends email when triggered by seed job`() {
      val applicant = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .withEmail(APPLICANT_EMAIL)
        .produce()

      val (application, booking) = createApplicationAndBooking(
        applicant,
        premises,
        arrivalDate = LocalDate.of(2023, 2, 1),
        departureDate = LocalDate.of(2023, 2, 14),
        caseManagerNotApplicant = true,
        cruManagementArea = Cas1CruManagementAreaEntityFactory()
          .withEmailAddress(CRU_MANAGEMENT_AREA_EMAIL)
          .produce(),
      )

      service.bookingWithdrawn(
        application = application,
        booking = booking,
        placementApplication = null,
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
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CASE_MANAGER_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CRU_MANAGEMENT_AREA_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )
    }
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
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CASE_MANAGER_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CRU_MANAGEMENT_AREA_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
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
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CASE_MANAGER_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )

      mockEmailNotificationService.assertEmailRequested(
        CRU_MANAGEMENT_AREA_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
        application,
      )
    }
  }

  @SuppressWarnings("LongParameterList")
  private fun createApplicationAndBooking(
    applicant: UserEntity,
    premises: ApprovedPremisesEntity,
    apArea: ApAreaEntity = ApAreaEntityFactory().produce(),
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    caseManagerNotApplicant: Boolean = false,
    cruManagementArea: Cas1CruManagementAreaEntity? = null,
  ): Pair<ApprovedPremisesApplicationEntity, BookingEntity> {
    val application = createApplication(applicant, apArea, caseManagerNotApplicant, cruManagementArea)

    val booking = BookingEntityFactory()
      .withApplication(application)
      .withPremises(premises)
      .withArrivalDate(arrivalDate)
      .withDepartureDate(departureDate)
      .produce()

    return Pair(application, booking)
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

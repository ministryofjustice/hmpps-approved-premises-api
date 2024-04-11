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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.APPLICANT_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.AP_AREA_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.CASE_MANAGER_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.PREMISES_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.PREMISES_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1BookingEmailServiceTest.TestConstants.REGION_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.MockEmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1BookingEmailServiceTest {

  private object TestConstants {
    const val AP_AREA_EMAIL = "apAreaEmail@test.com"
    const val APPLICANT_EMAIL = "applicantEmail@test.com"
    const val CRN = "CRN123"
    const val PREMISES_EMAIL = "premisesEmail@test.com"
    const val PREMISES_NAME = "The Premises Name"
    const val REGION_NAME = "The Region Name"
    const val WITHDRAWING_USER_NAME = "the withdrawing user"
    const val CASE_MANAGER_EMAIL = "caseManager@test.com"
  }

  private val notifyConfig = NotifyConfig()
  private val mockEmailNotificationService = MockEmailNotificationService()

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

      service.bookingMade(application, booking)

      mockEmailNotificationService.assertEmailRequestCount(1)
      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingMadePremises,
        mapOf(
          "name" to applicant.name,
          "apName" to PREMISES_NAME,
          "applicationUrl" to "http://frontend/applications/${application.id}",
          "bookingUrl" to "http://frontend/premises/${premises.id}/bookings/${booking.id}",
          "crn" to CRN,
          "startDate" to "2023-02-01",
          "endDate" to "2023-02-14",
          "lengthStay" to 2,
          "lengthStayUnit" to "weeks",
        ),
      )
    }

    @SuppressWarnings("CyclomaticComplexMethod")
    @Test
    fun `bookingMade sends email to applicant and premises email addresses when defined, when length of stay whole number of weeks`() {
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

      service.bookingMade(application, booking)

      mockEmailNotificationService.assertEmailRequestCount(2)

      val personalisation = mapOf(
        "name" to applicant.name,
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
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingMadePremises,
        personalisation,
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

      service.bookingMade(application, booking)

      mockEmailNotificationService.assertEmailRequestCount(2)

      val expectedPersonalisation = mapOf(
        "lengthStay" to 6,
        "lengthStayUnit" to "days",
      )

      mockEmailNotificationService.assertEmailRequested(
        APPLICANT_EMAIL,
        notifyConfig.templates.bookingMade,
        expectedPersonalisation,
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingMadePremises,
        expectedPersonalisation,
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
      )

      service.bookingWithdrawn(application, booking, withdrawingUser)

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
      )

      mockEmailNotificationService.assertEmailRequested(
        CASE_MANAGER_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
      )

      mockEmailNotificationService.assertEmailRequested(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
      )

      mockEmailNotificationService.assertEmailRequested(
        AP_AREA_EMAIL,
        notifyConfig.templates.bookingWithdrawnV2,
        expectedPersonalisation,
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
        apArea = ApAreaEntityFactory().withEmailAddress(null).produce(),
        arrivalDate = LocalDate.of(2023, 2, 1),
        departureDate = LocalDate.of(2023, 2, 14),
      )

      service.bookingWithdrawn(application, booking, withdrawingUser)

      mockEmailNotificationService.assertNoEmailsRequested()
    }
  }

  @SuppressWarnings("LongParameterList")
  private fun createApplicationAndBooking(
    applicant: UserEntity,
    premises: ApprovedPremisesEntity,
    apArea: ApAreaEntity = ApAreaEntityFactory().withEmailAddress(AP_AREA_EMAIL).produce(),
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    caseManagerNotApplicant: Boolean = false,
  ): Pair<ApprovedPremisesApplicationEntity, BookingEntity> {
    val application = ApprovedPremisesApplicationEntityFactory()
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
      .produce()

    val booking = BookingEntityFactory()
      .withApplication(application)
      .withPremises(premises)
      .withArrivalDate(arrivalDate)
      .withDepartureDate(departureDate)
      .produce()

    return Pair(application, booking)
  }
}

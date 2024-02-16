package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.TestConstants.APPLICANT_EMAIL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.TestConstants.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.TestConstants.PREMISES_EMAIL
import java.time.LocalDate
import java.time.OffsetDateTime

object TestConstants {
  const val APPLICANT_EMAIL = "applicantEmail@test.com"
  const val CRN = "CRN123"
  const val PREMISES_EMAIL = "premisesEmail@test.com"
}

class Cas1BookingEmailServiceTest {
  private val mockEmailNotificationService = mockk<EmailNotificationService>()
  private val notifyConfig = NotifyConfig()

  val service = Cas1BookingEmailService(
    mockEmailNotificationService,
    notifyConfig = notifyConfig,
    applicationUrlTemplate = "http://frontend/applications/#id",
    bookingUrlTemplate = "http://frontend/premises/#premisesId/bookings/#bookingId",
  )

  val premises = ApprovedPremisesEntityFactory()
    .withDefaults()
    .withEmailAddress(PREMISES_EMAIL)
    .produce()

  @Test
  fun `bookingMade doesnt send email to applicant if no email address defined`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(null)
      .produce()

    val (application,booking) = createApplicationAndBooking(
      applicant,
      premises,
      arrivalDate = LocalDate.of(2023,2,1),
      departureDate = LocalDate.of(2023,2,14),
    )

    every { mockEmailNotificationService.sendEmail(any(),any(),any()) } returns Unit

    service.bookingMade(application, booking)

    verify(exactly = 1) {
      mockEmailNotificationService.sendEmail(any(),any(),any())
    }

    verify(exactly = 1) {
      mockEmailNotificationService.sendEmail(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingMadePremises,
        match {
          it["name"] == applicant.name &&
            it["apName"] == premises.name &&
            (it["applicationUrl"] as String).matches(Regex("http://frontend/applications/${application.id}")) &&
            (it["bookingUrl"] as String).matches(Regex("http://frontend/premises/${premises.id}/bookings/${booking.id}")) &&
            it["crn"] == CRN &&
            it["startDate"] == "2023-02-01" &&
            it["endDate"] == "2023-02-14" &&
            (it["lengthStay"] as Int) == 2 &&
            (it["lengthStayUnit"] as String) == "weeks"
        },
      )
    }
  }

  @SuppressWarnings("CyclomaticComplexMethod")
  @Test
  fun `bookingMade sends email to applicant and premises email addresses when defined, when length of stay whole number of weeks`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(APPLICANT_EMAIL)
      .produce()

    val (application,booking) = createApplicationAndBooking(
      applicant,
      premises,
      arrivalDate = LocalDate.of(2023,2,1),
      departureDate = LocalDate.of(2023,2,14),
    )

    every { mockEmailNotificationService.sendEmail(any(),any(),any()) } returns Unit

    service.bookingMade(application, booking)

    verify(exactly = 2) { mockEmailNotificationService.sendEmail(any(),any(),any()) }

    verify(exactly = 1) {
        mockEmailNotificationService.sendEmail(
          APPLICANT_EMAIL,
          notifyConfig.templates.bookingMade,
          match {
            it["name"] == applicant.name &&
              it["apName"] == premises.name &&
              (it["applicationUrl"] as String).matches(Regex("http://frontend/applications/${application.id}")) &&
              (it["bookingUrl"] as String).matches(Regex("http://frontend/premises/${premises.id}/bookings/${booking.id}")) &&
              it["crn"] == CRN &&
              it["startDate"] == "2023-02-01" &&
              it["endDate"] == "2023-02-14" &&
              (it["lengthStay"] as Int) == 2 &&
              it["lengthStayUnit"] as String == "weeks"
          },
        )
      }

    verify(exactly = 1) {
      mockEmailNotificationService.sendEmail(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingMadePremises,
        match {
          it["name"] == applicant.name &&
            it["apName"] == premises.name &&
            (it["applicationUrl"] as String).matches(Regex("http://frontend/applications/${application.id}")) &&
            (it["bookingUrl"] as String).matches(Regex("http://frontend/premises/${premises.id}/bookings/${booking.id}")) &&
            it["crn"] == CRN &&
            it["startDate"] == "2023-02-01" &&
            it["endDate"] == "2023-02-14" &&
            (it["lengthStay"] as Int) == 2 &&
            it["lengthStayUnit"] as String == "weeks"
        },
      )
    }
  }

  @Test
  fun `bookingMade sends email to applicant and premises email addresses when defined, when length of stay not whole number of weeks`() {
    val applicant = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withEmail(APPLICANT_EMAIL)
      .produce()

    val (application,booking) = createApplicationAndBooking(
      applicant,
      premises,
      arrivalDate = LocalDate.of(2023,2,22),
      departureDate = LocalDate.of(2023,2,27),
    )

    every { mockEmailNotificationService.sendEmail(any(),any(),any()) } returns Unit

    service.bookingMade(application, booking)

    verify(exactly = 2) { mockEmailNotificationService.sendEmail(any(),any(),any()) }

    verify(exactly = 1) {
      mockEmailNotificationService.sendEmail(
        APPLICANT_EMAIL,
        notifyConfig.templates.bookingMade,
        match {
          (it["lengthStay"] as Int) == 6 &&
          it["lengthStayUnit"] as String == "days"
        },
      )
    }

    verify(exactly = 1) {
      mockEmailNotificationService.sendEmail(
        PREMISES_EMAIL,
        notifyConfig.templates.bookingMadePremises,
        match {
          (it["lengthStay"] as Int) == 6 &&
          it["lengthStayUnit"] as String == "days"
        },
      )
    }
  }

  private fun createApplicationAndBooking(
    applicant: UserEntity,
    premises: ApprovedPremisesEntity,
    arrivalDate: LocalDate,
    departureDate: LocalDate): Pair<ApprovedPremisesApplicationEntity,BookingEntity> {

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCrn(CRN)
      .withCreatedByUser(applicant)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    val booking = BookingEntityFactory()
      .withApplication(application)
      .withPremises(premises)
      .withArrivalDate(arrivalDate)
      .withDepartureDate(departureDate)
      .produce()

    return Pair(application,booking)
  }
}

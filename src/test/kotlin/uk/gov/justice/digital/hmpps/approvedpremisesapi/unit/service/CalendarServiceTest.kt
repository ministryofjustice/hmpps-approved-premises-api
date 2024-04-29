package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarBookingInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarLostBedInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CalendarRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CalendarService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.LocalDate
import java.util.UUID

class CalendarServiceTest {
  private val offenderServiceMock = mockk<OffenderService>()
  private val calendarRepositoryMock = mockk<CalendarRepository>()

  private val calendarService = CalendarService(
    offenderServiceMock,
    calendarRepositoryMock,
  )

  @Test
  fun `getCalendarInfo returns results from repository for Lost Beds`() {
    val premisesId = UUID.fromString("4aa9e9f0-e240-4786-8f18-b331c6255fa7")
    val startDate = LocalDate.of(2023, 6, 9)
    val endDate = LocalDate.of(2023, 6, 15)
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    val lostBed = LostBedsEntityFactory()
      .withYieldedReason { LostBedReasonEntityFactory().produce() }
      .withPremises(premises)
      .withBed(bed)
      .produce()

    val repositoryResults = mapOf(
      CalendarBedInfo(bed.id, bed.name) to listOf(
        CalendarLostBedInfo(
          startDate = startDate,
          endDate = endDate,
          lostBedId = lostBed.id,
        ),
      ),
    )

    every { calendarRepositoryMock.getCalendarInfo(premisesId, startDate, endDate) } returns repositoryResults
    every { offenderServiceMock.getOffenderSummariesByCrns(emptySet(), user.deliusUsername, ignoreLaoRestrictions = false, forceApDeliusContextApi = false) } returns emptyList()

    val result = calendarService.getCalendarInfo(user, premisesId, startDate, endDate)

    assertThat(result).isEqualTo(repositoryResults)
  }

  @Test
  fun `getCalendarInfo sets personName to 'Unknown' on Bookings for Offenders where details could not be found`() {
    val premisesId = UUID.fromString("4aa9e9f0-e240-4786-8f18-b331c6255fa7")
    val startDate = LocalDate.of(2023, 6, 9)
    val endDate = LocalDate.of(2023, 6, 15)
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
    val crn = "CRN1"

    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withBed(bed)
      .withCrn(crn)
      .produce()

    every { calendarRepositoryMock.getCalendarInfo(premisesId, startDate, endDate) } answers {
      mapOf(
        CalendarBedInfo(bed.id, bed.name) to listOf(
          CalendarBookingInfo(
            startDate = startDate,
            endDate = endDate,
            bookingId = booking.id,
            crn = crn,
          ),
        ),
      )
    }

    every { offenderServiceMock.getOffenderSummariesByCrns(setOf(crn), user.deliusUsername, ignoreLaoRestrictions = false, forceApDeliusContextApi = false) } returns
      listOf(PersonSummaryInfoResult.NotFound(crn))

    every { offenderServiceMock.getOffenderByCrn(crn, user.deliusUsername) } returns AuthorisableActionResult.NotFound()

    val result = calendarService.getCalendarInfo(user, premisesId, startDate, endDate)

    assertThat(result).isEqualTo(
      mapOf(
        CalendarBedInfo(bed.id, bed.name) to listOf(
          CalendarBookingInfo(
            startDate = startDate,
            endDate = endDate,
            bookingId = booking.id,
            crn = crn,
            personName = "Unknown",
          ),
        ),
      ),
    )
  }

  @Test
  fun `getCalendarInfo sets personName to 'LAO Offender' on Bookings for Offenders where caller is not allowed to view details about the CRN`() {
    val premisesId = UUID.fromString("4aa9e9f0-e240-4786-8f18-b331c6255fa7")
    val startDate = LocalDate.of(2023, 6, 9)
    val endDate = LocalDate.of(2023, 6, 15)
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
    val crn = "CRN1"

    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withBed(bed)
      .withCrn(crn)
      .produce()

    every { calendarRepositoryMock.getCalendarInfo(premisesId, startDate, endDate) } answers {
      mapOf(
        CalendarBedInfo(bed.id, bed.name) to listOf(
          CalendarBookingInfo(
            startDate = startDate,
            endDate = endDate,
            bookingId = booking.id,
            crn = crn,
          ),
        ),
      )
    }

    every { offenderServiceMock.getOffenderSummariesByCrns(setOf(crn), user.deliusUsername, ignoreLaoRestrictions = false, forceApDeliusContextApi = false) } returns
      listOf(PersonSummaryInfoResult.Success.Restricted(crn, "noms"))

    val result = calendarService.getCalendarInfo(user, premisesId, startDate, endDate)

    assertThat(result).isEqualTo(
      mapOf(
        CalendarBedInfo(bed.id, bed.name) to listOf(
          CalendarBookingInfo(
            startDate = startDate,
            endDate = endDate,
            bookingId = booking.id,
            crn = crn,
            personName = "Limited Access Offender",
          ),
        ),
      ),
    )
  }

  @Test
  fun `getCalendarInfo sets personName to First Name - Last Name on Bookings for Offenders where info can be fetched`() {
    val premisesId = UUID.fromString("4aa9e9f0-e240-4786-8f18-b331c6255fa7")
    val startDate = LocalDate.of(2023, 6, 9)
    val endDate = LocalDate.of(2023, 6, 15)
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()
    val crn = "CRN1"

    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withBed(bed)
      .withCrn(crn)
      .produce()

    every { calendarRepositoryMock.getCalendarInfo(premisesId, startDate, endDate) } answers {
      mapOf(
        CalendarBedInfo(bed.id, bed.name) to listOf(
          CalendarBookingInfo(
            startDate = startDate,
            endDate = endDate,
            bookingId = booking.id,
            crn = crn,
          ),
        ),
      )
    }

    every { offenderServiceMock.getOffenderSummariesByCrns(setOf(crn), user.deliusUsername, ignoreLaoRestrictions = false, forceApDeliusContextApi = false) } returns
      listOf(
        PersonSummaryInfoResult.Success.Full(
          crn,
          CaseSummaryFactory().withName(NameFactory().withForename("Firstname").withSurname("Lastname").produce()).produce(),
        ),
      )

    val result = calendarService.getCalendarInfo(user, premisesId, startDate, endDate)

    assertThat(result).isEqualTo(
      mapOf(
        CalendarBedInfo(bed.id, bed.name) to listOf(
          CalendarBookingInfo(
            startDate = startDate,
            endDate = endDate,
            bookingId = booking.id,
            crn = crn,
            personName = "Firstname Lastname",
          ),
        ),
      ),
    )
  }
}

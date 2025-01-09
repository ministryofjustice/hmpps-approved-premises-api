package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingSummaryForAvailabilityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.TimeService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd
import java.time.LocalDate

class PremisesServiceTest {
  private val premisesRepositoryMock = mockk<PremisesRepository>()
  private val cs3VoidBedspacesRepositoryMock = mockk<Cas3VoidBedspacesRepository>()
  private val bookingRepositoryMock = mockk<BookingRepository>()
  private val localAuthorityAreaRepositoryMock = mockk<LocalAuthorityAreaRepository>()
  private val probationRegionRepositoryMock = mockk<ProbationRegionRepository>()
  private val probationDeliveryUnitRepositoryMock = mockk<ProbationDeliveryUnitRepository>()
  private val characteristicServiceMock = mockk<CharacteristicService>()
  private val roomRepositoryMock = mockk<RoomRepository>()
  private val bedRepositoryMock = mockk<BedRepository>()
  private val timeService = mockk<TimeService>()

  private val approvedPremisesFactory = ApprovedPremisesEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }

  private val premisesService = PremisesService(
    premisesRepositoryMock,
    cs3VoidBedspacesRepositoryMock,
    bookingRepositoryMock,
    localAuthorityAreaRepositoryMock,
    probationRegionRepositoryMock,
    probationDeliveryUnitRepositoryMock,
    characteristicServiceMock,
    roomRepositoryMock,
    bedRepositoryMock,
    timeService,
  )

  @Test
  fun `getAvailabilityForRange returns correctly when there are no bookings or void bedspaces`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(3)

    val premises = approvedPremisesFactory.produce()

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf()
    every { cs3VoidBedspacesRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf()

    val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

    assertThat(result).containsValues(
      Availability(date = startDate, 0, 0, 0, 0, 0),
      Availability(date = startDate.plusDays(1), 0, 0, 0, 0, 0),
      Availability(date = startDate.plusDays(2), 0, 0, 0, 0, 0),
    )
  }

  @Test
  fun `getAvailabilityForRange returns correctly when there are bookings`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(6)

    val premises = approvedPremisesFactory.produce()

    val voidBedspaceEntityOne = Cas3VoidBedspaceEntityFactory()
      .withPremises(premises)
      .withStartDate(startDate.plusDays(1))
      .withEndDate(startDate.plusDays(2))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withBed(
        BedEntityFactory().apply {
          withYieldedRoom {
            RoomEntityFactory().apply {
              withYieldedPremises { premises }
            }.produce()
          }
        }.produce(),
      )
      .produce()

    val voidBedspaceEntityTwo = Cas3VoidBedspaceEntityFactory()
      .withPremises(premises)
      .withStartDate(startDate.plusDays(1))
      .withEndDate(startDate.plusDays(2))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withBed(
        BedEntityFactory().apply {
          withYieldedRoom {
            RoomEntityFactory().apply {
              withYieldedPremises { premises }
            }.produce()
          }
        }.produce(),
      )
      .produce()

    val pendingBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate.plusDays(1))
      .withDepartureDate(startDate.plusDays(3))
      .withArrived(false)
      .withCancelled(false)
      .withIsNotArrived(false)
      .produce()

    val arrivedBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate)
      .withDepartureDate(startDate.plusDays(2))
      .withArrived(true)
      .withCancelled(false)
      .withIsNotArrived(false)
      .produce()

    val nonArrivedBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate.plusDays(3))
      .withDepartureDate(startDate.plusDays(5))
      .withArrived(false)
      .withCancelled(false)
      .withIsNotArrived(true)
      .produce()

    val cancelledBookingEntity = BookingSummaryForAvailabilityFactory()
      .withArrivalDate(startDate.plusDays(4))
      .withDepartureDate(startDate.plusDays(6))
      .withArrived(false)
      .withCancelled(true)
      .withIsNotArrived(false)
      .produce()

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      pendingBookingEntity,
      arrivedBookingEntity,
      nonArrivedBookingEntity,
      cancelledBookingEntity,
    )
    every { cs3VoidBedspacesRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      voidBedspaceEntityOne,
      voidBedspaceEntityTwo,
    )

    val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

    assertThat(result).containsValues(
      Availability(date = startDate, pendingBookings = 0, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
      Availability(date = startDate.plusDays(1), pendingBookings = 1, arrivedBookings = 1, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 2),
      Availability(date = startDate.plusDays(2), pendingBookings = 1, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
      Availability(date = startDate.plusDays(3), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 0, voidBedspaces = 0),
      Availability(date = startDate.plusDays(4), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 1, cancelledBookings = 1, voidBedspaces = 0),
      Availability(date = startDate.plusDays(5), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 1, voidBedspaces = 0),
    )
  }

  @Test
  fun `getAvailabilityForRange returns correctly when there are cancelled void bedspaces`() {
    val startDate = LocalDate.now()
    val endDate = LocalDate.now().plusDays(6)

    val premises = approvedPremisesFactory.produce()

    val voidBedspaceEntity = Cas3VoidBedspaceEntityFactory()
      .withPremises(premises)
      .withStartDate(startDate.plusDays(1))
      .withEndDate(startDate.plusDays(2))
      .withYieldedReason { Cas3VoidBedspaceReasonEntityFactory().produce() }
      .withBed(
        BedEntityFactory().apply {
          withYieldedRoom {
            RoomEntityFactory().apply {
              withYieldedPremises { premises }
            }.produce()
          }
        }.produce(),
      )
      .produce()

    val lostBedCancellation = Cas3VoidBedspaceCancellationEntityFactory()
      .withYieldedVoidBedspace { voidBedspaceEntity }
      .produce()

    voidBedspaceEntity.cancellation = lostBedCancellation

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf()
    every { cs3VoidBedspacesRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate) } returns mutableListOf(
      voidBedspaceEntity,
    )

    val result = premisesService.getAvailabilityForRange(premises, startDate, endDate)

    assertThat(result).containsValues(
      Availability(date = startDate, pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
      Availability(date = startDate.plusDays(1), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
      Availability(date = startDate.plusDays(2), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
      Availability(date = startDate.plusDays(3), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
      Availability(date = startDate.plusDays(4), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
      Availability(date = startDate.plusDays(5), pendingBookings = 0, arrivedBookings = 0, nonArrivedBookings = 0, cancelledBookings = 0, voidBedspaces = 0),
    )
  }

  @Test
  fun `getDateCapacities looks ahead a maximum of one year in the future`() {
    val premises = approvedPremisesFactory.produce()

    val today = LocalDate.of(2020, 2, 28)

    every { timeService.nowAsLocalDate() } returns today

    val ninetyNineYearsFromNow = today.plusYears(99)
    val oneYearFromNow = today.plusYears(1)

    every { premisesService.getLastBookingDate(premises) } answers { ninetyNineYearsFromNow }
    every { premisesService.getLastLostBedsDate(premises) } answers { ninetyNineYearsFromNow }
    every { premisesService.getBedCount(premises) } answers { 30 }

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, today, oneYearFromNow) } answers { emptyList() }
    every { cs3VoidBedspacesRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, today, oneYearFromNow) } answers { emptyList() }

    val result = premisesService.getDateCapacities(premises)

    assertThat(result.size).isEqualTo(366)

    assertThat(
      result.map { it.date },
    ).isEqualTo(
      today.getDaysUntilExclusiveEnd(oneYearFromNow),
    )
  }

  @Test
  fun `getDateCapacities uses the getLastLostBedsDate if it is the latest date and less than one year ago`() {
    val premises = approvedPremisesFactory.produce()

    val today = LocalDate.of(2020, 2, 28)

    every { timeService.nowAsLocalDate() } returns today

    val fourMonthsFromNow = today.plusMonths(4)

    every { premisesService.getLastBookingDate(premises) } answers { today.plusWeeks(2) }
    every { premisesService.getLastLostBedsDate(premises) } answers { fourMonthsFromNow }
    every { premisesService.getBedCount(premises) } answers { 30 }

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, today, fourMonthsFromNow) } answers { emptyList() }
    every { cs3VoidBedspacesRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, today, fourMonthsFromNow) } answers { emptyList() }

    val result = premisesService.getDateCapacities(premises)

    assertThat(
      result.map { it.date },
    ).isEqualTo(
      today.getDaysUntilExclusiveEnd(fourMonthsFromNow),
    )
  }

  @Test
  fun `getDateCapacities uses the getLastBookingDate if it is the latest date and less than one year ago`() {
    val premises = approvedPremisesFactory.produce()

    val today = LocalDate.of(2020, 2, 28)

    every { timeService.nowAsLocalDate() } returns today

    val fourMonthsFromNow = today.plusMonths(4)

    every { premisesService.getLastBookingDate(premises) } answers { fourMonthsFromNow }
    every { premisesService.getLastLostBedsDate(premises) } answers { today.plusWeeks(2) }
    every { premisesService.getBedCount(premises) } answers { 30 }

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, today, fourMonthsFromNow) } answers { emptyList() }
    every { cs3VoidBedspacesRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, today, fourMonthsFromNow) } answers { emptyList() }

    val result = premisesService.getDateCapacities(premises)

    assertThat(
      result.map { it.date },
    ).isEqualTo(
      today.getDaysUntilExclusiveEnd(fourMonthsFromNow),
    )
  }

  @Test
  fun `getDateCapacities prioritises the lastBookingDate if the lastLostBedsDate is null`() {
    val premises = approvedPremisesFactory.produce()

    val today = LocalDate.of(2020, 2, 28)

    every { timeService.nowAsLocalDate() } returns today

    val fourMonthsFromNow = today.plusMonths(4)

    every { premisesService.getLastBookingDate(premises) } answers { fourMonthsFromNow }
    every { premisesService.getLastLostBedsDate(premises) } answers { null }
    every { premisesService.getBedCount(premises) } answers { 30 }

    every { bookingRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, today, fourMonthsFromNow) } answers { emptyList() }
    every { cs3VoidBedspacesRepositoryMock.findAllByPremisesIdAndOverlappingDate(premises.id, today, fourMonthsFromNow) } answers { emptyList() }

    val result = premisesService.getDateCapacities(premises)

    assertThat(
      result.map { it.date },
    ).isEqualTo(
      today.getDaysUntilExclusiveEnd(fourMonthsFromNow),
    )
  }
}

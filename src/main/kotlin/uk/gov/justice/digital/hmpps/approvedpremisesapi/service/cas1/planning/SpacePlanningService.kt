package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1BedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository.OutOfServiceBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1PlanningBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate
import java.util.UUID

@Service
class SpacePlanningService(
  private val spacePlanningModelsFactory: SpacePlanningModelsFactory,
  private val outOfServiceBedService: Cas1OutOfServiceBedService,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1BedsRepository: Cas1BedsRepository,
) {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun capacity(
    premisesId: UUID,
    rangeInclusive: DateRange,
    excludeSpaceBookingId: UUID?,
  ) = capacity(listOf(premisesId), rangeInclusive, excludeSpaceBookingId)[0]

  fun capacity(
    forPremisesIds: List<UUID>,
    rangeInclusive: DateRange,
    excludeSpaceBookingId: UUID?,
  ): List<PremiseCapacity> {
    val rangeList = rangeInclusive.orderedDatesInRange().toList()

    val allPremisesDayBedStates = bedStatesForEachDay(forPremisesIds, rangeList)
    val allPremisesDayBookings = spaceBookingsForEachDay(forPremisesIds, rangeList, excludeSpaceBookingId)

    return forPremisesIds.map { premisesId ->
      calculatePremisesCapacity(
        premisesId = premisesId,
        rangeInclusive = rangeInclusive,
        premisesDayBedStates = allPremisesDayBedStates.forPremises(premisesId),
        premisesDayBookings = allPremisesDayBookings.forPremises(premisesId),
      )
    }
  }

  private fun calculatePremisesCapacity(
    premisesId: UUID,
    rangeInclusive: DateRange,
    premisesDayBedStates: PremisesDayBedStates,
    premisesDayBookings: PremisesDayBookings,
  ): PremiseCapacity {
    val capacityForEachDay = rangeInclusive.orderedDatesInRange().map { day ->
      val bedStates = premisesDayBedStates.forDay(day)
      val bookings = premisesDayBookings.forDay(day)

      val availableBeds = bedStates.findActive()
      PremiseCapacityForDay(
        day = day,
        totalBedCount = bedStates.size,
        availableBedCount = availableBeds.size,
        bookingCount = bookings.size,
        characteristicAvailability = Cas1SpaceBookingEntity.ROOM_CHARACTERISTICS_OF_INTEREST.map {
          determineCharacteristicAvailability(
            characteristicPropertyName = it,
            availableBeds = availableBeds,
            bookings = bookings,
          )
        },
      )
    }.toList()

    return PremiseCapacity(
      premisesId = premisesId,
      range = rangeInclusive,
      byDay = capacityForEachDay,
    )
  }

  private fun determineCharacteristicAvailability(
    characteristicPropertyName: String,
    availableBeds: List<BedDayState>,
    bookings: List<Cas1SpaceBookingEntity>,
  ) = PremiseCharacteristicAvailability(
    characteristicPropertyName = characteristicPropertyName,
    availableBedCount = availableBeds.count { it.bed.characteristicsPropertyNames.contains(characteristicPropertyName) },
    bookingCount = bookings.count { booking -> booking.criteria.any { it.propertyName == characteristicPropertyName } },
  )

  private fun bedStatesForEachDay(
    forPremisesIds: List<UUID>,
    orderedRange: List<LocalDate>,
  ): List<PremisesDayBedStates> {
    val allPremisesOosbRecords = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesIds(forPremisesIds)
    val allPremisesBeds = cas1BedsRepository.bedSummary(
      premisesIds = forPremisesIds,
      excludeEndedBeds = false,
    )

    return forPremisesIds.map { premisesId ->

      val premisesBeds = allPremisesBeds.bedsForPremises(premisesId)
      val premisesOosbRecords = allPremisesOosbRecords.oosbForPremises(premisesId)

      PremisesDayBedStates(
        premisesId = premisesId,
        dayBedStates = orderedRange.map { day ->
          DayBedStates(
            date = day,
            bedStates = spacePlanningModelsFactory.allBedsDayState(
              day = day,
              beds = premisesBeds,
              outOfServiceBedRecordsToConsider = premisesOosbRecords,
            ),
          )
        }.toList(),
      )
    }
  }

  private fun spaceBookingsForEachDay(
    forPremisesIds: List<UUID>,
    orderedRange: List<LocalDate>,
    excludeSpaceBookingId: UUID? = null,
  ): List<PremisesDayBookings> {
    val allPremisesBookingsInRange = spaceBookingRepository.findNonCancelledBookingsInRange(
      premisesIds = forPremisesIds,
      rangeStartInclusive = orderedRange.first(),
      rangeEndInclusive = orderedRange.last(),
    ).filter { it.id != excludeSpaceBookingId }

    return forPremisesIds.map { premisesId ->
      val premisesBookingsInRange = allPremisesBookingsInRange.bookingsForPremises(premisesId)

      PremisesDayBookings(
        premisesId = premisesId,
        dayBookings = orderedRange.map { day ->
          DayBookings(
            date = day,
            bookings = spacePlanningModelsFactory.spaceBookingsForDay(
              day = day,
              spaceBookingsToConsider = premisesBookingsInRange,
            ),
          )
        }.toList(),
      )
    }
  }

  data class PremiseCapacity(
    val premisesId: UUID,
    val range: DateRange,
    val byDay: List<PremiseCapacityForDay>,
  )

  data class PremiseCapacityForDay(
    val day: LocalDate,
    val totalBedCount: Int,
    val availableBedCount: Int,
    val bookingCount: Int,
    val characteristicAvailability: List<PremiseCharacteristicAvailability>,
  ) {
    fun isPremiseOverbooked(): Boolean = isPremisesCapacityOverbooked() || characteristicAvailability.any { it.isCharacteristicOverbooked() }

    private fun isPremisesCapacityOverbooked(): Boolean = bookingCount > availableBedCount
  }

  data class PremiseCharacteristicAvailability(
    val characteristicPropertyName: String,
    val availableBedCount: Int,
    val bookingCount: Int,
  ) {

    fun isCharacteristicOverbooked(): Boolean = bookingCount > availableBedCount
  }

  private data class PremisesDayBedStates(
    val premisesId: UUID,
    val dayBedStates: List<DayBedStates>,
  ) {
    fun forDay(day: LocalDate) = dayBedStates.first { it.date == day }.bedStates
  }

  private fun List<PremisesDayBedStates>.forPremises(premisesId: UUID) = this.first { it.premisesId == premisesId }

  private data class DayBedStates(
    val date: LocalDate,
    val bedStates: List<BedDayState>,
  )

  private fun List<BedDayState>.findActive() = this.filter { it.isActive() }

  private data class PremisesDayBookings(
    val premisesId: UUID,
    val dayBookings: List<DayBookings>,
  ) {
    fun forDay(day: LocalDate) = dayBookings.first { it.date == day }.bookings
  }

  private fun List<PremisesDayBookings>.forPremises(premisesId: UUID) = this.first { it.premisesId == premisesId }

  private data class DayBookings(
    val date: LocalDate,
    val bookings: List<Cas1SpaceBookingEntity>,
  )

  private fun List<Cas1SpaceBookingEntity>.bookingsForPremises(premisesId: UUID) = filter { it.premises.id == premisesId }
  private fun List<Cas1PlanningBedSummary>.bedsForPremises(premisesId: UUID) = filter { it.premisesId == premisesId }
  private fun List<OutOfServiceBedSummary>.oosbForPremises(premisesId: UUID) = filter { it.getPremisesId() == premisesId }
}

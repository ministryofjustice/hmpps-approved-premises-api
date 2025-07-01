package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1BedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate
import java.util.UUID

@Service
class SpacePlanningService(
  private val spacePlanningModelsFactory: SpacePlanningModelsFactory,
  private val spaceBookingDayPlanner: SpaceBookingDayPlanner,
  private val outOfServiceBedService: Cas1OutOfServiceBedService,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
  private val cas1BedsRepository: Cas1BedsRepository,
) {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  data class PlanCriteria(
    val premises: ApprovedPremisesEntity,
    val range: DateRange,
  )

  fun plan(criteria: PlanCriteria): SpacePlan {
    val premises = criteria.premises
    val range = criteria.range

    val bedStatesForEachDay = bedStatesForEachDay(premises, range)
    val bookingsForEachDay = spaceBookingsForEachDay(premises, range)

    val dayPlans = range.orderedDatesInRange().map { day ->
      val bedStates = bedStatesForEachDay.forDay(day).bedStates
      val availableBeds = bedStates.filter { it.isActive() }.map { it.bed }.toSet()
      val bookings = bookingsForEachDay.forDay(day).bookings.toSet()

      SpaceDayPlan(
        day = day,
        bedStates = bedStates,
        planningResult = spaceBookingDayPlanner.plan(
          availableBeds,
          bookings,
        ),
      )
    }.toList()

    val beds = cas1BedsRepository.bedSummary(premises.id)
    val plan = SpacePlan(
      beds = spacePlanningModelsFactory.allBeds(beds),
      dayPlans = dayPlans,
    )

    log.debug(SpacePlanRenderer.render(criteria, plan))

    return plan
  }

  fun capacity(
    premises: ApprovedPremisesEntity,
    rangeInclusive: DateRange,
    excludeSpaceBookingId: UUID?,
  ): PremiseCapacitySummary {
    val bedStatesForEachDay = bedStatesForEachDay(premises, rangeInclusive)
    val bookingsForEachDay = spaceBookingsForEachDay(premises, rangeInclusive, excludeSpaceBookingId)

    val capacityForEachDay = rangeInclusive.orderedDatesInRange().map { day ->
      val bedStates = bedStatesForEachDay.forDay(day).bedStates
      val availableBeds = bedStates.findActive()
      val bookings = bookingsForEachDay.forDay(day).bookings
      PremiseCapacityForDay(
        day = day,
        totalBedCount = bedStates.totalBedCount(),
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

    return PremiseCapacitySummary(
      premise = premises,
      range = rangeInclusive,
      byDay = capacityForEachDay,
    )
  }

  private fun determineCharacteristicAvailability(
    characteristicPropertyName: String,
    availableBeds: List<BedDayState>,
    bookings: List<SpaceBooking>,
  ): PremiseCharacteristicAvailability = PremiseCharacteristicAvailability(
    characteristicPropertyName = characteristicPropertyName,
    availableBedCount = availableBeds.count { it.bed.hasCharacteristic(characteristicPropertyName) },
    bookingCount = bookings.count { it.hasCharacteristic(characteristicPropertyName) },
  )

  private fun bedStatesForEachDay(
    premises: ApprovedPremisesEntity,
    range: DateRange,
  ): List<DayBedStates> {
    val outOfServiceBedRecordsToConsider = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premises.id)
    val beds = cas1BedsRepository.bedSummary(premises.id)

    return range.orderedDatesInRange()
      .toList()
      .map {
        DayBedStates(
          date = it,
          bedStates = spacePlanningModelsFactory.allBedsDayState(
            day = it,
            beds = beds,
            outOfServiceBedRecordsToConsider = outOfServiceBedRecordsToConsider,
          ),
        )
      }
  }

  private fun spaceBookingsForEachDay(
    premises: ApprovedPremisesEntity,
    range: DateRange,
    excludeSpaceBookingId: UUID? = null,
  ): List<DayBookings> {
    val spaceBookingsToConsider = spaceBookingRepository.findNonCancelledBookingsInRange(
      premisesId = premises.id,
      rangeStartInclusive = range.fromInclusive,
      rangeEndInclusive = range.toInclusive,
    ).filter { it.id != excludeSpaceBookingId }

    return range.orderedDatesInRange()
      .toList()
      .map {
        DayBookings(
          date = it,
          spacePlanningModelsFactory.spaceBookingsForDay(
            day = it,
            spaceBookingsToConsider = spaceBookingsToConsider,
          )
        )
      }
  }

  data class SpacePlan(
    val beds: List<Bed>,
    val dayPlans: List<SpaceDayPlan>,
  )

  data class SpaceDayPlan(
    val day: LocalDate,
    val bedStates: List<BedDayState>,
    val planningResult: DayPlannerResult,
  )

  data class PremiseCapacitySummary(
    val premise: PremisesEntity,
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

  private data class DayBedStates(
    val date: LocalDate,
    val bedStates: List<BedDayState>,
  )

  private fun List<DayBedStates>.forDay(day: LocalDate) = this.first { it.date == day }
  private fun List<BedDayState>.findActive() = this.filter { it.isActive() }
  private fun List<BedDayState>.totalBedCount() = this.count { it.isActive() || it.isTemporarilyInactive() }

  private data class DayBookings(
    val date: LocalDate,
    val bookings: List<SpaceBooking>,
  )

  private fun List<DayBookings>.forDay(day: LocalDate) = this.first { it.date == day }
}

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

    val bedStatesForEachDay = bedStatesForEachDay(listOf(premises), range).forPremises(premises)
    val bookingsForEachDay = spaceBookingsForEachDay(listOf(premises), range).forPremises(premises)

    val dayPlans = range.orderedDatesInRange().map { day ->
      val bedStates = bedStatesForEachDay.forDay(day)
      val availableBeds = bedStates.filter { it.isActive() }.map { it.bed }.toSet()
      val bookings = bookingsForEachDay.forDay(day).toSet()

      SpaceDayPlan(
        day = day,
        bedStates = bedStates,
        planningResult = spaceBookingDayPlanner.plan(
          availableBeds,
          bookings,
        ),
      )
    }.toList()

    val beds = cas1BedsRepository.bedSummary(listOf(premises.id))
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
  ) = capacity(listOf(premises), rangeInclusive, excludeSpaceBookingId)[0]

  fun capacity(
    forPremises: List<ApprovedPremisesEntity>,
    rangeInclusive: DateRange,
    excludeSpaceBookingId: UUID?,
  ): List<PremiseCapacitySummary> {
    val dayBedStates = bedStatesForEachDay(forPremises, rangeInclusive)
    val dayBookings = spaceBookingsForEachDay(forPremises, rangeInclusive, excludeSpaceBookingId)

    return forPremises.map { premises ->
      premisesCapacity(
        premises,
        rangeInclusive,
        dayBedStates.forPremises(premises),
        dayBookings.forPremises(premises),
      )
    }
  }

  private fun premisesCapacity(
    premises: ApprovedPremisesEntity,
    rangeInclusive: DateRange,
    dayBedStates: PremisesDayBedStates,
    dayBookings: PremisesDayBookings,
  ): PremiseCapacitySummary {
    val capacityForEachDay = rangeInclusive.orderedDatesInRange().map { day ->
      val bedStates = dayBedStates.forDay(day)
      val availableBeds = bedStates.findActive()
      val bookings = dayBookings.forDay(day)
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
    forPremises: List<ApprovedPremisesEntity>,
    range: DateRange,
  ): List<PremisesDayBedStates> {
    val allPremisesIds = forPremises.map { it.id }
    val outOfServiceBedRecordsToConsider = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesIds(allPremisesIds)
    val beds = cas1BedsRepository.bedSummary(allPremisesIds)

    return forPremises.map { premises ->
      PremisesDayBedStates(
        premises = premises,
        bedStates = range.orderedDatesInRange()
          .toList()
          .map { day ->
            DayBedStates(
              date = day,
              bedStates = spacePlanningModelsFactory.allBedsDayState(
                day = day,
                beds = beds.filter { it.premisesId == premises.id },
                outOfServiceBedRecordsToConsider = outOfServiceBedRecordsToConsider.filter { it.premises.id == premises.id },
              ),
            )
          },
      )
    }
  }

  private fun spaceBookingsForEachDay(
    forPremises: List<ApprovedPremisesEntity>,
    range: DateRange,
    excludeSpaceBookingId: UUID? = null,
  ): List<PremisesDayBookings> {
    val nonCancelledBookingsInRange = spaceBookingRepository.findNonCancelledBookingsInRange(
      premisesIds = forPremises.map { it.id },
      rangeStartInclusive = range.fromInclusive,
      rangeEndInclusive = range.toInclusive,
    ).filter { it.id != excludeSpaceBookingId }

    return forPremises.map { premises ->
      PremisesDayBookings(
        premises = premises,
        dayBookings = range.orderedDatesInRange()
          .toList()
          .map { day ->
            DayBookings(
              date = day,
              spacePlanningModelsFactory.spaceBookingsForDay(
                day = day,
                spaceBookingsToConsider = nonCancelledBookingsInRange.filter { it.premises.id == premises.id },
              ),
            )
          },
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

  private data class PremisesDayBedStates(
    val premises: ApprovedPremisesEntity,
    val bedStates: List<DayBedStates>,
  ) {
    fun forDay(day: LocalDate) = bedStates.first { it.date == day }.bedStates
  }

  private fun List<PremisesDayBedStates>.forPremises(premises: ApprovedPremisesEntity) = this.first { it.premises.id == premises.id }

  private data class DayBedStates(
    val date: LocalDate,
    val bedStates: List<BedDayState>,
  )

  private fun List<BedDayState>.findActive() = this.filter { it.isActive() }
  private fun List<BedDayState>.totalBedCount() = this.count { it.isActive() || it.isTemporarilyInactive() }

  private data class PremisesDayBookings(
    val premises: ApprovedPremisesEntity,
    val dayBookings: List<DayBookings>,
  ) {
    fun forDay(day: LocalDate) = dayBookings.first { it.date == day }.bookings
  }

  private fun List<PremisesDayBookings>.forPremises(premises: ApprovedPremisesEntity) = this.first { it.premises.id == premises.id }

  private data class DayBookings(
    val date: LocalDate,
    val bookings: List<SpaceBooking>,
  )
}

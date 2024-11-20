package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate

@Service
class SpacePlanningService(
  private val spacePlanningModelsFactory: SpacePlanningModelsFactory,
  private val spaceBookingDayPlanner: SpaceBookingDayPlanner,
  private val outOfServiceBedService: Cas1OutOfServiceBedService,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
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
      val bedStates = bedStatesForEachDay[day]!!
      val availableBeds = bedStates.filter { it.isActive() }.map { it.bed }.toSet()
      val bookings = bookingsForEachDay[day]!!.toSet()

      SpaceDayPlan(
        day = day,
        bedStates = bedStates,
        planningResult = spaceBookingDayPlanner.plan(
          availableBeds,
          bookings,
        ),
      )
    }.toList()

    val plan = SpacePlan(
      beds = spacePlanningModelsFactory.allBeds(premises),
      dayPlans = dayPlans,
    )

    log.debug(SpacePlanRenderer.render(criteria, plan))

    return plan
  }

  fun bedStatesForEachDay(
    premises: ApprovedPremisesEntity,
    range: DateRange,
  ): Map<LocalDate, List<BedDayState>> {
    val outOfServiceBedRecordsToConsider = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premises.id)

    return range.orderedDatesInRange()
      .toList()
      .associateBy(
        keySelector = { it },
        valueTransform = { day ->
          spacePlanningModelsFactory.allBedsDayState(
            day = day,
            premises = premises,
            outOfServiceBedRecordsToConsider = outOfServiceBedRecordsToConsider,
          )
        },
      )
  }

  fun spaceBookingsForEachDay(
    premises: ApprovedPremisesEntity,
    range: DateRange,
  ): Map<LocalDate, List<SpaceBooking>> {
    val spaceBookingsToConsider = spaceBookingRepository.findAllBookingsActiveWithinAGivenRangeWithCriteria(
      premisesId = premises.id,
      rangeStartInclusive = range.fromInclusive,
      rangeEndInclusive = range.toInclusive,
    )

    return range.orderedDatesInRange()
      .toList()
      .associateBy(
        keySelector = { it },
        valueTransform = { day ->
          spacePlanningModelsFactory.spaceBookingsForDay(
            day = day,
            spaceBookingsToConsider = spaceBookingsToConsider,
          )
        },
      )
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
}

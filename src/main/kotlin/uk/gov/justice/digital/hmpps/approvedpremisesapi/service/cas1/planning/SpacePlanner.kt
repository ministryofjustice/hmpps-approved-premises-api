package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import java.time.LocalDate

@Service
class SpacePlanner(
  private val spacePlanningModelsFactory: SpacePlanningModelsFactory,
  private val spaceBookingDayPlanner: SpaceBookingDayPlanner,
  private val outOfServiceBedService: Cas1OutOfServiceBedService,
) {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  data class PlanCriteria(
    val premises: ApprovedPremisesEntity,
    val startDate: LocalDate,
    val endDate: LocalDate,
  )

  fun plan(criteria: PlanCriteria): SpacePlan {
    val premises = criteria.premises
    val allBeds = spacePlanningModelsFactory.allBeds(premises)
    val outOfServiceBedRecords = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premises.id)

    val daysToPlan = criteria.startDate.datesUntil(criteria.endDate.plusDays(1))

    val bedDayStatesForEachDay = daysToPlan.map { dayToPlan ->
      Pair(
        dayToPlan,
        spacePlanningModelsFactory.allBedsDayState(
          day = dayToPlan,
          premises = premises,
          outOfServiceBedRecords = outOfServiceBedRecords,
        ),
      )
    }

    val dayPlans = bedDayStatesForEachDay.map { (day, bedDayStates) ->
      val availableBeds = bedDayStates.filter { it.isActive() }.map { it.bed }.toSet()
      val bookings = spacePlanningModelsFactory.spaceBookingsForDay(day, premises).toSet()

      SpaceDayPlan(
        day = day,
        bedStates = bedDayStates,
        planningResult = spaceBookingDayPlanner.plan(
          availableBeds,
          bookings,
        ),
      )
    }.toList()

    val plan = SpacePlan(
      beds = allBeds,
      dayPlans = dayPlans,
    )

    log.debug(SpacePlanRenderer.render(criteria, plan))

    return plan
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

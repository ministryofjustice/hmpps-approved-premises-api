package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanRenderer.CONSTANTS.DAY_COL_WIDTH
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanRenderer.CONSTANTS.ROOM_COL_WIDTH
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.SpaceDayPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.SpacePlan

/**
 * We render results in markdown because it's user readable and also easy to format into a neat table using IDE tools,
 * which can then be copied into excel etc. for further analysis
 */
object SpacePlanRenderer {

  private object CONSTANTS {
    const val ROOM_COL_WIDTH = 20
    const val DAY_COL_WIDTH = 70
  }

  @SuppressWarnings("MagicNumber")
  fun render(
    criteria: SpacePlanningService.PlanCriteria,
    plan: SpacePlan,
  ): String {
    val output = StringBuilder()
    val premises = criteria.premises

    output.appendLine("Space Plan for ${premises.name} (${premises.id}) from ${criteria.range.fromInclusive} to ${criteria.range.toInclusive}")
    output.appendLine()

    val daysWithUnplannedBookings = plan.dayPlans.filter { it.planningResult.unplanned.isNotEmpty() }

    if (daysWithUnplannedBookings.isNotEmpty()) {
      output.appendLine("There are ${daysWithUnplannedBookings.size} days with unplanned bookings:")
      output.appendLine()
      daysWithUnplannedBookings.forEach { unplannedDay ->
        output.appendLine("${unplannedDay.day} has ${unplannedDay.planningResult.unplanned.size}")
      }
      output.appendLine()
    }

    val headers =
      listOf(roomsHeader(plan)) +
        plan.dayPlans.map { dayHeader(it) }

    val colWidths = listOf(ROOM_COL_WIDTH) + plan.dayPlans.map { DAY_COL_WIDTH }

    val bedsBody = plan.beds
      .sortedBy { it.label }
      .map { bed ->
        listOf(
          bedDescription(bed),
        ) +
          plan.dayPlans.map { dayPlan ->
            val bedState = dayPlan.bedStates.first { it.bed == bed }
            val bedBooking = dayPlan.planningResult.plan.firstOrNull { it.bed == bed }
            bedDayDescription(bedState, bedBooking?.booking)
          }
      }

    val unplannedRow =
      listOf("unplanned".bold()) +
        plan.dayPlans.map { dayPlan ->
          dayPlan.planningResult.unplanned.joinToString(separator = "<br/><br/>") { unplannedBookingDescription(it) }
        }

    output.append(
      MarkdownTableRenderer.render(
        headers,
        bedsBody + listOf(unplannedRow),
        colWidths,
      ),
    )

    return output.toString()
  }

  private fun roomsHeader(plan: SpacePlan) = "Room (${plan.beds.size})"

  private fun dayHeader(plan: SpaceDayPlan): String {
    val result = StringBuilder()
    result.append(plan.day.toString().bold())
    result.append("<br/>")

    val activeBeds = plan.bedStates.count { it.isActive() }
    result.append("capacity: $activeBeds<br/>")

    val planned = plan.planningResult.planned.size
    result.append("planned: $planned<br/>")

    val unplanned = plan.planningResult.unplanned.size
    result.append("unplanned: $unplanned")

    return result.toString()
  }

  private fun bedDayDescription(
    bedState: BedDayState,
    booking: SpaceBooking?,
  ): String {
    return if (!bedState.isActive()) {
      when (val inactiveReason = bedState.inactiveReason!!) {
        is BedEnded -> return "Bed Ended ${inactiveReason.ended}"
        is BedOutOfService -> return "OOSB ${inactiveReason.reason}"
      }
    } else if (booking != null) {
      val bookingDescription = StringBuilder()

      bookingDescription.append(booking.label.bold())

      val characteristicSummary = SpaceDayPlanRenderer.characteristicsMatchingSummary(
        bedState.bed,
        booking,
      )

      if (characteristicSummary.isNotEmpty()) {
        bookingDescription.append("<br/>")
        bookingDescription.append(characteristicSummary.joinToString("<br/>"))
      }

      bookingDescription.toString()
    } else {
      ""
    }
  }

  private fun bedDescription(bed: Bed): String {
    val result = StringBuilder()
    result.append(bed.label.bold())
    if (bed.room.characteristics.isNotEmpty()) {
      result.append("<br/>")
      result.append(
        bed.room.characteristics
          .toList()
          .sortedBy { it.label }
          .map { it.label }
          .toHtmlString(),
      )
    }
    return result.toString()
  }

  private fun unplannedBookingDescription(booking: SpaceBooking): String {
    val result = StringBuilder()
    result.append(booking.label.bold())
    if (booking.requiredRoomCharacteristics.isNotEmpty()) {
      result.append("<br/>")
      result.append(booking.requiredRoomCharacteristics.map { it.label }.toHtmlString())
    }
    return result.toString()
  }

  private fun String.bold() = "**$this**"
  private fun List<String>.toHtmlString() = this.joinToString("<br/>")
}

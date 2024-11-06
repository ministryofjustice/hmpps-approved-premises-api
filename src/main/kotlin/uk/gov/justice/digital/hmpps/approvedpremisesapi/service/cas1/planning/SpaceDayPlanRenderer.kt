package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

object SpaceDayPlanRenderer {

  @SuppressWarnings("MagicNumber")
  fun render(
    beds: Set<Bed>,
    result: DayPlannerResult,
  ): String {
    val output = StringBuilder()

    output.appendLine("Planned: ${result.planned.size}")

    if (result.plan.isNotEmpty()) {
      output.appendLine("")

      val body = beds.sortedBy { it.label }.map { bed ->
        val booking = result.plan.firstOrNull { it.bed == bed }?.booking
        val bedCharacteristics = characteristicsMatchingSummary(bed, booking).joinToString(",")

        listOf(
          bed.label,
          booking?.label ?: "",
          bedCharacteristics,
        )
      }

      output.append(
        MarkdownTableRenderer.render(
          headers = listOf("Bed", "Booking", "Characteristics"),
          body = body,
          colWidths = listOf(15, 15, 30),
        ),
      )

      output.appendLine("")
    }

    output.appendLine()
    output.appendLine("Unplanned: ${result.unplanned.size}")
    if (result.unplanned.isNotEmpty()) {
      output.appendLine("")
      output.append(
        MarkdownTableRenderer.render(
          headers = listOf("Booking", "Characteristics"),
          body = result.unplanned.sortedBy { it.label }.map { unplanned ->
            listOf(
              unplanned.label,
              unplanned.requiredRoomCharacteristics.joinToString(",") { it.label },
            )
          },
          colWidths = listOf(15, 30),
        ),
      )
    }

    return output.toString().trimIndent()
  }

  fun characteristicsMatchingSummary(
    bed: Bed,
    booking: SpaceBooking?,
  ): List<String> {
    val allCharacteristics =
      (bed.room.characteristics + (booking?.requiredRoomCharacteristics ?: emptyList()))
        .toList()
        .sortedBy { it.label }

    return allCharacteristics.map { characteristic ->
      val description = StringBuilder()
      description.append(characteristic.label)
      description.append("(")
      if (bed.room.characteristics.contains(characteristic)) {
        description.append("r")
      }
      if (booking != null && booking.requiredRoomCharacteristics.contains(characteristic)) {
        description.append("b")
      }
      description.append(")")

      description.toString()
    }
  }
}

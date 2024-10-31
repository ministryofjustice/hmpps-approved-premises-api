package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

object SpaceDayPlanRenderer {

  @SuppressWarnings("MagicNumber")
  fun render(
    beds: Set<Bed>,
    result: DayPlannerResult,
  ): String {
    val output = StringBuilder()

    output.appendLine("Planned: ${result.planned.size}")

    if (result.planned.isNotEmpty()) {
      output.appendLine("")

      val body = beds.sortedBy { it.label }.map { bed ->
        val booking = result.planned.firstOrNull { it.bed == bed }?.booking
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

    output.appendLine("Unplanned: ${result.unplanned.size}")
    if (result.unplanned.isNotEmpty()) {
      output.appendLine("")
      output.append(
        MarkdownTableRenderer.render(
          headers = listOf("Booking", "Characteristics"),
          body = result.unplanned.sortedBy { it.booking.label }.map { unplanned ->
            listOf(
              unplanned.booking.label,
              unplanned.booking.requiredCharacteristics.joinToString(",") { it.label },
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
      (bed.room.characteristics + (booking?.requiredCharacteristics ?: emptyList()))
        .toList()
        .sortedBy { it.label }

    return allCharacteristics.map { characteristic ->
      val description = StringBuilder()
      description.append(characteristic.label)
      description.append("(")
      if (bed.room.characteristics.contains(characteristic)) {
        description.append("r")
      }
      if (booking != null && booking.requiredCharacteristics.contains(characteristic)) {
        description.append("b")
      }
      description.append(")")

      description.toString()
    }
  }
}

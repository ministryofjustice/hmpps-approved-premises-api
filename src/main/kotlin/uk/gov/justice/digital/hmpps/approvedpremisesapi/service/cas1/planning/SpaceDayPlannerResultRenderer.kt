package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

object SpaceDayPlannerResultRenderer {

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

        val bedCharacteristics = bed.room.characteristics.joinToString(",") { bedCharacteristic ->
          val matched = booking?.let {
            if (booking.requiredCharacteristics.contains(bedCharacteristic)) { "(+)" } else { "(-)" }
          }

          bedCharacteristic.name + (matched ?: "")
        }

        listOf(
          bed.label,
          booking?.label ?: "",
          bedCharacteristics,
        )
      }

      output.append(
        MarkdownTableRenderer.render(
          headers = listOf("Bed", "Booking", "Bed Characteristics"),
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
              unplanned.booking.requiredCharacteristics.joinToString(",") { it.name },
            )
          },
          colWidths = listOf(15, 30),
        ),
      )
    }

    return output.toString().trimIndent()
  }
}

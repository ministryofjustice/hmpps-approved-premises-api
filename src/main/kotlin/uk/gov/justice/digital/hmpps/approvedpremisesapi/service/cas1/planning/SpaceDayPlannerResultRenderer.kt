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
      output.appendLine("| Bed             | Booking         | Bed Characteristics            |")
      output.appendLine("| --------------- | --------------- | ------------------------------ |")
      beds.sortedBy { it.label }.forEach { bed ->

        output.append("| ${bed.label.padEnd(15)} ")

        val booking = result.planned.firstOrNull { it.bed == bed }?.booking
        output.append("| ${(booking?.label ?: "").padEnd(15)} ")

        val bedCharacteristics = bed.room.characteristics.joinToString(",") { bedCharacteristic ->
          val matched = booking?.let {
            if (booking.requiredCharacteristics.contains(bedCharacteristic)) {
              "(+)"
            } else {
              "(-)"
            }
          }

          bedCharacteristic.name + (matched ?: "")
        }
        output.appendLine("| ${bedCharacteristics.padEnd(30)} |")
      }

      output.appendLine("")
    }

    output.appendLine("Unplanned: ${result.unplanned.size}")
    if (result.unplanned.isNotEmpty()) {
      output.appendLine("")
      output.appendLine("| Booking         | Characteristics                |")
      output.appendLine("| --------------- | ------------------------------ |")
      result.unplanned.sortedBy { it.booking.label }.forEach { unplanned ->
        val booking = unplanned.booking
        output.append("| ${booking.label.padEnd(15)} |")
        output.appendLine(" ${booking.requiredCharacteristics.joinToString(",") { it.name }.padEnd(30)} |")
      }
    }
    return output.toString().trimIndent()
  }
}

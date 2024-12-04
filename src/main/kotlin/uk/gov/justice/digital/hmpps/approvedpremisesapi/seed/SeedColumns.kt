package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class SeedColumns(
  val columns: Map<String, String>,
) {
  fun stringOrNull(label: String?) = columns[label]?.trim()?.ifBlank { null }

  fun uuidOrNull(label: String) = columns[label]?.let { UUID.fromString(it.trim()) }

  fun dateTimeFromList(label: String?, formatter: DateTimeFormatter): LocalDateTime? {
    val dateTimeList = stringOrNull(label)
    if (dateTimeList.isNullOrBlank()) {
      return null
    }

    val lastDateTime = dateTimeList.split(",").last()
    return LocalDateTime.parse(lastDateTime, formatter)
  }

  fun dateFromUtcDateTime(label: String?): LocalDate? {
    val dateTime = stringOrNull(label)
    if (dateTime.isNullOrBlank()) {
      return null
    }

    return LocalDate.parse(dateTime.substring(startIndex = 0, endIndex = 10))
  }
}

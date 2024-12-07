package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class SeedColumns(
  val columns: Map<String, String>,
) {
  fun stringOrNull(label: String) = columns[label]?.trim()?.ifBlank { null }

  fun uuidOrNull(label: String) = columns[label]?.let { UUID.fromString(it.trim()) }

  fun lastDateTimeFromList(label: String, formatter: DateTimeFormatter): LocalDateTime? {
    val rawValue = stringOrNull(label) ?: return null

    val lastDateTime = rawValue.split(",").last()
    return LocalDateTime.parse(lastDateTime, formatter)
  }

  fun dateFromUtcDateTime(label: String): LocalDate? {
    val rawValue = stringOrNull(label) ?: return null

    return LocalDate.parse(rawValue.substring(startIndex = 0, endIndex = 10))
  }

  fun stringsFromList(label: String): List<String> {
    val rawValue = stringOrNull(label) ?: return emptyList()

    return rawValue.split(",")
  }

  fun yesNoBoolean(label: String): Boolean? {
    val rawValue = stringOrNull(label)?.uppercase() ?: return null

    return when (rawValue) {
      "YES" -> {
        true
      }
      "NO" -> {
        false
      }
      else -> {
        error("'$rawValue' is not a recognised boolean for '$label' (use yes | no)")
      }
    }
  }
}

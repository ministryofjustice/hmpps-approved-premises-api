package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.json.JSONObject
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2ApplicationDataFormattedDate
import java.time.OffsetDateTime

fun String?.trimToNull(): String? {
  if (this.isNullOrBlank()) return null

  return this.trim()
}

fun String.canonicalise(): String = this.trim()
  .lowercase()
  .replace("&", "and")
  .replace("-", " ")
  .replace(Regex("[^a-z\\s]"), "")
  .replace(Regex("\\s+"), " ")

@SuppressWarnings("MagicNumber")
fun insertHdcDates(data: String): String {
  val obj = JSONObject(data)
  val conditionalReleaseDate = OffsetDateTime.now().plusDays(22)
  val hdcDate = conditionalReleaseDate.minusMonths(2)
  val dates = mapOf(
    "hdcEligibilityDate" to hdcDate.toCas2ApplicationDataFormattedDate(),
    "hdcEligibilityDate-year" to hdcDate.year.toString(),
    "hdcEligibilityDate-month" to hdcDate.month.value.toString(),
    "hdcEligibilityDate-day" to hdcDate.dayOfMonth.toString(),
    "conditionalReleaseDate" to conditionalReleaseDate.toCas2ApplicationDataFormattedDate(),
    "conditionalReleaseDate-year" to conditionalReleaseDate.year.toString(),
    "conditionalReleaseDate-month" to conditionalReleaseDate.month.value.toString(),
    "conditionalReleaseDate-day" to conditionalReleaseDate.dayOfMonth.toString(),
  )
  obj.put("hdc-licence-dates", mapOf("hdc-licence-dates" to dates))
  return obj.toString()
}

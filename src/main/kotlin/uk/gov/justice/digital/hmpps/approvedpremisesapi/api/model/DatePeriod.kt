package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param startDate
 * @param endDate
 */
data class DatePeriod(

  @get:JsonProperty("startDate", required = true) val startDate: java.time.LocalDate,

  @get:JsonProperty("endDate", required = true) val endDate: java.time.LocalDate,
)

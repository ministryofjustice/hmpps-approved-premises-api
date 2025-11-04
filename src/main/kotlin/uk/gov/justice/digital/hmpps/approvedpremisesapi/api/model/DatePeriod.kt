package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param startDate
 * @param endDate
 */
data class DatePeriod(

  val startDate: java.time.LocalDate,

  val endDate: java.time.LocalDate,
)

package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

/**
 *
 * @param startDate
 * @param endDate
 */
data class DatePeriod(

  val startDate: java.time.LocalDate,

  val endDate: java.time.LocalDate,
)

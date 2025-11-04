package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param expectedArrival
 * @param duration
 */
data class PlacementDates(

  val expectedArrival: java.time.LocalDate,

  val duration: kotlin.Int,
)

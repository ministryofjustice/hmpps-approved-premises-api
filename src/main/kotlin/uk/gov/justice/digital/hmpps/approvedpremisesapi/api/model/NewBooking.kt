package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param crn
 * @param arrivalDate
 * @param departureDate
 * @param serviceName
 * @param bedId
 * @param enableTurnarounds
 * @param assessmentId
 * @param eventNumber
 */
data class NewBooking(

  @Schema(example = "A123456", required = true, description = "")
  val crn: kotlin.String,

  @Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
  val arrivalDate: java.time.LocalDate,

  @Schema(example = "Fri Sep 30 01:00:00 BST 2022", required = true, description = "")
  val departureDate: java.time.LocalDate,

  val serviceName: ServiceName,

  val bedId: java.util.UUID? = null,

  val enableTurnarounds: kotlin.Boolean? = null,

  val assessmentId: java.util.UUID? = null,

  val eventNumber: kotlin.String? = null,
)

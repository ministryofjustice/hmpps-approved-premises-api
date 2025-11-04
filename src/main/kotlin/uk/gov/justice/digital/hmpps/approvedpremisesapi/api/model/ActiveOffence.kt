package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param deliusEventNumber
 * @param offenceDescription
 * @param offenceId
 * @param convictionId
 * @param offenceDate
 */
data class ActiveOffence(

  @Schema(example = "7", required = true, description = "")
  val deliusEventNumber: kotlin.String,

  val offenceDescription: kotlin.String,

  @Schema(example = "M1502750438", required = true, description = "")
  val offenceId: kotlin.String,

  @Schema(example = "1502724704", required = true, description = "")
  val convictionId: kotlin.Long,

  val offenceDate: java.time.LocalDate? = null,
)

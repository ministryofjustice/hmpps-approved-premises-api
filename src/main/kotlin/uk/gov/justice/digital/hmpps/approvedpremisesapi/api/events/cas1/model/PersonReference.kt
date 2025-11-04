package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param crn
 * @param noms
 */
data class PersonReference(

  @Schema(example = "C123456", required = true, description = "")
  val crn: kotlin.String,

  @Schema(example = "A1234ZX", required = true, description = "")
  val noms: kotlin.String,
)

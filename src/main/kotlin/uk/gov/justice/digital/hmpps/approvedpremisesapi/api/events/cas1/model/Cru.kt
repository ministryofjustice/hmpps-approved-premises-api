package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param name
 */
data class Cru(

  @Schema(example = "NPS North East", required = true, description = "")
  val name: kotlin.String,
)

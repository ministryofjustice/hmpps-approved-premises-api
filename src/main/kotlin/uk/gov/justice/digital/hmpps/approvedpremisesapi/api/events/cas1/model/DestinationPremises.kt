package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param id
 * @param name
 * @param apCode
 * @param legacyApCode
 * @param probationArea
 */
data class DestinationPremises(

  val id: java.util.UUID,

  @Schema(example = "New Place", required = true, description = "")
  val name: kotlin.String,

  @Schema(example = "NENEW1", required = true, description = "")
  val apCode: kotlin.String,

  @Schema(example = "Q061", required = true, description = "")
  val legacyApCode: kotlin.String,

  val probationArea: ProbationArea,
)

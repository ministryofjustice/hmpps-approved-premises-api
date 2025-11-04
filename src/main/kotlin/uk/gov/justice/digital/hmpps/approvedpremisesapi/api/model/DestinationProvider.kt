package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class DestinationProvider(

  val id: java.util.UUID,

  @Schema(example = "Ext - North East Region", required = true, description = "")
  val name: kotlin.String,

  val isActive: kotlin.Boolean,
)

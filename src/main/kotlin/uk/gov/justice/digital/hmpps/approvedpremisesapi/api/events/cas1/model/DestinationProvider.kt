package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import io.swagger.v3.oas.annotations.media.Schema

data class DestinationProvider(

  @Schema(example = "Ext - North East Region", required = true, description = "")
  val description: kotlin.String,

  @Schema(example = "f0703382-3e8f-49ff-82bc-b970c9fe1b35", required = true, description = "")
  val id: java.util.UUID,
)

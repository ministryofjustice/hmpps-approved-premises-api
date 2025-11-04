package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class Cas2v2StatusUpdate(

  val id: UUID,

  @Schema(example = "moreInfoRequested", required = true, description = "")
  val name: String,

  @Schema(example = "More information requested", required = true, description = "")
  val label: String,

  @Schema(example = "More information about the application has been requested from the HMPPS user.", required = true, description = "")
  val description: String,

  val updatedBy: Cas2v2User? = null,

  val updatedAt: Instant? = null,

  val statusUpdateDetails: List<Cas2v2StatusUpdateDetail>? = null,
)

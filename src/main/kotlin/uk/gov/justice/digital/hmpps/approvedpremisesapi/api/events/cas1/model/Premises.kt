package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Premises(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @field:Schema(example = "Hope House", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @field:Schema(example = "NEHOPE1", required = true, description = "")
  @get:JsonProperty("apCode", required = true) val apCode: kotlin.String,

  @field:Schema(example = "Q057", required = true, description = "The 'Q code' used in Delius to identify an Approved Premises")
  @get:JsonProperty("legacyApCode", required = true) val legacyApCode: kotlin.String,

  @get:JsonProperty("localAuthorityAreaName", required = true) val localAuthorityAreaName: kotlin.String,
)

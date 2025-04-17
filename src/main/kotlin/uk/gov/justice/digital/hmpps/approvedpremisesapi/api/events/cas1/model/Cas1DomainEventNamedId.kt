package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class Cas1DomainEventNamedId (
  @Schema(required = true)
  @get:JsonProperty("id", required = true) val id: UUID,

  @Schema(example = "North East", required = true)
  @get:JsonProperty("name", required = true) val name: String,
)
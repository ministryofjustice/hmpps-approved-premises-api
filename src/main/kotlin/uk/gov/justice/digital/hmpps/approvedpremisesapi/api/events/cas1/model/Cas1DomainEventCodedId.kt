package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class Cas1DomainEventCodedId(
  @Schema(required = true)
  val id: UUID,

  @Schema(required = true)
  val code: String,
)

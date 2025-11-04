package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class Adjudication(

  val id: kotlin.Long,

  val reportedAt: java.time.Instant,

  val establishment: kotlin.String,

  @Schema(example = "Wounding or inflicting grievous bodily harm (inflicting bodily injury with or without weapon) (S20) - 00801", required = true, description = "")
  val offenceDescription: kotlin.String,

  val hearingHeld: kotlin.Boolean,

  val finding: kotlin.String? = null,
)

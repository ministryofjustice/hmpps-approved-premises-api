package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class Cas1UpdatedClarificationNote(

  @get:JsonProperty("response", required = true) val response: kotlin.String,

  @field:Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
  @get:JsonProperty("responseReceivedOn", required = true) val responseReceivedOn: java.time.LocalDate,
)

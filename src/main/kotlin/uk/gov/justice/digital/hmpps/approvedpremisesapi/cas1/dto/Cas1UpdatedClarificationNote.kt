package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class Cas1UpdatedClarificationNote(

  @get:JsonProperty("response", required = true) val response: String,

  @Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
  @get:JsonProperty("responseReceivedOn", required = true) val responseReceivedOn: LocalDate,
)

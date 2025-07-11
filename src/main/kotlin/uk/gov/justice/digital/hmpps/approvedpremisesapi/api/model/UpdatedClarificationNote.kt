package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param response
 * @param responseReceivedOn
 */
data class UpdatedClarificationNote(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("response", required = true) val response: kotlin.String,

  @Schema(example = "Thu Jul 28 01:00:00 BST 2022", required = true, description = "")
  @get:JsonProperty("responseReceivedOn", required = true) val responseReceivedOn: java.time.LocalDate,
)

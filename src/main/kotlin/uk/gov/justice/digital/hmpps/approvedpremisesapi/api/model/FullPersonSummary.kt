package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param name
 */
data class FullPersonSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("name", required = true) val name: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("crn", required = true) override val crn: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("personType", required = true) override val personType: PersonSummaryDiscriminator,
) : PersonSummary {
}

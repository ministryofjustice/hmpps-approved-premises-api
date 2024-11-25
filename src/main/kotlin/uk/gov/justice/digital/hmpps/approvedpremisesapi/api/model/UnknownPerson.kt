package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 */
class UnknownPerson(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("crn", required = true) override val crn: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: PersonType,
) : Person {
}

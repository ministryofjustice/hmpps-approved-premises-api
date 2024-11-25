package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param crn
 * @param noms
 */
data class PersonReference(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("crn", required = true) val crn: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("noms") val noms: kotlin.String? = null,
)

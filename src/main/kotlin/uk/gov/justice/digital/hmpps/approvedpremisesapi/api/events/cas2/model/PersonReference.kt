package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param noms
 * @param crn
 */
data class PersonReference(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("noms", required = true) val noms: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("crn") val crn: kotlin.String? = null,
)

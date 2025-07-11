package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param type
 * @param url
 */
data class Cas1TimelineEventAssociatedUrl(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) val type: Cas1TimelineEventUrlType,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("url", required = true) val url: kotlin.String,
)

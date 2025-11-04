package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1TimelineEventAssociatedUrl(

  @get:JsonProperty("type", required = true) val type: Cas1TimelineEventUrlType,

  @get:JsonProperty("url", required = true) val url: kotlin.String,
)

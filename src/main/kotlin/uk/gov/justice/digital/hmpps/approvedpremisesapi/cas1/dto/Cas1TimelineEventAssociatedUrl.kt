package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1TimelineEventAssociatedUrl(

  @get:JsonProperty("type", required = true) val type: Cas1TimelineEventUrlType,

  @get:JsonProperty("url", required = true) val url: String,
)

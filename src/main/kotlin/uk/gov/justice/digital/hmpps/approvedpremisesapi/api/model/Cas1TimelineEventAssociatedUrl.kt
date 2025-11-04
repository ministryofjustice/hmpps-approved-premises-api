package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param type
 * @param url
 */
data class Cas1TimelineEventAssociatedUrl(

  val type: Cas1TimelineEventUrlType,

  val url: kotlin.String,
)

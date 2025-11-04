package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

/**
 *
 * @param type
 * @param url
 */
data class Cas1TimelineEventAssociatedUrl(

  val type: Cas1TimelineEventUrlType,

  val url: kotlin.String,
)

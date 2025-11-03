package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param notes
 */
data class Cas1NewOutOfServiceBedCancellation(

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)

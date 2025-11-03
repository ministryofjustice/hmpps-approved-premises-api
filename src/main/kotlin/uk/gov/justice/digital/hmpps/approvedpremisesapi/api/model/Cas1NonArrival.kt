package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param reason
 * @param notes
 */
data class Cas1NonArrival(

  @get:JsonProperty("reason", required = true) val reason: java.util.UUID,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)

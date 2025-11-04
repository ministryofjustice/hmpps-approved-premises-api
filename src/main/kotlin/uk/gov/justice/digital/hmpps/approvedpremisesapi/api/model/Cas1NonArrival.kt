package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param reason
 * @param notes
 */
data class Cas1NonArrival(

  val reason: java.util.UUID,

  val notes: kotlin.String? = null,
)

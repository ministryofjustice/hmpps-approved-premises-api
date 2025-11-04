package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param createdAt
 * @param notes
 */
data class LostBedCancellation(

  val id: java.util.UUID,

  val createdAt: java.time.Instant,

  val notes: kotlin.String? = null,
)

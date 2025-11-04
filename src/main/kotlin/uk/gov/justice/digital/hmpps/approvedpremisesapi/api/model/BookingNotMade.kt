package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param placementRequestId
 * @param createdAt
 * @param notes
 */
data class BookingNotMade(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("placementRequestId", required = true) val placementRequestId: java.util.UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)

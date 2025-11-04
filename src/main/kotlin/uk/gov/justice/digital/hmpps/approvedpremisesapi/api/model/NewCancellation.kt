package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param date
 * @param reason
 * @param notes
 * @param otherReason
 */
data class NewCancellation(

  @get:JsonProperty("date", required = true) val date: java.time.LocalDate,

  @get:JsonProperty("reason", required = true) val reason: java.util.UUID,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,

  @get:JsonProperty("otherReason") val otherReason: kotlin.String? = null,
)

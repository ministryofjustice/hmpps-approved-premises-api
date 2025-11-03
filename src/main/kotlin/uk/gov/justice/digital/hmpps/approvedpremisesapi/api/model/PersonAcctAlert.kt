package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param alertId
 * @param dateCreated
 * @param comment
 * @param description
 * @param dateExpires
 * @param alertTypeDescription
 */
data class PersonAcctAlert(

  @get:JsonProperty("alertId", required = true) val alertId: kotlin.Long,

  @get:JsonProperty("dateCreated", required = true) val dateCreated: java.time.LocalDate,

  @get:JsonProperty("comment") val comment: kotlin.String? = null,

  @get:JsonProperty("description") val description: kotlin.String? = null,

  @get:JsonProperty("dateExpires") val dateExpires: java.time.LocalDate? = null,

  @get:JsonProperty("alertTypeDescription") val alertTypeDescription: kotlin.String? = null,
)

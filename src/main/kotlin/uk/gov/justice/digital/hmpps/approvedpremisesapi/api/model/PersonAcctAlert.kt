package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PersonAcctAlert(

  @get:JsonProperty("alertId", required = true) val alertId: Long,

  @get:JsonProperty("dateCreated", required = true) val dateCreated: java.time.LocalDate,

  @get:JsonProperty("comment") val comment: String? = null,

  @get:JsonProperty("description") val description: String? = null,

  @get:JsonProperty("dateExpires") val dateExpires: java.time.LocalDate? = null,

  @get:JsonProperty("alertTypeDescription") val alertTypeDescription: String? = null,
)

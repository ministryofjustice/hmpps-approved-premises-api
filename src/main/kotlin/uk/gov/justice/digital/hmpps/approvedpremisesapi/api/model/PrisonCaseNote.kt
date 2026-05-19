package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PrisonCaseNote(

  @get:JsonProperty("id", required = true) val id: String,

  @get:JsonProperty("sensitive", required = true) val sensitive: Boolean,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.Instant,

  @get:JsonProperty("authorName", required = true) val authorName: String,

  @get:JsonProperty("type", required = true) val type: String,

  @get:JsonProperty("subType", required = true) val subType: String,

  @get:JsonProperty("note", required = true) val note: String,
)

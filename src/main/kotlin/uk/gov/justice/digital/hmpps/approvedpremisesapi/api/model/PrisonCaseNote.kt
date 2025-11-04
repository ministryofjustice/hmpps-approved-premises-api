package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param sensitive
 * @param createdAt
 * @param occurredAt
 * @param authorName
 * @param type
 * @param subType
 * @param note
 */
data class PrisonCaseNote(

  @get:JsonProperty("id", required = true) val id: kotlin.String,

  @get:JsonProperty("sensitive", required = true) val sensitive: kotlin.Boolean,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("occurredAt", required = true) val occurredAt: java.time.Instant,

  @get:JsonProperty("authorName", required = true) val authorName: kotlin.String,

  @get:JsonProperty("type", required = true) val type: kotlin.String,

  @get:JsonProperty("subType", required = true) val subType: kotlin.String,

  @get:JsonProperty("note", required = true) val note: kotlin.String,
)

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

  val id: kotlin.String,

  val sensitive: kotlin.Boolean,

  val createdAt: java.time.Instant,

  val occurredAt: java.time.Instant,

  val authorName: kotlin.String,

  val type: kotlin.String,

  val subType: kotlin.String,

  val note: kotlin.String,
)

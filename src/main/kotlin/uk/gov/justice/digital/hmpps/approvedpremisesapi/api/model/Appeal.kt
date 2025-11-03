package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param appealDate
 * @param appealDetail
 * @param decision
 * @param decisionDetail
 * @param createdAt
 * @param applicationId
 * @param createdByUser
 * @param assessmentId
 */
data class Appeal(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("appealDate", required = true) val appealDate: java.time.LocalDate,

  @get:JsonProperty("appealDetail", required = true) val appealDetail: kotlin.String,

  @get:JsonProperty("decision", required = true) val decision: AppealDecision,

  @get:JsonProperty("decisionDetail", required = true) val decisionDetail: kotlin.String,

  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @get:JsonProperty("createdByUser", required = true) val createdByUser: User,

  @get:JsonProperty("assessmentId") val assessmentId: java.util.UUID? = null,
)

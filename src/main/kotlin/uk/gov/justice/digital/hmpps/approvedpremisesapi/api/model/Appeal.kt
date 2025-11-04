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

  val id: java.util.UUID,

  val appealDate: java.time.LocalDate,

  val appealDetail: kotlin.String,

  val decision: AppealDecision,

  val decisionDetail: kotlin.String,

  val createdAt: java.time.Instant,

  val applicationId: java.util.UUID,

  val createdByUser: User,

  val assessmentId: java.util.UUID? = null,
)

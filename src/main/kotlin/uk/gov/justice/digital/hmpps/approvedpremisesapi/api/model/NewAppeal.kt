package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param appealDate
 * @param appealDetail
 * @param decision
 * @param decisionDetail
 */
data class NewAppeal(

  val appealDate: java.time.LocalDate,

  val appealDetail: kotlin.String,

  val decision: AppealDecision,

  val decisionDetail: kotlin.String,
)

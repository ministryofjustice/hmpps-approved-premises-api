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

  @get:JsonProperty("appealDate", required = true) val appealDate: java.time.LocalDate,

  @get:JsonProperty("appealDetail", required = true) val appealDetail: kotlin.String,

  @get:JsonProperty("decision", required = true) val decision: AppealDecision,

  @get:JsonProperty("decisionDetail", required = true) val decisionDetail: kotlin.String,
)

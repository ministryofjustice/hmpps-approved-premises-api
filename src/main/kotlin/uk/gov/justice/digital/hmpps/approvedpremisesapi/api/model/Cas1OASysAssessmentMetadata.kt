package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param hasApplicableAssessment
 * @param dateStarted
 * @param dateCompleted
 */
data class Cas1OASysAssessmentMetadata(

  val hasApplicableAssessment: kotlin.Boolean,

  val dateStarted: java.time.Instant? = null,

  val dateCompleted: java.time.Instant? = null,
)

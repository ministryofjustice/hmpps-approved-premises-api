package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1OASysAssessmentMetadata(

  @get:JsonProperty("hasApplicableAssessment", required = true) val hasApplicableAssessment: kotlin.Boolean,

  @get:JsonProperty("dateStarted") val dateStarted: java.time.Instant? = null,

  @get:JsonProperty("dateCompleted") val dateCompleted: java.time.Instant? = null,
)

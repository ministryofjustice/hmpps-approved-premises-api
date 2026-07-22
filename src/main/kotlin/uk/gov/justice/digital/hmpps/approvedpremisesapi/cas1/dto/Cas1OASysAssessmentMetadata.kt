package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class Cas1OASysAssessmentMetadata(

  @get:JsonProperty("hasApplicableAssessment", required = true) val hasApplicableAssessment: Boolean,

  @get:JsonProperty("dateStarted") val dateStarted: Instant? = null,

  @get:JsonProperty("dateCompleted") val dateCompleted: Instant? = null,
  @get:JsonProperty("lastUpdatedDate") val lastUpdatedDate: Instant? = null,
)

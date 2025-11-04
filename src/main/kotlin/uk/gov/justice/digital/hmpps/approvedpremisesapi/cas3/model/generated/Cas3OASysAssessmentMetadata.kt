package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 *
 * @param hasApplicableAssessment
 * @param dateStarted
 * @param dateCompleted
 */
data class Cas3OASysAssessmentMetadata(

  @get:JsonProperty("hasApplicableAssessment", required = true) val hasApplicableAssessment: Boolean,

  @get:JsonProperty("dateStarted") val dateStarted: Instant? = null,

  @get:JsonProperty("dateCompleted") val dateCompleted: Instant? = null,
)

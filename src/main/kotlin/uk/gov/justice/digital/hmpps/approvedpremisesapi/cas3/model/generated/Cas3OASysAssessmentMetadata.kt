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

  val hasApplicableAssessment: Boolean,

  val dateStarted: Instant? = null,

  val dateCompleted: Instant? = null,
)

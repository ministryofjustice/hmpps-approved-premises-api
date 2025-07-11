package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 *
 * @param hasApplicableAssessment
 * @param dateStarted
 * @param dateCompleted
 */
data class Cas3OASysAssessmentMetadata(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("hasApplicableAssessment", required = true) val hasApplicableAssessment: Boolean,

  @Schema(example = "null", description = "")
  @get:JsonProperty("dateStarted") val dateStarted: Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("dateCompleted") val dateCompleted: Instant? = null,
)

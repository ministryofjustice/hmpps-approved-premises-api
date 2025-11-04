package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param newStatus The \"name\" of the new status to be applied
 * @param newStatusDetails
 */
data class Cas2AssessmentStatusUpdate(

  @Schema(example = "moreInfoRequired", required = true, description = "The \"name\" of the new status to be applied")
  val newStatus: String,

  val newStatusDetails: List<String>? = null,
)

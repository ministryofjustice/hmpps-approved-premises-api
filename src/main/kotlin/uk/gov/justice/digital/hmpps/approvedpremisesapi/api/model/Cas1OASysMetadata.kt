package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param assessmentMetadata
 * @param supportingInformation
 */
data class Cas1OASysMetadata(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assessmentMetadata", required = true) val assessmentMetadata: Cas1OASysAssessmentMetadata,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("supportingInformation", required = true) val supportingInformation: kotlin.collections.List<Cas1OASysSupportingInformationQuestionMetaData>,
)

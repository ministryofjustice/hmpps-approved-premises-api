package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Groups questions and answers from OAsys
 * @param assessmentMetadata 
 * @param answers 
 */
data class Cas3OASysGroup(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("assessmentMetadata", required = true) val assessmentMetadata: Cas3OASysAssessmentMetadata,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("answers", required = true) val answers: List<OASysQuestion>
    ) {

}


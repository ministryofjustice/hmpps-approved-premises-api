package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysAssessmentMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroupName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Groups questions and answers from OAsys. Groups directly align with OAsys Sections other than 'needs', which collates questions from multiple sections
 * @param group 
 * @param assessmentMetadata 
 * @param answers 
 */
data class Cas1OASysGroup(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("group", required = true) val group: Cas1OASysGroupName,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("assessmentMetadata", required = true) val assessmentMetadata: Cas1OASysAssessmentMetadata,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("answers", required = true) val answers: kotlin.collections.List<OASysQuestion>
    ) {

}


package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param translatedDocument Any object that conforms to the current JSON schema for an application
 * @param applicationId Id of the application being submitted
 * @param telephoneNumber 
 * @param preferredAreas First and second preferences for where the accommodation should be located, pipe-separated
 * @param hdcEligibilityDate 
 * @param conditionalReleaseDate 
 */
data class SubmitCas2Application(

    @Schema(example = "null", required = true, description = "Any object that conforms to the current JSON schema for an application")
    @get:JsonProperty("translatedDocument", required = true) val translatedDocument: kotlin.Any,

    @Schema(example = "null", required = true, description = "Id of the application being submitted")
    @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("telephoneNumber", required = true) val telephoneNumber: kotlin.String,

    @Schema(example = "Leeds | Bradford", description = "First and second preferences for where the accommodation should be located, pipe-separated")
    @get:JsonProperty("preferredAreas") val preferredAreas: kotlin.String? = null,

    @Schema(example = "Thu Mar 30 01:00:00 BST 2023", description = "")
    @get:JsonProperty("hdcEligibilityDate") val hdcEligibilityDate: java.time.LocalDate? = null,

    @Schema(example = "Sun Apr 30 01:00:00 BST 2023", description = "")
    @get:JsonProperty("conditionalReleaseDate") val conditionalReleaseDate: java.time.LocalDate? = null
) {

}


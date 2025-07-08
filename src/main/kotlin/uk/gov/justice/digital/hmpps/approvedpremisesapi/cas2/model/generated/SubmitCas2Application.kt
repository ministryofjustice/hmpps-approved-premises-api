package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param translatedDocument Any object
 * @param applicationId Id of the application being submitted
 * @param telephoneNumber
 * @param preferredAreas First and second preferences for where the accommodation should be located, pipe-separated
 * @param hdcEligibilityDate
 * @param conditionalReleaseDate
 */
data class SubmitCas2Application(

  @Schema(example = "null", required = true, description = "Any object")
  @get:JsonProperty("translatedDocument", required = true) val translatedDocument: Any,

  @Schema(example = "null", required = true, description = "Id of the application being submitted")
  @get:JsonProperty("applicationId", required = true) val applicationId: UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("telephoneNumber", required = true) val telephoneNumber: String,

  @Schema(example = "Leeds | Bradford", description = "First and second preferences for where the accommodation should be located, pipe-separated")
  @get:JsonProperty("preferredAreas") val preferredAreas: String? = null,

  @Schema(example = "Thu Mar 30 01:00:00 BST 2023", description = "")
  @get:JsonProperty("hdcEligibilityDate") val hdcEligibilityDate: LocalDate? = null,

  @Schema(example = "Sun Apr 30 01:00:00 BST 2023", description = "")
  @get:JsonProperty("conditionalReleaseDate") val conditionalReleaseDate: LocalDate? = null,
)

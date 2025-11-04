package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param translatedDocument Any object
 * @param applicationId Id of the application being submitted
 * @param telephoneNumber
 * @param applicationOrigin
 * @param preferredAreas First and second preferences for where the accommodation should be located, pipe-separated
 * @param hdcEligibilityDate
 * @param conditionalReleaseDate
 * @param bailHearingDate
 */
data class SubmitCas2v2Application(

  val translatedDocument: Any,

  @Schema(example = "null", required = true, description = "Id of the application being submitted")
  val applicationId: UUID,

  val telephoneNumber: String,

  val applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,

  @Schema(example = "Leeds | Bradford", description = "First and second preferences for where the accommodation should be located, pipe-separated")
  val preferredAreas: String? = null,

  @Schema(example = "Thu Mar 30 01:00:00 BST 2023", description = "")
  val hdcEligibilityDate: LocalDate? = null,

  @Schema(example = "Sun Apr 30 01:00:00 BST 2023", description = "")
  val conditionalReleaseDate: LocalDate? = null,

  val bailHearingDate: LocalDate? = null,
)

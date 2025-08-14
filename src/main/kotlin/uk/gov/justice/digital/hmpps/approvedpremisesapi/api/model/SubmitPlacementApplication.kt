package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod

/**
 *
 * @param translatedDocument Any object
 * @param placementType
 * @param placementDates
 */
data class SubmitPlacementApplication(

  @Schema(required = true)
  val translatedDocument: Any,

  @Schema(deprecated = true, description = "Please use release type instead")
  val placementType: PlacementType?,

  @Schema(deprecated = true, description = "Please use requestedPlacementPeriods instead")
  val placementDates: List<PlacementDates>?,

  val requestedPlacementPeriods: List<Cas1RequestedPlacementPeriod>?,

  val releaseType: ReleaseTypeOption?,
  val sentenceType: SentenceTypeOption?,
  val situationType: SituationOption?,
)

package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod

data class SubmitPlacementApplication(

  @Schema(required = true)
  val translatedDocument: Any,
  val requestedPlacementPeriods: List<Cas1RequestedPlacementPeriod>,

  val releaseType: ReleaseTypeOption,
  val sentenceType: SentenceTypeOption?,
  val situationType: SituationOption?,
)

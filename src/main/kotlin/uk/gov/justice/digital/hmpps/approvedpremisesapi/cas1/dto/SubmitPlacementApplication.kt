package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption

data class SubmitPlacementApplication(

  @Schema(required = true)
  val translatedDocument: Any,
  val requestedPlacementPeriods: List<Cas1RequestedPlacementPeriod>,

  val releaseType: ReleaseTypeOption,
  val sentenceType: SentenceTypeOption?,
  val situationType: SituationOption?,
)

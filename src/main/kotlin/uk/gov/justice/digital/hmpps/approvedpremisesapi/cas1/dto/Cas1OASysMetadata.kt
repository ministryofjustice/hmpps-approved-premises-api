package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema

data class Cas1OASysMetadata(
  val assessmentMetadata: Cas1OASysAssessmentMetadata,
  @Schema(description = "Supporting information specifies which optional questions/answers are available for inclusion in an application")
  val supportingInformation: List<Cas1OASysSupportingInformationQuestionMetaData>,
)

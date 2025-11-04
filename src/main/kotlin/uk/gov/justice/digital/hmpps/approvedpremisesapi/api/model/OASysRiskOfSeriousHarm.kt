package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class OASysRiskOfSeriousHarm(

  @Schema(example = "138985987", required = true, description = "The ID of assessment being used. This should always be the latest Layer 3 assessment, regardless of state.")
  val assessmentId: kotlin.Long,

  val assessmentState: OASysAssessmentState,

  val dateStarted: java.time.Instant,

  val rosh: kotlin.collections.List<OASysQuestion>,

  val dateCompleted: java.time.Instant? = null,
)

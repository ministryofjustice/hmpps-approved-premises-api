package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.time.Instant

data class Cas2OASysAssessmentMetadataDto(
  val hasApplicableAssessment: Boolean,
  val dateStarted: Instant? = null,
  val dateCompleted: Instant? = null,
)

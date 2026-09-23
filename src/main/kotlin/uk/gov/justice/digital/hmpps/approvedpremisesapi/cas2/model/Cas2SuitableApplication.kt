package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.time.OffsetDateTime
import java.util.UUID

data class Cas2SuitableApplication(
  val uiUrl: String,
  val id: UUID,
  val submittedApplication: Cas2ExternalSubmittedApplicationDto?,
)

data class Cas2ExternalSubmittedApplicationDto(
  val latestAssessmentStatus: Cas2AssessmentStatus?,
  val submittedAt: OffsetDateTime,
)

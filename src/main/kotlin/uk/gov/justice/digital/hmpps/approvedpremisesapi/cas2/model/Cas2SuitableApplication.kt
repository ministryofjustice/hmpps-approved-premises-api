package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.time.OffsetDateTime
import java.util.UUID

data class Cas2SuitableApplication(
  val uiUrl: String,
  val id: UUID,
  val submittedApplication: Cas2ExternalSubmittedApplicationDto?,

  @Deprecated("Use submittedApplication instead")
  val application: Cas2ExternalApplicationDto?,
)

data class Cas2ExternalApplicationDto(
  val id: UUID,
  val status: String?,
)

data class Cas2ExternalSubmittedApplicationDto(
  val latestAssessmentStatus: String?,
  val submittedAt: OffsetDateTime,
)

package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import java.time.LocalDate

data class ReferralsDataDto(
  val tier: String?,
  val isEsapApplication: Boolean?,
  val isPipeApplication: Boolean?,
  val decision: String?,
  val applicationSubmittedAt: LocalDate?,
  val assessmentSubmittedAt: LocalDate?,
  val rejectionRationale: String?,
  val releaseType: String?,
  val clarificationNoteCount: Int,
)

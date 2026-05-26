package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys

import java.time.OffsetDateTime

data class OASysAssessmentSummary(
  val assessmentPk: Long,
  val assessmentType: String,
  val initiationDate: OffsetDateTime,
  val status: String,
  val completedDate: OffsetDateTime?,
)

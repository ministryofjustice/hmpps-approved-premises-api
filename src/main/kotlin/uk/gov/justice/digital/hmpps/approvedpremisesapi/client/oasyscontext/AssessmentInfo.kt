package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext

import java.time.OffsetDateTime

abstract class AssessmentInfo(
  val assessmentId: Long,
  val assessmentType: String,
  val dateCompleted: OffsetDateTime?,
  val assessorSignedDate: OffsetDateTime?,
  val initiationDate: OffsetDateTime,
  val assessmentStatus: String,
  val superStatus: String?,
  val limitedAccessOffender: Boolean,
)

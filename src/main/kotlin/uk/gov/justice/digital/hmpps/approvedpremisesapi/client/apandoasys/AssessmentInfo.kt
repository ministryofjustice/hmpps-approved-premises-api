package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
@Suppress("LongParameterList")
abstract class AssessmentInfo(
  val assessmentId: Long,
  val assessmentType: String,
  val dateCompleted: OffsetDateTime?,
  val assessorSignedDate: OffsetDateTime?,
  val initiationDate: OffsetDateTime,
  val assessmentStatus: String,
  val superStatus: String?,
  val limitedAccessOffender: Boolean,
  val laterWIPAssessmentExists: Boolean? = null,
  val lastUpdatedDate: OffsetDateTime? = null,
)

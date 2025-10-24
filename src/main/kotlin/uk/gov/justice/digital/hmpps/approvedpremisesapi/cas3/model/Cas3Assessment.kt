package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationUser
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas3Assessment(
  val id: UUID,
  val application: TemporaryAccommodationApplication,
  val summaryData: Any,
  val createdAt: Instant,
  val clarificationNotes: List<ClarificationNote>,
  val allocatedToStaffMember: TemporaryAccommodationUser? = null,
  val status: TemporaryAccommodationAssessmentStatus? = null,
  val releaseDate: LocalDate? = null,
  val accommodationRequiredFromDate: LocalDate? = null,
  val allocatedAt: Instant? = null,
  val submittedAt: Instant? = null,
  val decision: AssessmentDecision? = null,
  val rejectionRationale: String? = null,
  val `data`: Any? = null,
)

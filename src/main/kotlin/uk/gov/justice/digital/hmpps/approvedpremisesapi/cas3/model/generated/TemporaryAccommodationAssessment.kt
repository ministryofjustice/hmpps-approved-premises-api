package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.TemporaryAccommodationApplication
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class TemporaryAccommodationAssessment(
  val application: TemporaryAccommodationApplication,
  val summaryData: Any,
  override val service: String,
  override val id: UUID,
  override val createdAt: Instant,
  override val clarificationNotes: List<ClarificationNote>,
  val allocatedToStaffMember: TemporaryAccommodationUser? = null,
  val status: TemporaryAccommodationAssessmentStatus? = null,
  val releaseDate: LocalDate? = null,
  val accommodationRequiredFromDate: LocalDate? = null,
  override val allocatedAt: Instant? = null,
  override val submittedAt: Instant? = null,
  override val decision: AssessmentDecision? = null,
  override val rejectionRationale: String? = null,
  override val `data`: Any? = null,
) : Assessment

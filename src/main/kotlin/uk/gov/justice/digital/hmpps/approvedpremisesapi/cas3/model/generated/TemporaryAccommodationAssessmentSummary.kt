package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import java.time.Instant
import java.util.UUID

data class TemporaryAccommodationAssessmentSummary(
  override val type: String,
  val status: TemporaryAccommodationAssessmentStatus,
  override val id: UUID,
  override val applicationId: UUID,
  override val createdAt: Instant,
  override val person: Person,
  val probationDeliveryUnitName: String? = null,
  override val arrivalDate: Instant? = null,
  override val dateOfInfoRequest: Instant? = null,
  override val decision: AssessmentDecision? = null,
  override val risks: PersonRisks? = null,
) : AssessmentSummary

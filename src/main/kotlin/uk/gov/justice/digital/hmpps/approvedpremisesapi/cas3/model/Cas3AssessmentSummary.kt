package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import java.time.Instant
import java.util.UUID

data class Cas3AssessmentSummary(
  val id: UUID,
  val applicationId: UUID,
  val status: TemporaryAccommodationAssessmentStatus,
  val createdAt: Instant,
  val person: Person,
  val probationDeliveryUnitName: String? = null,
  val arrivalDate: Instant? = null,
  val dateOfInfoRequest: Instant? = null,
  val decision: AssessmentDecision? = null,
  val risks: PersonRisks? = null,
)

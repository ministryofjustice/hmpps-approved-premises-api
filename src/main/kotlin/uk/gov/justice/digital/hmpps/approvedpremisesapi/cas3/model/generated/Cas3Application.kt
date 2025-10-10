package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import java.time.Instant
import java.util.UUID

data class Cas3Application(

  val id: UUID,
  val person: Person,
  val createdAt: Instant,
  val createdByUserId: UUID,
  val status: ApplicationStatus,
  val offenceId: String,
  val `data`: Any? = null,
  val document: Any? = null,
  val risks: PersonRisks? = null,
  val submittedAt: Instant? = null,
  val arrivalDate: Instant? = null,
  val assessmentId: UUID? = null,
)

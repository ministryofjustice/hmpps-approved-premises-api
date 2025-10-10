package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import java.time.Instant
import java.util.UUID

data class TemporaryAccommodationApplication(
  val createdByUserId: UUID,
  val status: ApplicationStatus,
  val offenceId: String,
  override val type: String,
  override val id: UUID,
  override val person: Person,
  override val createdAt: Instant,
  val `data`: Any? = null,
  val document: Any? = null,
  val risks: PersonRisks? = null,
  val submittedAt: Instant? = null,
  val arrivalDate: Instant? = null,
  val assessmentId: UUID? = null,
) : Application

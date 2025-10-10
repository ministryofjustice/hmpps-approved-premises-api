package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks

data class TemporaryAccommodationApplicationSummary(
  val createdByUserId: java.util.UUID,
  val status: ApplicationStatus,
  override val type: kotlin.String = "CAS3",
  override val id: java.util.UUID,
  override val person: Person,
  override val createdAt: java.time.Instant,
  val risks: PersonRisks? = null,
  override val submittedAt: java.time.Instant? = null,
) : ApplicationSummary

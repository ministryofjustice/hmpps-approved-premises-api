package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import java.time.Instant
import java.util.UUID

/**
 *
 * @param id
 * @param person
 * @param createdAt
 * @param createdByUserId
 * @param status
 * @param submittedAt
 * @param risks
 */
data class Cas3ApplicationSummary(

  val id: UUID,

  val person: Person,

  val createdAt: Instant,

  val createdByUserId: UUID,

  val status: ApplicationStatus,

  val submittedAt: Instant? = null,

  val risks: PersonRisks? = null,
)

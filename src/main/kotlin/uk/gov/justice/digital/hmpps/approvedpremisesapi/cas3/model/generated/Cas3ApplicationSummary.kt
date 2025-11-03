package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
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

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("person", required = true) val person: Person,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: UUID,

  @get:JsonProperty("status", required = true) val status: ApplicationStatus,

  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @get:JsonProperty("risks") val risks: PersonRisks? = null,
)

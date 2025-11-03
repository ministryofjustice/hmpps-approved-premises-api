package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks

/**
 *
 * @param createdByUserId
 * @param status
 * @param risks
 */
data class TemporaryAccommodationApplicationSummary(

  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: java.util.UUID,

  @get:JsonProperty("status", required = true) val status: ApplicationStatus,

  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("person", required = true) override val person: Person,

  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @get:JsonProperty("risks") val risks: PersonRisks? = null,

  @get:JsonProperty("submittedAt") override val submittedAt: java.time.Instant? = null,
) : ApplicationSummary

package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

/**
 *
 * @param id
 * @param createdByUserId
 * @param crn
 * @param nomsNumber
 * @param personName
 * @param createdAt
 * @param submittedAt
 */
data class Cas2SubmittedApplicationSummary(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: UUID,

  @get:JsonProperty("crn", required = true) val crn: String,

  @get:JsonProperty("nomsNumber", required = true) val nomsNumber: String,

  @get:JsonProperty("personName", required = true) val personName: String,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,
)

package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param id
 * @param createdByUserId
 * @param crn
 * @param personName
 * @param createdAt
 * @param nomsNumber
 * @param submittedAt
 * @param applicationOrigin
 * @param bailHearingDate
 */
data class Cas2v2SubmittedApplicationSummary(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: UUID,

  @get:JsonProperty("crn", required = true) val crn: String,

  @get:JsonProperty("personName", required = true) val personName: String,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("nomsNumber") val nomsNumber: String? = null,

  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @get:JsonProperty("applicationOrigin") val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,

  @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,
)

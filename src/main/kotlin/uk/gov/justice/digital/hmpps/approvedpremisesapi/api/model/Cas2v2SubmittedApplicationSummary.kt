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

  val id: UUID,

  val createdByUserId: UUID,

  val crn: String,

  val personName: String,

  val createdAt: Instant,

  val nomsNumber: String? = null,

  val submittedAt: Instant? = null,

  val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,

  val bailHearingDate: LocalDate? = null,
)

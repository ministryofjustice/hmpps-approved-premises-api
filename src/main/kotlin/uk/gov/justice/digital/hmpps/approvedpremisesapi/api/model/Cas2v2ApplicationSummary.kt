package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param type
 * @param id
 * @param createdAt
 * @param createdByUserId
 * @param status
 * @param personName
 * @param crn
 * @param applicationOrigin
 * @param submittedAt
 * @param createdByUserName
 * @param latestStatusUpdate
 * @param risks
 * @param hdcEligibilityDate
 * @param nomsNumber
 * @param bailHearingDate
 * @param prisonCode
 */
data class Cas2v2ApplicationSummary(

  val type: String,

  val id: UUID,

  val createdAt: Instant,

  val createdByUserId: UUID,

  val status: ApplicationStatus,

  val personName: String,

  val crn: String,

  @get:JsonProperty(
    "applicationOrigin",
    required = true,
  ) val applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,

  val submittedAt: Instant? = null,

  val createdByUserName: String? = null,

  val latestStatusUpdate: LatestCas2v2StatusUpdate? = null,

  val risks: PersonRisks? = null,

  val hdcEligibilityDate: LocalDate? = null,

  val nomsNumber: String? = null,

  val bailHearingDate: LocalDate? = null,

  val prisonCode: String? = null,
)

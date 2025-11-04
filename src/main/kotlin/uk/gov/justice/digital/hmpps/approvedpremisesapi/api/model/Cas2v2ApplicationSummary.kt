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

  @get:JsonProperty("type", required = true) val type: String,

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: UUID,

  @get:JsonProperty("status", required = true) val status: ApplicationStatus,

  @get:JsonProperty("personName", required = true) val personName: String,

  @get:JsonProperty("crn", required = true) val crn: String,

  @get:JsonProperty(
    "applicationOrigin",
    required = true,
  ) val applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,

  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @get:JsonProperty("createdByUserName") val createdByUserName: String? = null,

  @get:JsonProperty("latestStatusUpdate") val latestStatusUpdate: LatestCas2v2StatusUpdate? = null,

  @get:JsonProperty("risks") val risks: PersonRisks? = null,

  @get:JsonProperty("hdcEligibilityDate") val hdcEligibilityDate: LocalDate? = null,

  @get:JsonProperty("nomsNumber") val nomsNumber: String? = null,

  @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,

  @get:JsonProperty("prisonCode") val prisonCode: String? = null,
)

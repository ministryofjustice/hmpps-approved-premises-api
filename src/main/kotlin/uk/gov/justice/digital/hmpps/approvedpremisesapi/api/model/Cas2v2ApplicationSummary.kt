package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
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

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) val type: String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: ApplicationStatus,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("personName", required = true) val personName: String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("crn", required = true) val crn: String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty(
    "applicationOrigin",
    required = true,
  ) val applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("createdByUserName") val createdByUserName: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("latestStatusUpdate") val latestStatusUpdate: LatestCas2v2StatusUpdate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("risks") val risks: PersonRisks? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("hdcEligibilityDate") val hdcEligibilityDate: LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("nomsNumber") val nomsNumber: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("prisonCode") val prisonCode: String? = null,
)

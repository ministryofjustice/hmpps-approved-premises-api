package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
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
 * @param nomsNumber
 * @param allocatedPomUserId
 * @param allocatedPomName
 * @param assignmentDate
 * @param submittedAt
 * @param createdByUserName
 * @param latestStatusUpdate
 * @param risks
 * @param hdcEligibilityDate
 * @param currentPrisonName
 * @param applicationOrigin
 * @param bailHearingDate
 */
data class Cas2ApplicationSummary(

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
  @get:JsonProperty("nomsNumber", required = true) val nomsNumber: String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("allocatedPomUserId", required = true) val allocatedPomUserId: UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("allocatedPomName", required = true) val allocatedPomName: String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("assignmentDate", required = true) val assignmentDate: LocalDate,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("createdByUserName") val createdByUserName: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("latestStatusUpdate") val latestStatusUpdate: LatestCas2StatusUpdate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("risks") val risks: PersonRisks? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("hdcEligibilityDate") val hdcEligibilityDate: LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("currentPrisonName") val currentPrisonName: String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicationOrigin") val applicationOrigin: ApplicationOrigin? = ApplicationOrigin.homeDetentionCurfew,

  @Schema(example = "null", description = "")
  @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,
)

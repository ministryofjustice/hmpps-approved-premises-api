package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

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
 * @param submittedAt
 * @param createdByUserName
 * @param latestStatusUpdate
 * @param risks
 * @param hdcEligibilityDate
 */
data class Cas2ApplicationSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) val type: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) val createdAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: ApplicationStatus,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("personName", required = true) val personName: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("crn", required = true) val crn: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("nomsNumber", required = true) val nomsNumber: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") val submittedAt: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("createdByUserName") val createdByUserName: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("latestStatusUpdate") val latestStatusUpdate: LatestCas2StatusUpdate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("risks") val risks: PersonRisks? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("hdcEligibilityDate") val hdcEligibilityDate: java.time.LocalDate? = null,
)

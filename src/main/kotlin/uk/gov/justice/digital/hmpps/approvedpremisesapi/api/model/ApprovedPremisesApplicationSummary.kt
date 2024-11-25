package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param createdByUserId
 * @param status
 * @param isWithdrawn
 * @param hasRequestsForPlacement
 * @param isWomensApplication
 * @param isPipeApplication
 * @param isEmergencyApplication
 * @param isEsapApplication
 * @param arrivalDate
 * @param risks
 * @param tier
 * @param releaseType
 */
data class ApprovedPremisesApplicationSummary(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: ApprovedPremisesApplicationStatus,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("isWithdrawn", required = true) val isWithdrawn: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("hasRequestsForPlacement", required = true) val hasRequestsForPlacement: kotlin.Boolean,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("person", required = true) override val person: Person,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isWomensApplication") val isWomensApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isPipeApplication") val isPipeApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isEmergencyApplication") val isEmergencyApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isEsapApplication") val isEsapApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.Instant? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("risks") val risks: PersonRisks? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("tier") val tier: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("releaseType") val releaseType: ReleaseTypeOption? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("submittedAt") override val submittedAt: java.time.Instant? = null,
) : ApplicationSummary

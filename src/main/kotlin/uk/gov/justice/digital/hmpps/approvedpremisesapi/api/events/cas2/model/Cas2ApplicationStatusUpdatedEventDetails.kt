package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2User

/**
 *
 * @param applicationId
 * @param applicationUrl
 * @param personReference
 * @param newStatus
 * @param updatedBy
 * @param updatedAt
 * @param applicationOrigin
 */
data class Cas2ApplicationStatusUpdatedEventDetails(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,

  @Schema(example = "https://community-accommodation-tier-2-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "")
  @get:JsonProperty("applicationUrl", required = true) val applicationUrl: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("personReference", required = true) val personReference: PersonReference,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("newStatus", required = true) val newStatus: Cas2Status,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("updatedBy", required = true) val updatedBy: Cas2User,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("updatedAt", required = true) val updatedAt: java.time.Instant,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("applicationOrigin", required = true) val applicationOrigin: kotlin.String = "homeDetentionCurfew",
)

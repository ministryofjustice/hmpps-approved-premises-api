package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param isInapplicable
 * @param isWomensApplication
 * @param isPipeApplication Use apType
 * @param isEmergencyApplication noticeType should be used to indicate if an emergency application
 * @param isEsapApplication Use apType
 * @param apType
 * @param targetLocation
 * @param releaseType
 * @param arrivalDate
 * @param noticeType
 */
data class UpdateApprovedPremisesApplication(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: UpdateApplicationType,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("data", required = true) override val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isInapplicable") val isInapplicable: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isWomensApplication") val isWomensApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Use apType")
  @Deprecated(message = "")
  @get:JsonProperty("isPipeApplication") val isPipeApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "noticeType should be used to indicate if an emergency application")
  @Deprecated(message = "")
  @get:JsonProperty("isEmergencyApplication") val isEmergencyApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Use apType")
  @Deprecated(message = "")
  @get:JsonProperty("isEsapApplication") val isEsapApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("apType") val apType: ApType? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("targetLocation") val targetLocation: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("releaseType") val releaseType: ReleaseTypeOption? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("noticeType") val noticeType: Cas1ApplicationTimelinessCategory? = null,
) : UpdateApplication {
}

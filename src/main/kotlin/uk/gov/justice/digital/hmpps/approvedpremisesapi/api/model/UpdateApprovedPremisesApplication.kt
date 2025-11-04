package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param isInapplicable
 * @param isWomensApplication
 * @param isEmergencyApplication noticeType should be used to indicate if an emergency application
 * @param apType
 * @param targetLocation
 * @param releaseType
 * @param arrivalDate
 * @param noticeType
 */
data class UpdateApprovedPremisesApplication(

  @get:JsonProperty("type", required = true) override val type: UpdateApplicationType,

  @get:JsonProperty("data", required = true) override val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,

  @get:JsonProperty("isInapplicable") val isInapplicable: kotlin.Boolean? = null,

  @get:JsonProperty("isWomensApplication") val isWomensApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "noticeType should be used to indicate if an emergency application")
  @Deprecated(message = "")
  @get:JsonProperty("isEmergencyApplication") val isEmergencyApplication: kotlin.Boolean? = null,

  @get:JsonProperty("apType") val apType: ApType? = null,

  @get:JsonProperty("targetLocation") val targetLocation: kotlin.String? = null,

  @get:JsonProperty("releaseType") val releaseType: ReleaseTypeOption? = null,

  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.LocalDate? = null,

  @get:JsonProperty("noticeType") val noticeType: Cas1ApplicationTimelinessCategory? = null,
) : UpdateApplication

package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ApplicationTimelinessCategory

data class UpdateApprovedPremisesApplication(

  @get:JsonProperty("type", required = true) override val type: UpdateApplicationType,

  @get:JsonProperty("data", required = true) override val `data`: Map<String, Any>,

  @get:JsonProperty("isInapplicable") val isInapplicable: Boolean? = null,

  @get:JsonProperty("isWomensApplication") val isWomensApplication: Boolean? = null,

  @Schema(example = "null", description = "noticeType should be used to indicate if an emergency application")
  @Deprecated(message = "")
  @get:JsonProperty("isEmergencyApplication") val isEmergencyApplication: Boolean? = null,

  @get:JsonProperty("apType") val apType: ApType? = null,

  @get:JsonProperty("targetLocation") val targetLocation: String? = null,

  @get:JsonProperty("releaseType") val releaseType: ReleaseTypeOption? = null,

  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.LocalDate? = null,

  @get:JsonProperty("noticeType") val noticeType: Cas1ApplicationTimelinessCategory? = null,
) : UpdateApplication

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

  override val type: UpdateApplicationType,

  override val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,

  val isInapplicable: kotlin.Boolean? = null,

  val isWomensApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "noticeType should be used to indicate if an emergency application")
  @Deprecated(message = "")
  val isEmergencyApplication: kotlin.Boolean? = null,

  val apType: ApType? = null,

  val targetLocation: kotlin.String? = null,

  val releaseType: ReleaseTypeOption? = null,

  val arrivalDate: java.time.LocalDate? = null,

  val noticeType: Cas1ApplicationTimelinessCategory? = null,
) : UpdateApplication

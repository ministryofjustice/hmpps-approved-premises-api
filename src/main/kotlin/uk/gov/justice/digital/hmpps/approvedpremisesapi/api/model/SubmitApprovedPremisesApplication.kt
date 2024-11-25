package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param targetLocation
 * @param releaseType
 * @param sentenceType
 * @param isPipeApplication Use apType
 * @param isWomensApplication
 * @param isEmergencyApplication noticeType should be used to indicate if this an emergency application
 * @param isEsapApplication Use apType
 * @param apType
 * @param situation
 * @param arrivalDate
 * @param apAreaId If the user's ap area id is incorrect, they can optionally override it for the application
 * @param applicantUserDetails
 * @param caseManagerIsNotApplicant
 * @param caseManagerUserDetails
 * @param noticeType
 * @param reasonForShortNotice
 * @param reasonForShortNoticeOther
 */
data class SubmitApprovedPremisesApplication(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("targetLocation", required = true) val targetLocation: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("releaseType", required = true) val releaseType: ReleaseTypeOption,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("sentenceType", required = true) val sentenceType: SentenceTypeOption,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @Schema(example = "null", description = "Use apType")
  @Deprecated(message = "")
  @get:JsonProperty("isPipeApplication") val isPipeApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isWomensApplication") val isWomensApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "noticeType should be used to indicate if this an emergency application")
  @Deprecated(message = "")
  @get:JsonProperty("isEmergencyApplication") val isEmergencyApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "Use apType")
  @Deprecated(message = "")
  @get:JsonProperty("isEsapApplication") val isEsapApplication: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("apType") val apType: ApType? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("situation") val situation: SituationOption? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "If the user's ap area id is incorrect, they can optionally override it for the application")
  @get:JsonProperty("apAreaId") val apAreaId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("applicantUserDetails") val applicantUserDetails: Cas1ApplicationUserDetails? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("caseManagerIsNotApplicant") val caseManagerIsNotApplicant: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("caseManagerUserDetails") val caseManagerUserDetails: Cas1ApplicationUserDetails? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("noticeType") val noticeType: Cas1ApplicationTimelinessCategory? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("reasonForShortNotice") val reasonForShortNotice: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("reasonForShortNoticeOther") val reasonForShortNoticeOther: kotlin.String? = null,

  @Schema(example = "null", description = "Any object that conforms to the current JSON schema for an application")
  @get:JsonProperty("translatedDocument") override val translatedDocument: kotlin.Any? = null,
) : SubmitApplication

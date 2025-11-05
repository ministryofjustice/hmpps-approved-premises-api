package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

data class SubmitApprovedPremisesApplication(
  val apType: ApType,
  val targetLocation: String,
  val releaseType: ReleaseTypeOption,
  val sentenceType: SentenceTypeOption,
  override val type: String,
  val isWomensApplication: Boolean? = null,
  @field:Schema(description = "noticeType should be used to indicate if this an emergency application")
  @Deprecated(message = "noticeType should be used to indicate if this an emergency application")
  val isEmergencyApplication: Boolean? = null,
  val situation: SituationOption? = null,
  @field:Schema(description = "If the applicant has requested a placement, this is the requested arrival date")
  val arrivalDate: java.time.LocalDate? = null,
  @field:Schema(description = "If the applicant has requested a placement, this is the requested duration in days")
  val duration: Int? = null,
  @field:Schema(description = "If the user's ap area id is incorrect, they can optionally override it for the application")
  val apAreaId: java.util.UUID? = null,
  val applicantUserDetails: Cas1ApplicationUserDetails? = null,
  val caseManagerIsNotApplicant: Boolean? = null,
  val caseManagerUserDetails: Cas1ApplicationUserDetails? = null,
  val noticeType: Cas1ApplicationTimelinessCategory? = null,
  val reasonForShortNotice: String? = null,
  val reasonForShortNoticeOther: String? = null,
  val licenseExpiryDate: java.time.LocalDate? = null,
  override val translatedDocument: Any? = null,
) : SubmitApplication

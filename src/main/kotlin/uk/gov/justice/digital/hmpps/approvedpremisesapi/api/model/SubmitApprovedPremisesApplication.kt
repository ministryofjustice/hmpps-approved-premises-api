package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ApplicationUserDetails

data class SubmitApprovedPremisesApplication(
  val apType: ApType,
  val targetLocation: String,
  val releaseType: ReleaseTypeOption,
  val sentenceType: SentenceTypeOption,
  override val type: String,
  val isWomensApplication: Boolean? = null,
  @Schema(description = "noticeType should be used to indicate if this an emergency application")
  @Deprecated(message = "noticeType should be used to indicate if this an emergency application")
  val isEmergencyApplication: Boolean? = null,
  val situation: SituationOption? = null,
  @Schema(description = "If the applicant has requested a placement, this is the requested arrival date")
  @Deprecated(message = "Use requestedPlacementPeriod.arrival instead")
  val arrivalDate: java.time.LocalDate? = null,
  @Schema(description = "The default duration for any request for placement linked to this application. This will be provided even if requestedPlacementPeriod is null")
  val duration: Int? = null,
  @Schema(description = "If the user's ap area id is incorrect, they can optionally override it for the application")
  val apAreaId: java.util.UUID? = null,
  val applicantUserDetails: Cas1ApplicationUserDetails? = null,
  val caseManagerIsNotApplicant: Boolean? = null,
  val caseManagerUserDetails: Cas1ApplicationUserDetails? = null,
  val noticeType: Cas1ApplicationTimelinessCategory? = null,
  val reasonForShortNotice: String? = null,
  val reasonForShortNoticeOther: String? = null,
  val licenseExpiryDate: java.time.LocalDate? = null,
  override val translatedDocument: Any? = null,
  @Schema(description = "The applicant can make a single request for placement as part of the initial application")
  val requestedPlacementPeriod: Cas1RequestedPlacementPeriod? = null,
) : SubmitApplication

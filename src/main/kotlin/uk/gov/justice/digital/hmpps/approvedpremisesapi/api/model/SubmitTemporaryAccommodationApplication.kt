package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param arrivalDate
 * @param probationDeliveryUnitId
 * @param summaryData Any object
 * @param isRegisteredSexOffender
 * @param needsAccessibleProperty
 * @param hasHistoryOfArson
 * @param isDutyToReferSubmitted
 * @param dutyToReferSubmissionDate
 * @param dutyToReferOutcome
 * @param isApplicationEligible
 * @param eligibilityReason
 * @param dutyToReferLocalAuthorityAreaName
 * @param personReleaseDate
 * @param isHistoryOfSexualOffence
 * @param isConcerningSexualBehaviour
 * @param isConcerningArsonBehaviour
 * @param prisonReleaseTypes
 */
data class SubmitTemporaryAccommodationApplication(

  val arrivalDate: java.time.LocalDate,

  val probationDeliveryUnitId: java.util.UUID,

  val summaryData: kotlin.Any,

  override val type: kotlin.String,

  val isRegisteredSexOffender: kotlin.Boolean? = null,

  val needsAccessibleProperty: kotlin.Boolean? = null,

  val hasHistoryOfArson: kotlin.Boolean? = null,

  val isDutyToReferSubmitted: kotlin.Boolean? = null,

  val dutyToReferSubmissionDate: java.time.LocalDate? = null,

  @Schema(example = "Pending", description = "")
  val dutyToReferOutcome: kotlin.String? = null,

  val isApplicationEligible: kotlin.Boolean? = null,

  val eligibilityReason: kotlin.String? = null,

  val dutyToReferLocalAuthorityAreaName: kotlin.String? = null,

  @Schema(example = "Wed Feb 21 00:00:00 GMT 2024", description = "")
  val personReleaseDate: java.time.LocalDate? = null,

  val isHistoryOfSexualOffence: kotlin.Boolean? = null,

  val isConcerningSexualBehaviour: kotlin.Boolean? = null,

  val isConcerningArsonBehaviour: kotlin.Boolean? = null,

  val prisonReleaseTypes: kotlin.collections.List<kotlin.String>? = null,

  override val translatedDocument: kotlin.Any? = null,
) : SubmitApplication

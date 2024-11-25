package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param arrivalDate
 * @param summaryData Any object that conforms to the current JSON schema for an application
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
 * @param pdu
 * @param probationDeliveryUnitId
 * @param isHistoryOfSexualOffence
 * @param isConcerningSexualBehaviour
 * @param isConcerningArsonBehaviour
 * @param prisonReleaseTypes
 */
data class SubmitTemporaryAccommodationApplication(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

  @Schema(example = "null", required = true, description = "Any object that conforms to the current JSON schema for an application")
  @get:JsonProperty("summaryData", required = true) val summaryData: kotlin.Any,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isRegisteredSexOffender") val isRegisteredSexOffender: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("needsAccessibleProperty") val needsAccessibleProperty: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("hasHistoryOfArson") val hasHistoryOfArson: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isDutyToReferSubmitted") val isDutyToReferSubmitted: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("dutyToReferSubmissionDate") val dutyToReferSubmissionDate: java.time.LocalDate? = null,

  @Schema(example = "Pending", description = "")
  @get:JsonProperty("dutyToReferOutcome") val dutyToReferOutcome: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isApplicationEligible") val isApplicationEligible: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("eligibilityReason") val eligibilityReason: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("dutyToReferLocalAuthorityAreaName") val dutyToReferLocalAuthorityAreaName: kotlin.String? = null,

  @Schema(example = "Wed Feb 21 00:00:00 GMT 2024", description = "")
  @get:JsonProperty("personReleaseDate") val personReleaseDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("pdu") val pdu: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("probationDeliveryUnitId") val probationDeliveryUnitId: java.util.UUID? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isHistoryOfSexualOffence") val isHistoryOfSexualOffence: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isConcerningSexualBehaviour") val isConcerningSexualBehaviour: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isConcerningArsonBehaviour") val isConcerningArsonBehaviour: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("prisonReleaseTypes") val prisonReleaseTypes: kotlin.collections.List<kotlin.String>? = null,

  @Schema(example = "null", description = "Any object that conforms to the current JSON schema for an application")
  @get:JsonProperty("translatedDocument") override val translatedDocument: kotlin.Any? = null,
) : SubmitApplication

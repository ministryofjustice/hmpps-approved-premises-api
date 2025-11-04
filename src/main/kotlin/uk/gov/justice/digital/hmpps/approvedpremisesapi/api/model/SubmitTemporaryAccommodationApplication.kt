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

  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

  @get:JsonProperty("probationDeliveryUnitId", required = true) val probationDeliveryUnitId: java.util.UUID,

  @get:JsonProperty("summaryData", required = true) val summaryData: kotlin.Any,

  @get:JsonProperty("type", required = true) override val type: kotlin.String,

  @get:JsonProperty("isRegisteredSexOffender") val isRegisteredSexOffender: kotlin.Boolean? = null,

  @get:JsonProperty("needsAccessibleProperty") val needsAccessibleProperty: kotlin.Boolean? = null,

  @get:JsonProperty("hasHistoryOfArson") val hasHistoryOfArson: kotlin.Boolean? = null,

  @get:JsonProperty("isDutyToReferSubmitted") val isDutyToReferSubmitted: kotlin.Boolean? = null,

  @get:JsonProperty("dutyToReferSubmissionDate") val dutyToReferSubmissionDate: java.time.LocalDate? = null,

  @Schema(example = "Pending", description = "")
  @get:JsonProperty("dutyToReferOutcome") val dutyToReferOutcome: kotlin.String? = null,

  @get:JsonProperty("isApplicationEligible") val isApplicationEligible: kotlin.Boolean? = null,

  @get:JsonProperty("eligibilityReason") val eligibilityReason: kotlin.String? = null,

  @get:JsonProperty("dutyToReferLocalAuthorityAreaName") val dutyToReferLocalAuthorityAreaName: kotlin.String? = null,

  @Schema(example = "Wed Feb 21 00:00:00 GMT 2024", description = "")
  @get:JsonProperty("personReleaseDate") val personReleaseDate: java.time.LocalDate? = null,

  @get:JsonProperty("isHistoryOfSexualOffence") val isHistoryOfSexualOffence: kotlin.Boolean? = null,

  @get:JsonProperty("isConcerningSexualBehaviour") val isConcerningSexualBehaviour: kotlin.Boolean? = null,

  @get:JsonProperty("isConcerningArsonBehaviour") val isConcerningArsonBehaviour: kotlin.Boolean? = null,

  @get:JsonProperty("prisonReleaseTypes") val prisonReleaseTypes: kotlin.collections.List<kotlin.String>? = null,

  @get:JsonProperty("translatedDocument") override val translatedDocument: kotlin.Any? = null,
) : SubmitApplication

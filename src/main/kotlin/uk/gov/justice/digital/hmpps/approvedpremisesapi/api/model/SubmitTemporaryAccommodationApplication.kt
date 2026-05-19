package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class SubmitTemporaryAccommodationApplication(

  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: java.time.LocalDate,

  @get:JsonProperty("probationDeliveryUnitId", required = true) val probationDeliveryUnitId: java.util.UUID,

  @get:JsonProperty("summaryData", required = true) val summaryData: Any,

  @get:JsonProperty("type", required = true) override val type: String,

  @get:JsonProperty("isRegisteredSexOffender") val isRegisteredSexOffender: Boolean? = null,

  @get:JsonProperty("needsAccessibleProperty") val needsAccessibleProperty: Boolean? = null,

  @get:JsonProperty("hasHistoryOfArson") val hasHistoryOfArson: Boolean? = null,

  @get:JsonProperty("isDutyToReferSubmitted") val isDutyToReferSubmitted: Boolean? = null,

  @get:JsonProperty("dutyToReferSubmissionDate") val dutyToReferSubmissionDate: java.time.LocalDate? = null,

  @Schema(example = "Pending", description = "")
  @get:JsonProperty("dutyToReferOutcome") val dutyToReferOutcome: String? = null,

  @get:JsonProperty("isApplicationEligible") val isApplicationEligible: Boolean? = null,

  @get:JsonProperty("eligibilityReason") val eligibilityReason: String? = null,

  @get:JsonProperty("dutyToReferLocalAuthorityAreaName") val dutyToReferLocalAuthorityAreaName: String? = null,

  @Schema(example = "Wed Feb 21 00:00:00 GMT 2024", description = "")
  @get:JsonProperty("personReleaseDate") val personReleaseDate: java.time.LocalDate? = null,

  @get:JsonProperty("isHistoryOfSexualOffence") val isHistoryOfSexualOffence: Boolean? = null,

  @get:JsonProperty("isConcerningSexualBehaviour") val isConcerningSexualBehaviour: Boolean? = null,

  @get:JsonProperty("isConcerningArsonBehaviour") val isConcerningArsonBehaviour: Boolean? = null,

  @get:JsonProperty("prisonReleaseTypes") val prisonReleaseTypes: List<String>? = null,

  @get:JsonProperty("translatedDocument") override val translatedDocument: Any? = null,
) : SubmitApplication

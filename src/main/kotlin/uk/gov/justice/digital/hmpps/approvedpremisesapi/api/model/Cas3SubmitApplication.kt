package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

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
 * @param translatedDocument Any object
 */
data class Cas3SubmitApplication(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("arrivalDate", required = true) val arrivalDate: LocalDate,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("probationDeliveryUnitId", required = true) val probationDeliveryUnitId: UUID,

    @Schema(example = "null", required = true, description = "Any object")
    @get:JsonProperty("summaryData", required = true) val summaryData: Any,

    @Schema(example = "null", description = "")
    @get:JsonProperty("isRegisteredSexOffender") val isRegisteredSexOffender: Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("needsAccessibleProperty") val needsAccessibleProperty: Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("hasHistoryOfArson") val hasHistoryOfArson: Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("isDutyToReferSubmitted") val isDutyToReferSubmitted: Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("dutyToReferSubmissionDate") val dutyToReferSubmissionDate: LocalDate? = null,

    @Schema(example = "Pending", description = "")
    @get:JsonProperty("dutyToReferOutcome") val dutyToReferOutcome: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("isApplicationEligible") val isApplicationEligible: Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("eligibilityReason") val eligibilityReason: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("dutyToReferLocalAuthorityAreaName") val dutyToReferLocalAuthorityAreaName: String? = null,

    @Schema(example = "Wed Feb 21 00:00:00 GMT 2024", description = "")
    @get:JsonProperty("personReleaseDate") val personReleaseDate: LocalDate? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("isHistoryOfSexualOffence") val isHistoryOfSexualOffence: Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("isConcerningSexualBehaviour") val isConcerningSexualBehaviour: Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("isConcerningArsonBehaviour") val isConcerningArsonBehaviour: Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("prisonReleaseTypes") val prisonReleaseTypes: List<String>? = null,

    @Schema(example = "null", description = "Any object")
    @get:JsonProperty("translatedDocument") val translatedDocument: Any? = null
    ) {

}


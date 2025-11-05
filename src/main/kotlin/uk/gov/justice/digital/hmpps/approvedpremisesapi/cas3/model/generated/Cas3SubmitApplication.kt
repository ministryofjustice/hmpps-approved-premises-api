package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

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

  @get:JsonProperty("arrivalDate", required = true) val arrivalDate: LocalDate,

  @get:JsonProperty("probationDeliveryUnitId", required = true) val probationDeliveryUnitId: UUID,

  @get:JsonProperty("summaryData", required = true) val summaryData: Any,

  @get:JsonProperty("isRegisteredSexOffender") val isRegisteredSexOffender: Boolean? = null,

  @get:JsonProperty("needsAccessibleProperty") val needsAccessibleProperty: Boolean? = null,

  @get:JsonProperty("hasHistoryOfArson") val hasHistoryOfArson: Boolean? = null,

  @get:JsonProperty("isDutyToReferSubmitted") val isDutyToReferSubmitted: Boolean? = null,

  @get:JsonProperty("dutyToReferSubmissionDate") val dutyToReferSubmissionDate: LocalDate? = null,

  @field:Schema(example = "Pending", description = "")
  @get:JsonProperty("dutyToReferOutcome") val dutyToReferOutcome: String? = null,

  @get:JsonProperty("isApplicationEligible") val isApplicationEligible: Boolean? = null,

  @get:JsonProperty("eligibilityReason") val eligibilityReason: String? = null,

  @get:JsonProperty("dutyToReferLocalAuthorityAreaName") val dutyToReferLocalAuthorityAreaName: String? = null,

  @field:Schema(example = "Wed Feb 21 00:00:00 GMT 2024", description = "")
  @get:JsonProperty("personReleaseDate") val personReleaseDate: LocalDate? = null,

  @get:JsonProperty("isHistoryOfSexualOffence") val isHistoryOfSexualOffence: Boolean? = null,

  @get:JsonProperty("isConcerningSexualBehaviour") val isConcerningSexualBehaviour: Boolean? = null,

  @get:JsonProperty("isConcerningArsonBehaviour") val isConcerningArsonBehaviour: Boolean? = null,

  @get:JsonProperty("prisonReleaseTypes") val prisonReleaseTypes: List<String>? = null,

  @get:JsonProperty("translatedDocument") val translatedDocument: Any? = null,

  val outOfRegionPduId: UUID? = null,

  val outOfRegionProbationRegionId: UUID? = null,

)

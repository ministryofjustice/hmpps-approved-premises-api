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

  val arrivalDate: LocalDate,

  val probationDeliveryUnitId: UUID,

  val summaryData: Any,

  val isRegisteredSexOffender: Boolean? = null,

  val needsAccessibleProperty: Boolean? = null,

  val hasHistoryOfArson: Boolean? = null,

  val isDutyToReferSubmitted: Boolean? = null,

  val dutyToReferSubmissionDate: LocalDate? = null,

  @Schema(example = "Pending", description = "")
  val dutyToReferOutcome: String? = null,

  val isApplicationEligible: Boolean? = null,

  val eligibilityReason: String? = null,

  val dutyToReferLocalAuthorityAreaName: String? = null,

  @Schema(example = "Wed Feb 21 00:00:00 GMT 2024", description = "")
  val personReleaseDate: LocalDate? = null,

  val isHistoryOfSexualOffence: Boolean? = null,

  val isConcerningSexualBehaviour: Boolean? = null,

  val isConcerningArsonBehaviour: Boolean? = null,

  val prisonReleaseTypes: List<String>? = null,

  val translatedDocument: Any? = null,

  val outOfRegionPduId: UUID? = null,

  val outOfRegionProbationRegionId: UUID? = null,

)

package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Licence(
  val id: Long?,
  val kind: String?,
  val licenceType: LicenceType?,
  val policyVersion: String?,
  val version: String?,
  val statusCode: LicenceStatus?,
  val prisonNumber: String?,
  val bookingId: Long?,
  val crn: String?,
  val approvedByUsername: String?,
  val approvedDateTime: LocalDateTime?,
  val createdByUsername: String?,
  val createdDateTime: LocalDateTime?,
  val updatedByUsername: String?,
  val updatedDateTime: LocalDateTime?,
  val licenceStartDate: LocalDate?,
  val isInPssPeriod: Boolean?,
  val conditions: LicenceConditions?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LicenceConditions(
  @field:JsonProperty("AP") val apConditions: LicenceConditionsBlock?,
  @field:JsonProperty("PSS") val pssConditions: LicenceConditionsBlock?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LicenceConditionsBlock(
  val standard: List<StandardCondition>?,
  val additional: List<AdditionalCondition>?,
  val bespoke: List<BespokeCondition>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StandardCondition(
  val code: String?,
  val text: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AdditionalCondition(
  val id: Long?,
  val type: String?,
  val text: String?,
  val code: String?,
  val category: String?,
  val restrictions: String?,
  val hasImageUpload: Boolean?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BespokeCondition(
  val text: String?,
)

enum class LicenceType {
  AP,
  PSS,
  AP_PSS,
}

enum class LicenceStatus {
  IN_PROGRESS,
  SUBMITTED,
  APPROVED,
  ACTIVE,
  REJECTED,
  INACTIVE,
  RECALLED,
  VARIATION_IN_PROGRESS,
  VARIATION_SUBMITTED,
  VARIATION_REJECTED,
  VARIATION_APPROVED,
  NOT_STARTED,
  TIMED_OUT,
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDate
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Licence(
  val id: Long,
  val kind: String?,
  val licenceType: LicenceType,
  val policyVersion: String?,
  val version: String?,
  val statusCode: LicenceStatus,
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
  val conditions: LicenceConditions,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LicenceConditions(
  @field:JsonProperty("AP") val apConditions: ApConditions,
  @field:JsonProperty("PSS") val pssConditions: PssConditions,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApConditions(
  val standard: List<StandardCondition>,
  val additional: List<AdditionalCondition>,
  val bespoke: List<BespokeCondition>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PssConditions(
  val standard: List<StandardCondition>,
  val additional: List<AdditionalCondition>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StandardCondition(
  val code: String?,
  val text: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BespokeCondition(
  val text: String?,
)

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "type",
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
)
@JsonSubTypes(
  JsonSubTypes.Type(
    value = GenericAdditionalCondition::class,
    name = ConditionTypes.STANDARD,
  ),
  JsonSubTypes.Type(
    value = ElectronicMonitoringAdditionalConditionWithRestriction::class,
    name = ConditionTypes.ELECTRONIC_MONITORING,
  ),
  JsonSubTypes.Type(
    value = MultipleExclusionZoneAdditionalCondition::class,
    name = ConditionTypes.MULTIPLE_EXCLUSION_ZONE,
  ),
  JsonSubTypes.Type(
    value = SingleUploadAdditionalCondition::class,
    name = ConditionTypes.SINGLE_UPLOAD,
  ),
)
sealed interface AdditionalCondition {
  val id: Long
  val category: String
  val code: String
  val text: String
  val type: String
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(ConditionTypes.STANDARD)
data class GenericAdditionalCondition(
  override val id: Long,
  override val category: String,
  override val code: String,
  override val text: String,
) : AdditionalCondition {
  override val type: String = ConditionTypes.STANDARD
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(ConditionTypes.ELECTRONIC_MONITORING)
data class ElectronicMonitoringAdditionalConditionWithRestriction(
  override val id: Long,
  override val category: String,
  override val code: String,
  override val text: String,
  val restrictions: List<ElectronicMonitoringType>,
) : AdditionalCondition {
  override val type: String = ConditionTypes.ELECTRONIC_MONITORING
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(ConditionTypes.MULTIPLE_EXCLUSION_ZONE)
data class MultipleExclusionZoneAdditionalCondition(
  override val id: Long,
  override val category: String,
  override val code: String,
  override val text: String,
  val hasImageUpload: Boolean,
) : AdditionalCondition {
  override val type: String = ConditionTypes.MULTIPLE_EXCLUSION_ZONE
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(ConditionTypes.SINGLE_UPLOAD)
data class SingleUploadAdditionalCondition(
  override val id: Long,
  override val category: String,
  override val code: String,
  override val text: String,
  val hasImageUpload: Boolean,
) : AdditionalCondition {
  override val type: String = ConditionTypes.SINGLE_UPLOAD
}

object ConditionTypes {
  const val ELECTRONIC_MONITORING = "ELECTRONIC_MONITORING"
  const val MULTIPLE_EXCLUSION_ZONE = "MULTIPLE_EXCLUSION_ZONE"
  const val SINGLE_UPLOAD = "SINGLE_UPLOAD"
  const val STANDARD = "STANDARD"
}

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

enum class ElectronicMonitoringType(val value: String) {
  EXCLUSION_ZONE("exclusion zone"),
  CURFEW("curfew"),
  LOCATION_MONITORING("location monitoring"),
  ATTENDANCE_AT_APPOINTMENTS("attendance at appointments"),
  ALCOHOL_MONITORING("alcohol monitoring"),
  ALCOHOL_ABSTINENCE("alcohol abstinence"),
  ;

  companion object {
    @JsonCreator
    @JvmStatic
    fun fromValue(value: String): ElectronicMonitoringType = entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
      ?: throw IllegalArgumentException("Unknown ElectronicMonitoringType: $value")
  }

  @JsonValue
  fun toJson(): String = value
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.health

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class DietAndAllergyResponse(
  val dietAndAllergy: DietAndAllergyDto? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DietAndAllergyDto(
  val foodAllergies: ValueWithMetadata<List<DietaryItemDto>>?,
  val medicalDietaryRequirements: ValueWithMetadata<List<DietaryItemDto>>?,
  val personalisedDietaryRequirements: ValueWithMetadata<List<DietaryItemDto>>?,
  val cateringInstructions: ValueWithMetadata<String>?,
  val topLevelLocation: String?,
  val lastAdmissionDate: LocalDate?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ValueWithMetadata<T>(
  @JsonProperty("value")
  val data: T?,
  val lastModifiedAt: OffsetDateTime?,
  val lastModifiedBy: String?,
  val lastModifiedPrisonId: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DietaryItemDto(
  val value: CodeDescriptionDto,
  val comment: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CodeDescriptionDto(
  val id: String,
  val code: String,
  val description: String,
)

package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.health.CodeDescriptionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.health.DietAndAllergyDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.health.DietAndAllergyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.health.DietaryItemDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.health.ValueWithMetadata
import java.time.LocalDate
import java.time.OffsetDateTime

class DietAndAllergyResponseFactory : Factory<DietAndAllergyResponse> {
  private var dietAndAllergy: Yielded<DietAndAllergyDto?> = { DietAndAllergyDtoFactory().produce() }

  fun withDietAndAllergy(dietAndAllergy: DietAndAllergyDto?) = apply {
    this.dietAndAllergy = { dietAndAllergy }
  }

  override fun produce() = DietAndAllergyResponse(
    dietAndAllergy = this.dietAndAllergy(),
  )
}

class DietAndAllergyDtoFactory : Factory<DietAndAllergyDto> {
  private var foodAllergies: Yielded<ValueWithMetadata<List<DietaryItemDto>>?> = { null }
  private var medicalDietaryRequirements: Yielded<ValueWithMetadata<List<DietaryItemDto>>?> = { null }
  private var personalisedDietaryRequirements: Yielded<ValueWithMetadata<List<DietaryItemDto>>?> = { null }
  private var cateringInstructions: Yielded<ValueWithMetadata<String>?> = { null }
  private var topLevelLocation: Yielded<String?> = { null }
  private var lastAdmissionDate: Yielded<LocalDate?> = { null }

  fun withFoodAllergies(foodAllergies: ValueWithMetadata<List<DietaryItemDto>>?) = apply {
    this.foodAllergies = { foodAllergies }
  }

  fun withMedicalDietaryRequirements(medicalDietaryRequirements: ValueWithMetadata<List<DietaryItemDto>>?) = apply {
    this.medicalDietaryRequirements = { medicalDietaryRequirements }
  }

  fun withPersonalisedDietaryRequirements(personalisedDietaryRequirements: ValueWithMetadata<List<DietaryItemDto>>?) = apply {
    this.personalisedDietaryRequirements = { personalisedDietaryRequirements }
  }

  fun withCateringInstructions(cateringInstructions: ValueWithMetadata<String>?) = apply {
    this.cateringInstructions = { cateringInstructions }
  }

  fun withTopLevelLocation(topLevelLocation: String?) = apply {
    this.topLevelLocation = { topLevelLocation }
  }

  fun withLastAdmissionDate(lastAdmissionDate: LocalDate?) = apply {
    this.lastAdmissionDate = { lastAdmissionDate }
  }

  override fun produce() = DietAndAllergyDto(
    foodAllergies = this.foodAllergies(),
    medicalDietaryRequirements = this.medicalDietaryRequirements(),
    personalisedDietaryRequirements = this.personalisedDietaryRequirements(),
    cateringInstructions = this.cateringInstructions(),
    topLevelLocation = this.topLevelLocation(),
    lastAdmissionDate = this.lastAdmissionDate(),
  )
}

class DietaryItemDtoFactory : Factory<DietaryItemDto> {
  private var value: Yielded<CodeDescriptionDto> = {
    CodeDescriptionDto(id = "1", code = "NUT", description = "Nut allergy")
  }
  private var comment: Yielded<String?> = { "severe" }

  fun withValue(value: CodeDescriptionDto) = apply {
    this.value = { value }
  }

  fun withComment(comment: String?) = apply {
    this.comment = { comment }
  }

  override fun produce() = DietaryItemDto(
    value = this.value(),
    comment = this.comment(),
  )
}

fun <T> ValueWithMetadata<T>.factory(data: T) = ValueWithMetadata(
  data = data,
  lastModifiedAt = OffsetDateTime.now(),
  lastModifiedBy = "user1",
  lastModifiedPrisonId = "MDI",
)

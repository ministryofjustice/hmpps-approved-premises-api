package uk.gov.justice.digital.hmpps.approvedpremisesapi.results

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors

sealed interface ValidatableActionResult<EntityType> {
  data class Success<EntityType>(val entity: EntityType) : ValidatableActionResult<EntityType>
  data class FieldValidationError<EntityType>(val validationMessages: ValidationErrors) :
    ValidatableActionResult<EntityType>
  data class GeneralValidationError<EntityType>(val message: String) : ValidatableActionResult<EntityType>
}

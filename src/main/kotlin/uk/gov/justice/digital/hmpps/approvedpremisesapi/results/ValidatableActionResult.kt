package uk.gov.justice.digital.hmpps.approvedpremisesapi.results

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import java.util.UUID

sealed interface ValidatableActionResult<EntityType> {
  fun <T> translateError(): ValidatableActionResult<T> = when (this) {
    is Success -> throw RuntimeException("Cannot translate Success")
    is FieldValidationError -> FieldValidationError(this.validationMessages)
    is GeneralValidationError -> GeneralValidationError(this.message)
    is ConflictError -> ConflictError(this.conflictingEntityId, this.message)
  }

  data class Success<EntityType>(val entity: EntityType) : ValidatableActionResult<EntityType>
  data class FieldValidationError<EntityType>(val validationMessages: ValidationErrors) :
    ValidatableActionResult<EntityType>
  data class GeneralValidationError<EntityType>(val message: String) : ValidatableActionResult<EntityType>
  data class ConflictError<EntityType>(val conflictingEntityId: UUID, val message: String) : ValidatableActionResult<EntityType>
}

fun extractMessage(validatableActionResult: ValidatableActionResult<*>): String? = when (validatableActionResult) {
  is ValidatableActionResult.Success -> null
  is ValidatableActionResult.FieldValidationError -> validatableActionResult.validationMessages.toString()
  is ValidatableActionResult.GeneralValidationError -> validatableActionResult.message
  is ValidatableActionResult.ConflictError -> validatableActionResult.message
}

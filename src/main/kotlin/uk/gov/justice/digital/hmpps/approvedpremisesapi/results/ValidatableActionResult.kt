package uk.gov.justice.digital.hmpps.approvedpremisesapi.results

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import java.util.UUID

@Deprecated("The ValidatableActionResult and AuthorisableActionResult have been replaced by CasResult, which effectively flattens these two classes into one")
sealed interface ValidatableActionResult<EntityType> {

  @Deprecated("Replaced by CasResult.Success", ReplaceWith("uk.gov.justice.digital.hmpps.approvedpremisesapi.CasResult.Success"))
  data class Success<EntityType>(val entity: EntityType) : ValidatableActionResult<EntityType>
  sealed interface ValidatableActionResultError<EntityType> : ValidatableActionResult<EntityType>

  @Deprecated("Replaced by CasResult.FieldValidationError", ReplaceWith("uk.gov.justice.digital.hmpps.approvedpremisesapi.CasResult.FieldValidationError"))
  data class FieldValidationError<EntityType>(val validationMessages: ValidationErrors) : ValidatableActionResultError<EntityType>

  @Deprecated("Replaced by CasResult.GeneralValidationError", ReplaceWith("uk.gov.justice.digital.hmpps.approvedpremisesapi.CasResult.GeneralValidationError"))
  data class GeneralValidationError<EntityType>(val message: String) : ValidatableActionResultError<EntityType>

  @Deprecated("Replaced by CasResult.ConflictError", ReplaceWith("uk.gov.justice.digital.hmpps.approvedpremisesapi.CasResult.ConflictError"))
  data class ConflictError<EntityType>(val conflictingEntityId: UUID, val message: String) : ValidatableActionResultError<EntityType>
}

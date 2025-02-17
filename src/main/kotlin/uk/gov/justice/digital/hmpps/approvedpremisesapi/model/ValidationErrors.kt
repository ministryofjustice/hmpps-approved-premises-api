package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.util.UUID

@JvmInline
value class ValidationErrors(private val errorMap: MutableMap<String, String>) : MutableMap<String, String> by errorMap {
  constructor() : this(mutableMapOf())

  override fun toString() = errorMap.map { "${it.key}: ${it.value}" }.joinToString(",")
}

private fun singleValidationErrorOf(propertyNameToMessage: Pair<String, String>) = ValidationErrors().apply { this[propertyNameToMessage.first] = propertyNameToMessage.second }

class ValidatedScope<EntityType> {
  val validationErrors = ValidationErrors()

  val fieldValidationError: ValidatableActionResult.FieldValidationError<EntityType> = ValidatableActionResult.FieldValidationError(validationErrors)

  infix fun success(entity: EntityType) = ValidatableActionResult.Success(entity)
  infix fun generalError(message: String) = ValidatableActionResult.GeneralValidationError<EntityType>(message)
  infix fun String.hasValidationError(message: String) = validationErrors.put(this, message)
  infix fun String.hasSingleValidationError(message: String) = ValidatableActionResult.FieldValidationError<EntityType>(singleValidationErrorOf(this to message))
  infix fun UUID.hasConflictError(message: String) = ValidatableActionResult.ConflictError<EntityType>(this, message)
}

@Deprecated("Use of ValidatableActionResult is deprecated. Callers should use CasResult and validatedCasResult", ReplaceWith("validatedCasResult"))
inline fun <EntityType> validated(scope: ValidatedScope<EntityType>.() -> ValidatableActionResult<EntityType>): ValidatableActionResult<EntityType> = scope(ValidatedScope())

class CasResultValidatedScope<EntityType> {
  val validationErrors = ValidationErrors()

  val fieldValidationError: CasResult.FieldValidationError<EntityType> = CasResult.FieldValidationError(validationErrors)

  infix fun success(entity: EntityType) = CasResult.Success(entity)
  infix fun generalError(message: String) = CasResult.GeneralValidationError<EntityType>(message)
  infix fun String.hasValidationError(message: String) = validationErrors.put(this, message)
  infix fun String.hasSingleValidationError(message: String) = CasResult.FieldValidationError<EntityType>(singleValidationErrorOf(this to message))
  infix fun UUID.hasConflictError(message: String) = CasResult.ConflictError<EntityType>(this, message)
}

inline fun <EntityType> validatedCasResult(scope: CasResultValidatedScope<EntityType>.() -> CasResult<EntityType>): CasResult<EntityType> = scope(CasResultValidatedScope())

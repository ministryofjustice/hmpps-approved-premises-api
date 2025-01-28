package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult

@Deprecated("Update calling code to use CasResult", ReplaceWith("extractEntityFromCasResult"))
fun <EntityType> extractEntityFromAuthorisableActionResult(result: AuthorisableActionResult<EntityType>) = when (result) {
  is AuthorisableActionResult.Success -> result.entity
  is AuthorisableActionResult.NotFound -> throw NotFoundProblem(result.id.toString(), result.entityType.toString())
  is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
}

@Deprecated("Update calling code to use CasResult", ReplaceWith("extractEntityFromCasResult"))
fun <EntityType> extractEntityFromValidatableActionResult(result: ValidatableActionResult<EntityType>) = when (result) {
  is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = result.message)
  is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = result.validationMessages)
  is ValidatableActionResult.ConflictError -> throw ConflictProblem(id = result.conflictingEntityId, conflictReason = result.message)
  is ValidatableActionResult.Success -> result.entity
}

@Deprecated("Update calling code to use CasResult", ReplaceWith("ensureEntityFromCasResultIsSuccess"))
fun <EntityType> ensureEntityFromNestedAuthorisableValidatableActionResultIsSuccess(result: AuthorisableActionResult<ValidatableActionResult<EntityType>>) {
  extractEntityFromNestedAuthorisableValidatableActionResult(result)
}

@Deprecated("Update calling code to use CasResult", ReplaceWith("ensureEntityFromCasResultIsSuccess"))
fun <EntityType> ensureEntityFromValidatableActionResultIsSuccess(result: ValidatableActionResult<EntityType>) {
  extractEntityFromValidatableActionResult(result)
}

@Deprecated("Update calling code to use CasResult", ReplaceWith("extractEntityFromCasResult"))
fun <EntityType> extractEntityFromNestedAuthorisableValidatableActionResult(result: AuthorisableActionResult<ValidatableActionResult<EntityType>>): EntityType {
  val validatableResult = extractEntityFromAuthorisableActionResult(result)
  return extractEntityFromValidatableActionResult(validatableResult)
}

fun <EntityType> ensureEntityFromCasResultIsSuccess(result: CasResult<EntityType>) {
  extractEntityFromCasResult(result)
}

fun <EntityType> extractEntityFromCasResult(result: CasResult<EntityType>) = when (result) {
  is CasResult.Success -> result.value
  is CasResult.NotFound -> throw NotFoundProblem(result.id, result.entityType)
  is CasResult.Unauthorised -> throw ForbiddenProblem(result.message)
  is CasResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = result.message)
  is CasResult.FieldValidationError -> throw BadRequestProblem(invalidParams = result.validationMessages)
  is CasResult.ConflictError -> throw ConflictProblem(id = result.conflictingEntityId, conflictReason = result.message)
}

fun extractMessageFromCasResult(result: CasResult<*>): String? = when (result) {
  is CasResult.Success -> null
  is CasResult.FieldValidationError -> result.validationMessages.toString()
  is CasResult.GeneralValidationError -> result.message
  is CasResult.ConflictError -> result.message
  is CasResult.NotFound -> "${result.entityType} ${result.id} not found"
  is CasResult.Unauthorised -> "Unauthorised"
}

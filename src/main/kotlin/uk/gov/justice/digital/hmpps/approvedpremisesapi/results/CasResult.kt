package uk.gov.justice.digital.hmpps.approvedpremisesapi.results

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.util.UUID

/**
 * CasResult is an amalgamation of [ValidatableActionResult] and [AuthorisableActionResult].
 *
 * [extractEntityFromCasResult] can be used to extract the success value or throw
 * an exception, matching the existing behaviour of other 'extractEntity' functions
 *
 * Using this approach to return success or errors removes the needs to embed a
 * [ValidatableActionResult] inside an [AuthorisableActionResult.Success], making it
 * easier to determine if a given result is success or error without having to dig
 * down into the contained values.
 */
sealed interface CasResult<SuccessType> {
  data class Success<SuccessType>(val value: SuccessType) : CasResult<SuccessType>
  sealed interface Error<SuccessType> : CasResult<SuccessType>
  data class FieldValidationError<SuccessType>(val validationMessages: ValidationErrors) : Error<SuccessType>
  data class GeneralValidationError<SuccessType>(val message: String) : Error<SuccessType>
  data class ConflictError<SuccessType>(val conflictingEntityId: UUID, val message: String) : Error<SuccessType>
  class Unauthorised<SuccessType> : Error<SuccessType>
  data class NotFound<SuccessType>(val entityType: String? = null, val id: String? = null) : Error<SuccessType>
}

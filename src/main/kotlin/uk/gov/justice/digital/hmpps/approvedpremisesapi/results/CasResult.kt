package uk.gov.justice.digital.hmpps.approvedpremisesapi.results

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
  sealed interface Error<SuccessType> : CasResult<SuccessType> {
    // This is safe as the generic is irrelevant on Error types
    @Suppress("UNCHECKED_CAST")
    fun <R> reviseType(): CasResult<R> = this as Error<R>
  }
  data class FieldValidationError<SuccessType>(val validationMessages: Map<String, String>) : Error<SuccessType>
  data class GeneralValidationError<SuccessType>(val message: String) : Error<SuccessType>
  data class ConflictError<SuccessType>(val conflictingEntityId: UUID, val message: String) : Error<SuccessType>
  data class Unauthorised<SuccessType>(val message: String? = null) : Error<SuccessType>
  data class NotFound<SuccessType>(val entityType: String, val id: String) : Error<SuccessType>
}

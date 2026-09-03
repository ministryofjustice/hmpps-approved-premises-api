package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import java.util.UUID

object CasResultFactory {

  @JvmStatic
  fun <T> oneOfEachErrorType(): List<CasResult.Error<T>> = listOf(
    CasResult.GeneralValidationError("oh dear"),
    CasResult.NotFound("type", "id"),
    CasResult.Unauthorised(),
    CasResult.Cas3FieldValidationError(emptyMap()),
    CasResult.ConflictError(UUID.randomUUID(), "message"),
    CasResult.FieldValidationError(emptyMap()),
  )
}

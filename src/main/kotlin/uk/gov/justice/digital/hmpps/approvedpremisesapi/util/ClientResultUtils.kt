package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult

fun <ResultType> extractResultFromClientResultOrThrow(result: ClientResult<ResultType>) = when (result) {
  is ClientResult.Failure -> result.throwException()
  is ClientResult.Success -> result.body
}

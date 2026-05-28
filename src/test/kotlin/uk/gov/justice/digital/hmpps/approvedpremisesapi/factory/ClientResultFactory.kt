package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult

object ClientResultFactory {
  fun <T> forbidden() = statusCode<T>(HttpStatus.FORBIDDEN)

  fun <T> notFound() = statusCode<T>(HttpStatus.NOT_FOUND)

  fun <T> statusCode(httpStatus: HttpStatus) = ClientResult.Failure.StatusCode<T>(
    method = HttpMethod.GET,
    path = "/undefined",
    status = httpStatus,
    body = null,
  )

  fun <T> failureOther() = ClientResult.Failure.Other<T>(
    method = HttpMethod.GET,
    path = "/undefined",
    exception = Exception("oh dear"),
  )
}

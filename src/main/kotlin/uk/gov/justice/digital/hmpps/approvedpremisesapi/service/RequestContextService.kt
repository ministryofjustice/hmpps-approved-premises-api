package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem

@Service
class RequestContextService(
  private val currentRequest: HttpServletRequest,
) {

  fun getServiceForRequest(): ServiceName? {
    val headerValue = currentRequest.getHeader("X-Service-Name")
    return ServiceName.entries.firstOrNull { it.value == headerValue }
  }

  fun ensureCas3Request() = if (!isCas3Request()) {
    throw ForbiddenProblem("This endpoint only supports CAS3 requests")
  } else {
    // do nothing
  }

  fun isCas3Request() = getServiceForRequest() == ServiceName.temporaryAccommodation
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import javax.servlet.http.HttpServletRequest

@Service
class RequestContextService(
  private val currentRequest: HttpServletRequest,
) {

  fun getServiceForRequest(): ServiceName? {
    val headerValue = currentRequest.getHeader("X-Service-Name")
    return ServiceName.entries.firstOrNull { it.value == headerValue }
  }
}

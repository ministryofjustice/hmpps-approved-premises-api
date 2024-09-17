package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RequestContextService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.io.IOException

@Component
class UserVersionFilter(
  private val userService: UserService,
  private val requestContextService: RequestContextService,
) : OncePerRequestFilter() {

  @Throws(ServletException::class, IOException::class)
  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    when (requestContextService.getServiceForRequest()) {
      ServiceName.approvedPremises -> {
        addUserHeaders(response)
      }
      else -> { }
    }
    filterChain.doFilter(request, response)
  }

  private fun addUserHeaders(response: HttpServletResponse) {
    val userVersionInfo = userService.getUserForRequestVersionInfo()
    userVersionInfo?.let {
      response.setHeader("X-CAS-User-ID", it.userId.toString())
      response.setHeader("X-CAS-User-Version", it.version.toString())
    }
  }
}

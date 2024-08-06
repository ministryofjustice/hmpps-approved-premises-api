package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RequestContextService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class UserVersionFilter(
  private val userService: UserService,
  private val requestContextService: RequestContextService,
) : OncePerRequestFilter() {

  @Throws(ServletException::class, IOException::class)
  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    when (requestContextService.getServiceForRequest()) {
      ServiceName.approvedPremises -> {
        addUserVersionHeader(response)
      }
      else -> { }
    }
    filterChain.doFilter(request, response)
  }

  private fun addUserVersionHeader(response: HttpServletResponse) {
    userService.getUserForRequestVersion()?.let {
      response.setHeader("X-CAS-User-Version", it.toString())
    }
  }
}

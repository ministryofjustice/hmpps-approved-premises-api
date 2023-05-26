package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(1)
class ContentCachingFilter : OncePerRequestFilter() {
  override fun doFilterInternal(
    httpServletRequest: HttpServletRequest,
    httpServletResponse: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    filterChain.doFilter(ContentCachingRequestWrapper(httpServletRequest), httpServletResponse)
  }
}

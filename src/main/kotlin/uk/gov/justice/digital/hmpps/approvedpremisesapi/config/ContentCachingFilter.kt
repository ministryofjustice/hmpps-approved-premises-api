package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper

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

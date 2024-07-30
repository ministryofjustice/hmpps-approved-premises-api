package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import io.sentry.IScope
import io.sentry.Sentry
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.util.UUID

@Component
class MDCHandlerInterceptor(
  private val userService: UserService,
) : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    appendRequestDiagnosticsToMDC(request)

    return super.preHandle(request, response, handler)
  }

  private fun appendRequestDiagnosticsToMDC(request: HttpServletRequest) {
    MDC.clear()
    MDC.put("request.id", UUID.randomUUID().toString())
    MDC.put("request.uri", request.requestURI)
    MDC.put("request.method", request.method)
    MDC.put("request.pathPattern", request.getPathPattern())
    request.getPathParams()?.entries?.forEach {
      MDC.put("request.params.${it.key}", it.value.toString())
    }
    request.parameterMap.entries.forEach {
      MDC.put("request.params.${it.key}", it.value.joinToString(","))
    }
    MDC.put("request.serviceName", request.getHeader("X-Service-Name") ?: "Not specified")

    Sentry.configureScope { scope: IScope -> scope.setTag("request.serviceName", request.getHeader("X-Service-Name") ?: "Not specified") }

    MDC.put("request.user", userService.getUserForRequestOrNull()?.deliusUsername ?: "Anonymous")
  }

  private fun HttpServletRequest.getPathPattern() =
    this.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String
  private fun HttpServletRequest.getPathParams() =
    this.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<*, *>
}

@Component
class MDCHandlerInterceptorConfig(
  private val interceptor: MDCHandlerInterceptor,
) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(interceptor)
  }
}

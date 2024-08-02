package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.handler.MappedInterceptor
import java.lang.Exception

@Configuration
class InterceptorConfig {
  @Bean
  fun mappedInterceptor(loggingHandlerInterceptor: HandlerInterceptor) = MappedInterceptor(
    null,
    arrayOf("/health/**"),
    loggingHandlerInterceptor,
  )
}

@Component
class LoggingHandlerInterceptor : HandlerInterceptor {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun afterCompletion(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
    ex: Exception?,
  ) {
    log.info("${request.method} ${request.requestURI} - ${response.status}")
    super.afterCompletion(request, response, handler, ex)
  }
}

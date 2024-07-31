package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.handler.MappedInterceptor
import java.lang.Exception
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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

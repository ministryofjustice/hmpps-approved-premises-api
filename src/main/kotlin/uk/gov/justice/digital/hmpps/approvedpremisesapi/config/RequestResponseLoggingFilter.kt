package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import jakarta.annotation.PostConstruct
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

@Component
@ConditionalOnProperty(name = ["log-request-response"])
class RequestResponseLoggingFilter(
  val sentryService: SentryService,
  val environmentService: EnvironmentService,
) : OncePerRequestFilter() {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  @PostConstruct
  fun logStartup() {
    if (environmentService.isNotATestEnvironment()) {
      error("Request/Response logging is enabled. This should only be enabled in local environments")
    }

    sentryService.captureErrorMessage("Request/Response logging is enabled. This should only be enabled in local environments")
  }

  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    if (request.requestURI.contains("health")) {
      return filterChain.doFilter(request, response)
    }

    val requestWrapper = ContentCachingRequestWrapper(request)
    val responseWrapper = ContentCachingResponseWrapper(response)

    filterChain.doFilter(requestWrapper, responseWrapper)
    logRequestResponse(requestWrapper, responseWrapper)
  }

  private fun logRequestResponse(
    requestWrapper: ContentCachingRequestWrapper,
    responseWrapper: ContentCachingResponseWrapper,
  ) {
    logRequest(requestWrapper)
    logResponse(responseWrapper)

    if (requestWrapper.isAsyncStarted) {
      requestWrapper.asyncContext.addListener(
        object : AsyncListener {
          override fun onComplete(p0: AsyncEvent?) {
            responseWrapper.copyBodyToResponse()
          }

          override fun onTimeout(p0: AsyncEvent?) {
            // deliberately empty
          }

          override fun onError(p0: AsyncEvent?) {
            // deliberately empty
          }

          override fun onStartAsync(p0: AsyncEvent?) {
            // deliberately empty
          }
        },
      )
    } else {
      responseWrapper.copyBodyToResponse()
    }
  }

  private fun logRequest(
    requestWrapper: ContentCachingRequestWrapper,
  ) {
    val request = requestWrapper.request
    log.info("Request {}", String(requestWrapper.contentAsByteArray))

    if (request is HttpServletRequest) {
      log.trace("Authorization: {}", request.getHeader("authorization"))
    }
  }

  private fun logResponse(
    responseWrapper: ContentCachingResponseWrapper,
  ) {
    log.info("Response Headers {}", responseWrapper.headerNames.map { "$it:  ${responseWrapper.getHeaders(it)}" })
    val contentType = responseWrapper.contentType
    if (contentType == "application/json") {
      log.info("Response Body {}", String(responseWrapper.contentAsByteArray))
    } else {
      log.info("Response Body not logged as content type is $contentType")
    }
  }
}

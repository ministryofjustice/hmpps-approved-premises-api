package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import java.io.IOException
import javax.annotation.PostConstruct
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@ConditionalOnProperty(name = ["log-request-response"])
class RequestResponseLoggingFilter(val sentryService: SentryService) : OncePerRequestFilter() {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  @PostConstruct
  fun logStartup() {
    sentryService.captureErrorMessage("Request/Response logging is enabled. This should only be enabled in local environments")
  }

  @Throws(ServletException::class, IOException::class)
  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    if (request.requestURI.contains("health")) {
      return filterChain.doFilter(request, response)
    }

    val requestWrapper = ContentCachingRequestWrapper(request)
    val responseWrapper = ContentCachingResponseWrapper(response)

    filterChain.doFilter(requestWrapper, responseWrapper)
    logResponse(requestWrapper, responseWrapper)
  }

  @Throws(IOException::class)
  private fun logResponse(
    requestWrapper: ContentCachingRequestWrapper,
    responseWrapper: ContentCachingResponseWrapper,
  ) {
    log.info("Request {}", String(requestWrapper.contentAsByteArray))
    val contentType = responseWrapper.contentType
    if (contentType == "application/json") {
      log.info("Response Body {}", String(responseWrapper.contentAsByteArray))
    } else {
      log.info("Response Body not logged as content type is $contentType")
    }
    log.info("Response Headers {}", responseWrapper.headerNames.map { "$it:  ${responseWrapper.getHeaders(it)}" })

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
}

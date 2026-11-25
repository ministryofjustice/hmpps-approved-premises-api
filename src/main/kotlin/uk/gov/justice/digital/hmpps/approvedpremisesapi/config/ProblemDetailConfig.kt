package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponse
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.net.URI

@Configuration
class ProblemDetailConfig : WebMvcConfigurer {
  override fun addErrorResponseInterceptors(interceptors: MutableList<ErrorResponse.Interceptor>) {
    interceptors.add(
      object : ErrorResponse.Interceptor {
        // Spring MVC invokes the request-aware overload when rendering a response.
        override fun handleError(detail: ProblemDetail, errorResponse: ErrorResponse?) = Unit

        override fun handleError(detail: ProblemDetail, errorResponse: ErrorResponse?, request: HttpRequest) {
          if (detail.instance == null) {
            detail.instance = URI.create(request.uri.rawPath)
          }
        }
      },
    )
  }
}

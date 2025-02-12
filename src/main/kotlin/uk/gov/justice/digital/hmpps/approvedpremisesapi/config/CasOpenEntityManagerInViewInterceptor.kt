package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Component
class CasOpenEntityManagerInViewInterceptor : OpenEntityManagerInViewInterceptor() {
  override fun preHandle(request: WebRequest) {
    if (!shouldApply(request)) {
      return
    }
    super.preHandle(request)
  }

  override fun afterCompletion(request: WebRequest, ex: Exception?) {
    if (!shouldApply(request)) {
      return
    }
    super.afterCompletion(request, ex)
  }

  private fun shouldApply(webRequest: WebRequest): Boolean {
    return if (webRequest is ServletWebRequest) {
      return !webRequest.request.requestURI.startsWith("/cas1")
    } else {
      true
    }
  }
}

@Configuration
class CasOpenEntityManagerInViewInterceptorConfigurer {
  @Bean
  fun openEntityManagerInViewInterceptor(
    interceptor: CasOpenEntityManagerInViewInterceptor,
  ): WebMvcConfigurer {
    return object : WebMvcConfigurer {
      override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addWebRequestInterceptor(interceptor)
      }
    }
  }
}

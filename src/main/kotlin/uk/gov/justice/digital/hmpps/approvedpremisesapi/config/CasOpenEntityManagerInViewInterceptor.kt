package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Component
class CasOpenEntityManagerInViewInterceptor : OpenEntityManagerInViewInterceptor() {
  override fun preHandle(request: WebRequest) {
    super.preHandle(request)
  }

  override fun afterCompletion(request: WebRequest, ex: Exception?) {
    super.afterCompletion(request, ex)
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

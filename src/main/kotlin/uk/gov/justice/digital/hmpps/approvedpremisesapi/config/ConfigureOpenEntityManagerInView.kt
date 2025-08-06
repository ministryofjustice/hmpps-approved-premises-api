package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class ConfigureOpenEntityManagerInView {
  @Bean
  fun openEntityManagerInViewInterceptor(interceptor: CasOpenEntityManagerInViewInterceptor): WebMvcConfigurer = object : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
      registry.addWebRequestInterceptor(interceptor)
        /**
         * Ideally all endpoints would be excluded from this interceptor, but
         * this requires updates to ensure they stop relying on lazy loading
         * before this can be done
         *
         * Note that care needs to be taken as integration tests currently
         * won't capture all lazy loading issues, see
         * https://dsdmoj.atlassian.net/browse/APS-1127?focusedCommentId=563114
         */
        .excludePathPatterns()
    }
  }
}

@Component
class CasOpenEntityManagerInViewInterceptor : OpenEntityManagerInViewInterceptor()

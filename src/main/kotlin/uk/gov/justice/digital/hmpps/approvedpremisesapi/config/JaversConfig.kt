package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.javers.spring.auditable.AuthorProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder

@Configuration
class JaversConfig {
  @Bean
  fun authorProvider(): AuthorProvider = AuthorProvider {
    SecurityContextHolder.getContext().authentication?.name ?: "System"
  }
}

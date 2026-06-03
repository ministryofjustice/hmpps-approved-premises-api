package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.javers.spring.auditable.AuthorProvider
import org.javers.spring.auditable.CommitPropertiesProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReleasePlanEntity

@Configuration
class JaversConfig {
  @Bean
  fun authorProvider(): AuthorProvider = AuthorProvider {
    SecurityContextHolder.getContext().authentication?.name ?: "System"
  }
}

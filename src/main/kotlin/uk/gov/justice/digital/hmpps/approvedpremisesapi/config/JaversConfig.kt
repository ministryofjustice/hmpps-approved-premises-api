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

  @Bean
  fun commitPropertiesProvider(): CommitPropertiesProvider = object : CommitPropertiesProvider {

    override fun provideForCommittedObject(savedDomainObject: Any): Map<String, String> = propertiesFor(savedDomainObject)

    override fun provideForDeletedObject(deletedDomainObject: Any): Map<String, String> = propertiesFor(deletedDomainObject)

    private fun propertiesFor(domainObject: Any): Map<String, String> = when (domainObject) {
      is ReleasePlanEntity -> releasePlanCommitProperties(domainObject)
      else -> emptyMap()
    }
  }

  private fun releasePlanCommitProperties(releasePlan: ReleasePlanEntity): Map<String, String> = mapOf(
    "spaceBookingId" to releasePlan.spaceBooking.id.toString(),
  )
}

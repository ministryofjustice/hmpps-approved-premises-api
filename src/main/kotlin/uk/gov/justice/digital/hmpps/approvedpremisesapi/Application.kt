package uk.gov.justice.digital.hmpps.approvedpremisesapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

const val SYSTEM_USERNAME = "COMMUNITY_ACCOMMODATION_API"

@EnableJpaRepositories(
  repositoryFactoryBeanClass =
  EnversRevisionRepositoryFactoryBean::class,
)
@SpringBootApplication
class Application

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
  runApplication<Application>(*args)
}

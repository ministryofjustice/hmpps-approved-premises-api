package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.SimpleDriverDataSource

@Configuration
class ConnectionPoolConfiguration {
  @Bean
  @ConfigurationProperties("datasource")
  fun developerPortalDataSourceProperties(): DataSourceProperties = DataSourceProperties()

  @Bean
  fun getDriverDataSource(@Qualifier("spring.datasource-org.springframework.boot.autoconfigure.jdbc.DataSourceProperties") properties: DataSourceProperties) = properties.initializeDataSourceBuilder().type(SimpleDriverDataSource::class.java).build()
}

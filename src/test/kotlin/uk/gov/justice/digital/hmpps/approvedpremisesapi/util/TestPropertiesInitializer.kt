package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.util.SocketUtils

class TestPropertiesInitializer : ApplicationContextInitializer<ConfigurableApplicationContext?> {
  override fun initialize(applicationContext: ConfigurableApplicationContext?) {
    val wiremockPort = SocketUtils.findAvailableTcpPort()
    val databaseName = setupDatabase()

    println("TestPropertiesInitializer - Wiremock Port is: $wiremockPort")

    // TODO: Programmatically load these from application-test.yml and replace the #WIREMOCK_PORTs
    TestPropertyValues
      .of(
        mapOf(
          "wiremock.port" to wiremockPort.toString(),
          "spring.datasource.url" to "jdbc:postgresql://localhost:5433/$databaseName",
          "services.community-api.base-url" to "http://localhost:$wiremockPort",
          "services.hmpps-tier.base-url" to "http://localhost:$wiremockPort",
          "services.prisons-api.base-url" to "http://localhost:$wiremockPort",
          "services.case-notes.base-url" to "http://localhost:$wiremockPort",
          "services.ap-delius-context-api.base-url" to "http://localhost:$wiremockPort",
          "services.ap-oasys-context-api.base-url" to "http://localhost:$wiremockPort",
          "hmpps.auth.url" to "http://localhost:$wiremockPort/auth",
        )
      ).applyTo(applicationContext)
  }

  private fun setupDatabase(): String {
    val driver = DriverManagerDataSource().apply {
      setDriverClassName("org.postgresql.Driver")
      url = "jdbc:postgresql://localhost:5433/postgres"
      username = "integration_test"
      password = "integration_test_password"
    }
1
    val jdbcTemplate = JdbcTemplate(driver)

    val databaseName = "approved_premises_integration_test_${randomStringLowerCase(6)}"

    jdbcTemplate.execute("CREATE DATABASE $databaseName")

    return databaseName
  }
}

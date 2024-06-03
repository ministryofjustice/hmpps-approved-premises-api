package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import jakarta.annotation.PreDestroy
import org.springframework.boot.env.OriginTrackedMapPropertySource
import org.springframework.boot.origin.OriginTrackedValue
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.WiremockPortHolder

class TestPropertiesInitializer : ApplicationContextInitializer<ConfigurableApplicationContext?> {
  private var postgresPort = System.getenv("POSTGRES_PORT") ?: "5433"

  override fun initialize(applicationContext: ConfigurableApplicationContext?) {
    val wiremockPort = WiremockPortHolder.getPort()

    val databaseName = setupDatabase()

    val upstreamServiceUrlsToOverride = mutableMapOf<String, String>()

    applicationContext!!.environment.propertySources
      .filterIsInstance<OriginTrackedMapPropertySource>()
      .filter { it.name.contains("application-test.yml") }
      .forEach { propertyFile ->
        propertyFile.source.forEach { (propertyName, propertyValue) ->
          if (propertyName.startsWith("services.") && (propertyValue as? OriginTrackedValue)?.value is String) {
            upstreamServiceUrlsToOverride[propertyName] = ((propertyValue as OriginTrackedValue).value as String).replace("#WIREMOCK_PORT", wiremockPort.toString())
            return@forEach
          }

          if (propertyName == "hmpps.auth.url" && (propertyValue as? OriginTrackedValue)?.value is String) {
            upstreamServiceUrlsToOverride[propertyName] = ((propertyValue as OriginTrackedValue).value as String).replace("#WIREMOCK_PORT", wiremockPort.toString())
          }
        }
      }

    TestPropertyValues
      .of(
        mapOf(
          "wiremock.port" to wiremockPort.toString(),
          "preemptive-cache-key-prefix" to wiremockPort.toString(),
          "hmpps.sqs.topics.domainevents.arn" to "arn:aws:sns:eu-west-2:000000000000:domainevents-int-test-${randomStringLowerCase(10)}",
          "spring.datasource.url" to "jdbc:postgresql://localhost:$postgresPort/$databaseName",
        ) + upstreamServiceUrlsToOverride,
      ).applyTo(applicationContext)
  }

  private fun setupDatabase(): String {
    val driver = DriverManagerDataSource().apply {
      setDriverClassName("org.postgresql.Driver")
      url = "jdbc:postgresql://localhost:$postgresPort/postgres"
      username = "integration_test"
      password = "integration_test_password"
    }

    val jdbcTemplate = JdbcTemplate(driver)

    val databaseName = "approved_premises_integration_test_${randomStringLowerCase(6)}"

    jdbcTemplate.execute("CREATE DATABASE $databaseName")

    return databaseName
  }
}

@Component
class TestPropertiesDestructor {
  @PreDestroy
  fun destroy() = WiremockPortHolder.releasePort()
}

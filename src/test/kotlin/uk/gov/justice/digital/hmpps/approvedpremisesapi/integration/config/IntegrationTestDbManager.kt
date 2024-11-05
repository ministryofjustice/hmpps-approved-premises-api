package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isPerClass
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase

/**
 * When tests are first ran against a new postgres instance, an 'it_template' instance is created,
 * with all flyway migrations applied. This 'it_template' database is then used to create a new
 * database for each test execution, ensuring each test starts with a database in a known state.
 *
 * Note that the 'api_user' should not be used to interrogate the integration test database
 * because this will create a connection to the 'api_template' database which then
 * blocks its usage as a template. Instead, use 'integration_test_monitor:integration_test_monitor_password'
 */
object IntegrationTestDbManager {

  private const val CONFIG_PATH_DATASOURCE_URL = "spring.datasource.url"
  private const val CONFIG_PATH_DATABASE_NAME = "spring.datasource.database-name"
  private const val CONFIG_PATH_TEMPLATE_DATABASE_NAME = "spring.datasource.template-database-name"

  private val log = LoggerFactory.getLogger(this::class.java)

  private var postgresPort = System.getenv("POSTGRES_PORT") ?: "5433"
  private var inCi = System.getenv("CI")?.isNotBlank() ?: false

  /**
   * Initialise a database template from flyway and then create a new uniquely named
   * database from this template to be used for tests by the calling context
   *
   * This function returns a map of spring configuration entries to be applied
   * to the context
   */
  fun initialiseDatabase(): Map<String, String> {
    val databasePostfix = "${System.currentTimeMillis()}_${randomStringLowerCase(4)}"

    val databaseName = "it_$databasePostfix"
    val templateDatabaseName = "it_template_$databasePostfix"

    val databaseUrl = "jdbc:postgresql://localhost:$postgresPort/$databaseName"
    ensureTemplateDatabaseExists(templateDatabaseName)
    recreateDatabaseFromTemplate(
      databaseName = databaseName,
      templateDatabaseName = templateDatabaseName,
    )

    return mapOf(
      CONFIG_PATH_DATASOURCE_URL to databaseUrl,
      CONFIG_PATH_DATABASE_NAME to databaseName,
      CONFIG_PATH_TEMPLATE_DATABASE_NAME to templateDatabaseName,
    )
  }

  private fun ensureTemplateDatabaseExists(templateDatabaseName: String) {
    val jdbcTemplate = getJdbcTemplate(databaseName = "postgres")

    val templateExists = jdbcTemplate.queryForObject("SELECT count(*) FROM pg_database WHERE datname='$templateDatabaseName'", Int::class.java)
    if (templateExists == 1) {
      log.info("Template database already exists")
      return
    }

    log.info("Creating template database $templateDatabaseName")

    jdbcTemplate.execute("CREATE DATABASE $templateDatabaseName")

    // only allow api_user to connect to the template database
    // this is required because _any_ connections to the api_template database blocks its usage
    jdbcTemplate.execute("REVOKE CONNECT ON DATABASE $templateDatabaseName FROM PUBLIC")
    jdbcTemplate.execute("GRANT CONNECT ON DATABASE $templateDatabaseName TO api_user")

    if (!inCi) {
      jdbcTemplate.execute("ALTER DATABASE $templateDatabaseName OWNER TO integration_test_monitor")
    }

    // this should match config in application config
    val flyway = Flyway.configure()
      .locations(*arrayOf("/db/migration/all", "/db/migration/integration"))
      .repeatableSqlMigrationPrefix("R")
      .sqlMigrationPrefix("")
      .outOfOrder(true)
      .placeholderReplacement(true)
      .dataSource(getJdbcTemplate(databaseName = templateDatabaseName).dataSource).load()
    flyway.migrate()
  }

  fun recreateDatabaseFromTemplate(context: ApplicationContext) =
    recreateDatabaseFromTemplate(
      databaseName = context.environment.getProperty(CONFIG_PATH_DATABASE_NAME)!!,
      templateDatabaseName = context.environment.getProperty(CONFIG_PATH_TEMPLATE_DATABASE_NAME)!!,
    )

  fun recreateDatabaseFromTemplate(
    databaseName: String,
    templateDatabaseName: String,
  ) {
    val jdbcTemplate = getJdbcTemplate("postgres")

    jdbcTemplate.execute("DROP DATABASE IF EXISTS $databaseName  WITH (FORCE)")

    log.info("Creating database $databaseName from template $templateDatabaseName")
    jdbcTemplate.execute("CREATE DATABASE $databaseName TEMPLATE $templateDatabaseName")

    if (!inCi) {
      val apiDatabaseJdbcTemplate = getJdbcTemplate(databaseName)
      apiDatabaseJdbcTemplate.execute("ALTER DATABASE $databaseName OWNER TO integration_test_monitor")
      apiDatabaseJdbcTemplate.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO integration_test_monitor")
    }

    log.info("Database created")
  }

  private fun getJdbcTemplate(databaseName: String) = JdbcTemplate(
    DriverManagerDataSource().apply {
      setDriverClassName("org.postgresql.Driver")
      url = "jdbc:postgresql://localhost:$postgresPort/$databaseName"
      username = "api_user"
      password = "api_user_password"
    },
  )

  class IntegrationTestListener : BeforeAllCallback, BeforeEachCallback {
    override fun beforeAll(context: ExtensionContext?) {
      if (isPerClass(context)) {
        recreateDatabaseFromTemplate(SpringExtension.getApplicationContext(context!!))
      }
    }

    override fun beforeEach(context: ExtensionContext?) {
      if (!isPerClass(context)) {
        recreateDatabaseFromTemplate(SpringExtension.getApplicationContext(context!!))
      }
    }
  }
}

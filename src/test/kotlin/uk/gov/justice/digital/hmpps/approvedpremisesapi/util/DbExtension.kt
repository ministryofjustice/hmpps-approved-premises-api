package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.flywaydb.core.Flyway
import org.hibernate.internal.SessionFactoryImpl
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicBoolean
import javax.persistence.EntityManagerFactory
import javax.persistence.metamodel.EntityType
import javax.sql.DataSource

class DbExtension : BeforeAllCallback, BeforeEachCallback {
  companion object {
    private val STARTED = AtomicBoolean(false)
    private val LOGGER = LoggerFactory.getLogger(DbExtension::class.java)

    private val TABLES_TO_IGNORE = listOf(
      TableData("flyway_schema_history"),
      TableData("spatial_ref_sys"),
    )

    private lateinit var INITIAL_DB_STATE: Map<EntityType<*>, List<Any>>
  }

  override fun beforeAll(context: ExtensionContext?) {
    if (STARTED.compareAndSet(false, true)) {
      val applicationContext = SpringExtension.getApplicationContext(context!!)

      runFlywayMigrations(applicationContext)
      getInitialDatabaseState(applicationContext)
    }
  }

  override fun beforeEach(context: ExtensionContext?) {
    val applicationContext = SpringExtension.getApplicationContext(context!!)
    val dataSource = getDatasource(applicationContext)
    cleanDatabase(dataSource)
    setInitialDatabaseState(applicationContext)
  }

  private fun runFlywayMigrations(applicationContext: ApplicationContext) {
    val flyway = applicationContext.getBean(Flyway::class.java)
    val jdbcTemplate = applicationContext.getBean(JdbcTemplate::class.java)

    flyway.clean()

    jdbcTemplate.execute("CREATE EXTENSION postgis;")

    flyway.migrate()
  }

  private fun getInitialDatabaseState(applicationContext: ApplicationContext) {
    val entityManagerFactory = applicationContext.getBean(EntityManagerFactory::class.java)
    val sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl::class.java)

    sessionFactory.openSession().use { session ->
      INITIAL_DB_STATE = session.metamodel.entities.associateWith {
        val query = session.criteriaBuilder.createQuery()
        val root = query.from(it)
        query.select(root)
        val results = session.createQuery(query).resultList

        LOGGER.debug("Found ${results.count()} ${it.name} instances")

        results
      }
    }
  }

  private fun setInitialDatabaseState(applicationContext: ApplicationContext) {
    val entityManagerFactory = applicationContext.getBean(EntityManagerFactory::class.java)
    val sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl::class.java)

    sessionFactory.openSession().use { session ->
      val transaction = session.beginTransaction()

      INITIAL_DB_STATE.forEach { (entityType, entities) ->
        entities.forEach { session.save(it) }
        LOGGER.debug("Restoring ${entities.count()} ${entityType.name} instances")
      }

      session.flush()
      transaction.commit()
    }
  }

  private fun getDatasource(applicationContext: ApplicationContext): DataSource {
    val configurableEnvironment = applicationContext.environment

    val binder = Binder.get(configurableEnvironment)
    val dataSourceProperties = binder
      .bind("spring.datasource", Bindable.of(DataSourceProperties::class.java))
      .get()

    val pgSimpleDataSource = PGSimpleDataSource()
    pgSimpleDataSource.setUrl(dataSourceProperties.url)
    pgSimpleDataSource.user = dataSourceProperties.username
    pgSimpleDataSource.password = dataSourceProperties.password
    return pgSimpleDataSource
  }

  private fun cleanDatabase(dataSource: DataSource) {
    try {
      dataSource.connection.use { connection ->
        connection.autoCommit = false
        val tablesToClean = loadTablesToClean(connection)
        cleanTablesData(tablesToClean, connection)
        connection.commit()
      }
    } catch (e: SQLException) {
      LOGGER.error(String.format("Failed to clean database due to error: \"%s\"", e.message))
      e.printStackTrace()
    }
  }

  private fun loadTablesToClean(connection: Connection): List<TableData> {
    val databaseMetaData = connection.metaData
    val resultSet = databaseMetaData.getTables(connection.catalog, null, null, arrayOf("TABLE"))

    val tablesToClean = mutableListOf<TableData>()
    while (resultSet.next()) {
      val table = TableData(
        schema = resultSet.getString("TABLE_SCHEM"),
        name = resultSet.getString("TABLE_NAME"),
      )

      if (!TABLES_TO_IGNORE.contains(table)) {
        tablesToClean.add(table)
      }
    }

    return tablesToClean
  }

  private fun cleanTablesData(tablesNames: List<TableData>, connection: Connection) {
    if (tablesNames.isEmpty()) {
      return
    }

    val statement = tablesNames.joinToString(separator = ", ", prefix = "TRUNCATE ") { it.fullyQualifiedTableName }

    connection.prepareStatement(statement).execute()
  }

  data class TableData(val name: String, val schema: String? = "public") {
    val fullyQualifiedTableName =
      if (schema != null) "$schema.$name" else name
  }
}

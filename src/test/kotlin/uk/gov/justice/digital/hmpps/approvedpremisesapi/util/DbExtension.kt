package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.flywaydb.core.Flyway
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
import javax.sql.DataSource

class DbExtension : BeforeAllCallback, BeforeEachCallback {
  companion object {
    private val STARTED = AtomicBoolean(false)
    private val LOGGER = LoggerFactory.getLogger(DbExtension::class.java)

    private val TABLES_TO_IGNORE = listOf(
      TableData("flyway_schema_history"),
      TableData("spatial_ref_sys"),
    )

    private lateinit var INITIAL_DB_STATE: Map<TableData, TableResults>
  }

  override fun beforeAll(context: ExtensionContext?) {
    if (STARTED.compareAndSet(false, true)) {
      val applicationContext = SpringExtension.getApplicationContext(context!!)

      runFlywayMigrations(applicationContext)
      val dataSource = getDatasource(applicationContext)
      getInitialDatabaseState(dataSource)
    }

    if (isPerClass(context)) {
      setupDatabase(context)
    }
  }

  override fun beforeEach(context: ExtensionContext?) {
    if (!isPerClass(context)) {
      setupDatabase(context)
    }
  }

  private fun setupDatabase(context: ExtensionContext?) {
    val applicationContext = SpringExtension.getApplicationContext(context!!)
    val dataSource = getDatasource(applicationContext)
    cleanDatabase(dataSource)
    setInitialDatabaseState(dataSource)
  }

  private fun runFlywayMigrations(applicationContext: ApplicationContext) {
    val flyway = applicationContext.getBean(Flyway::class.java)
    val jdbcTemplate = applicationContext.getBean(JdbcTemplate::class.java)

    flyway.clean()

    jdbcTemplate.execute("CREATE EXTENSION postgis;")

    flyway.migrate()
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

  private fun getInitialDatabaseState(dataSource: DataSource) {
    try {
      dataSource.connection.use { connection ->
        connection.autoCommit = false
        val tablesToCapture = getTableData(connection)
        INITIAL_DB_STATE = getDatabaseState(tablesToCapture, connection)
        connection.commit()
      }
    } catch (e: SQLException) {
      LOGGER.error(String.format("Failed to capture initial database state due to error: \"%s\"", e.message))
      e.printStackTrace()
    }
  }

  private fun setInitialDatabaseState(dataSource: DataSource) {
    try {
      dataSource.connection.use { connection ->
        connection.autoCommit = false
        INITIAL_DB_STATE.keys.forEach { table ->
          table.foreignKeys.forEach { fk ->
            connection.prepareStatement("ALTER TABLE ${table.fullyQualifiedTableName} ALTER CONSTRAINT $fk DEFERRABLE INITIALLY IMMEDIATE;")
              .execute()
          }
        }
        connection.prepareStatement("SET CONSTRAINTS ALL DEFERRED;").execute()
        INITIAL_DB_STATE.values.forEach { it.insert(connection) }
        connection.commit()
      }
    } catch (e: SQLException) {
      LOGGER.error(String.format("Failed to restore initial database state due to error: \"%s\"", e.message))
      e.printStackTrace()
    }
  }

  private fun cleanDatabase(dataSource: DataSource) {
    try {
      dataSource.connection.use { connection ->
        connection.autoCommit = false
        val tablesToClean = getTableData(connection)
        cleanTablesData(tablesToClean, connection)
        connection.commit()
      }
    } catch (e: SQLException) {
      LOGGER.error(String.format("Failed to clean database due to error: \"%s\"", e.message))
      e.printStackTrace()
    }
  }

  private fun getTableData(connection: Connection): List<TableData> {
    val databaseMetaData = connection.metaData
    val resultSet = databaseMetaData.getTables(connection.catalog, null, null, arrayOf("TABLE"))

    val tablesToClean = mutableListOf<TableData>()
    while (resultSet.next()) {
      val schema = resultSet.getString("TABLE_SCHEM")
      val name = resultSet.getString("TABLE_NAME")

      val constraintsResultSet = databaseMetaData.getCrossReference(connection.catalog, null, null, null, schema, name)
      val foreignKeys = mutableListOf<String>()
      while (constraintsResultSet.next()) {
        foreignKeys.add(constraintsResultSet.getString("FK_NAME"))
      }

      val table = TableData(
        schema = schema,
        name = name,
        foreignKeys = foreignKeys,
      )

      if (!TABLES_TO_IGNORE.contains(table)) {
        tablesToClean.add(table)
      }
    }

    return tablesToClean
  }

  private fun getDatabaseState(tablesNames: List<TableData>, connection: Connection): Map<TableData, TableResults> {
    return tablesNames.associateWith { table ->
      val resultSet = connection.prepareStatement("SELECT * FROM ${table.fullyQualifiedTableName}").executeQuery()
      val columns = (1..resultSet.metaData.columnCount).map { resultSet.metaData.getColumnName(it) }

      val results = mutableListOf<Map<String, Any>>()

      while (resultSet.next()) {
        val row = columns.associateWith { key -> resultSet.getObject(key) }
        results.add(row)
      }

      TableResults(table, columns, results)
    }
  }

  private fun cleanTablesData(tablesNames: List<TableData>, connection: Connection) {
    if (tablesNames.isEmpty()) {
      return
    }

    val statement = tablesNames.joinToString(separator = ", ", prefix = "TRUNCATE ") { it.fullyQualifiedTableName }

    connection.prepareStatement(statement).execute()
  }

  data class TableData(val name: String, val schema: String? = "public", val foreignKeys: List<String> = listOf()) {
    val fullyQualifiedTableName =
      if (schema != null) "$schema.$name" else name
  }

  data class TableResults(val tableData: TableData, val columns: List<String>, val results: List<Map<String, Any>>) {
    fun insert(connection: Connection) {
      val sql = "INSERT INTO ${tableData.fullyQualifiedTableName} ($columnNamesSql) VALUES ($valueParametersSql);"
      println(sql)
      val statement = connection.prepareStatement(sql)

      results.forEach { result ->
        columns.map { result[it] }.forEachIndexed { i, param -> statement.setObject(i + 1, param) }
        statement.execute()
      }
    }

    private val columnNamesSql
      get() = columns.joinToString()

    private val valueParametersSql
      get() = columns.indices.joinToString { "?" }
  }
}

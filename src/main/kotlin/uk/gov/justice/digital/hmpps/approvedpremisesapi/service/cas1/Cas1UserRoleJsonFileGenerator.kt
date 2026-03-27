package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import tools.jackson.core.util.DefaultIndenter
import tools.jackson.core.util.DefaultPrettyPrinter
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Cas1UserRoleJsonFileGenerator {

  fun generate(outputPath: Path) {
    Files.createDirectories(outputPath.parent)

    val roles = UserRole.entries
      .filter { it.service == ServiceName.approvedPremises }
      .map { role ->
        mapOf(
          "name" to role.cas1ApiValue?.value,
          "permissions" to role.permissions.mapNotNull { it.cas1ApiValue?.value }.sorted(),
        )
      }

    val jsonMapper = JsonMapper.builder()
      .enable(SerializationFeature.INDENT_OUTPUT)
      .build()

    val prettyPrinter = DefaultPrettyPrinter()
    prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)

    jsonMapper.writer().with(prettyPrinter).writeValue(outputPath, roles)
  }

  companion object {
    @JvmStatic
    @Suppress("TooGenericExceptionCaught")
    fun main(args: Array<String>) {
      val outputPath = Paths.get("src/main/resources/static/codegen/built-cas1-roles.json")

      try {
        val generator = Cas1UserRoleJsonFileGenerator()
        generator.generate(outputPath)

        println("✅ Successfully generated built-cas1-roles.json at: $outputPath")
      } catch (e: Exception) {
        println("❌ Failed to generate built-cas1-roles.json: ${e.message}")
        throw e
      }
    }
  }
}

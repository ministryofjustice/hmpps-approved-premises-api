package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Cas1UserRoleJsonFileGenerator(private val objectMapper: ObjectMapper) {

  fun generate(outputPath: Path) {
    Files.createDirectories(outputPath.parent)

    val roles = UserRole.entries
      .filter { it.service == ServiceName.approvedPremises }
      .map { role ->
        mapOf(
          "name" to role.cas1ApiValue?.value,
          "permissions" to role.permissions.mapNotNull { it.cas1ApiValue?.value },
        )
      }

    val jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roles)

    Files.write(outputPath, jsonContent.toByteArray())
  }

  companion object {
    @JvmStatic
    @Suppress("TooGenericExceptionCaught")
    fun main(args: Array<String>) {
      val outputPath = Paths.get("src/main/resources/static/codegen/built-cas1-roles.json")
      val objectMapper = ObjectMapper()

      try {
        val generator = Cas1UserRoleJsonFileGenerator(objectMapper)
        generator.generate(outputPath)

        println("✅ Successfully generated built-cas1-roles.json at: $outputPath")
      } catch (e: Exception) {
        println("❌ Failed to generate built-cas1-roles.json: ${e.message}")
        throw e
      }
    }
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicRepository
import java.util.UUID

@Component
class CharacteristicsSeedJob(
  private val cas1CharacteristicRepository: Cas1CharacteristicRepository,
) : SeedJob<CharacteristicsSeedCsvRow>(
  requiredHeaders = setOf(
    "characteristic_name",
    "characteristic_property_name",
    "service_scope",
    "model_scope",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = CharacteristicsSeedCsvRow(
    name = throwIfBlank(columns["characteristic_name"]!!, "characteristic_name"),
    propertyName = columns["characteristic_property_name"]!!,
    serviceScope = throwIfBlankOrInvalid(columns["service_scope"]!!, "service_scope"),
    modelScope = throwIfBlankOrInvalid(columns["model_scope"]!!, "model_scope"),
  )

  override fun processRow(row: CharacteristicsSeedCsvRow) {
    log.info("Processing characteristic for ${row.propertyName}")
    if (row.name.isNullOrEmpty()) {
      return log.info("Skipping due to blank name")
    }

    if (row.serviceScope != Characteristic.ServiceScope.approvedMinusPremises.value) {
      throw IllegalStateException(
        "Characteristic '${row.propertyName}' has service_scope '${row.serviceScope}' " +
          "but only '${Characteristic.ServiceScope.approvedMinusPremises.value}' characteristics can be seeded into the CAS1 characteristics table",
      )
    }

    val existingCharacteristic = cas1CharacteristicRepository.findByPropertyNameAndModelScope(
      propertyName = row.propertyName,
      modelName = row.modelScope,
    )

    if (existingCharacteristic != null) {
      updateExistingCharacteristic(row, existingCharacteristic)
    } else {
      createNewCharacteristic(row)
    }
  }

  private fun throwIfBlank(value: String, requiredField: String): String {
    if (value.isNullOrBlank()) {
      throw RuntimeException("The field: '$requiredField' is required")
    }

    return value
  }

  private fun throwIfBlankOrInvalid(value: String, requiredField: String): String {
    throwIfBlank(value, requiredField)

    val valid = when (requiredField) {
      "service_scope" -> Characteristic.ServiceScope.values().map { it.value }.contains(value)
      "model_scope" -> Characteristic.ModelScope.values().map { it.value }.contains(value)
      else -> false
    }

    if (valid == false) {
      throw RuntimeException("Your '$requiredField' value: '$value' is not recognised")
    }

    return value
  }

  private fun createNewCharacteristic(
    row: CharacteristicsSeedCsvRow,
  ) {
    log.info("Creating new Characteristic: ${row.propertyName}")

    cas1CharacteristicRepository.save(
      Cas1CharacteristicEntity(
        id = UUID.randomUUID(),
        propertyName = row.propertyName,
        name = row.name,
        modelScope = row.modelScope,
        isActive = true,
      ),
    )
  }

  private fun updateExistingCharacteristic(
    row: CharacteristicsSeedCsvRow,
    existingCharacteristic: Cas1CharacteristicEntity,
  ) {
    log.info("Updating Characteristic: '${row.propertyName}' with name '${row.name}'")

    existingCharacteristic.apply {
      this.name = row.name
    }

    cas1CharacteristicRepository.save(existingCharacteristic)
  }
}

data class CharacteristicsSeedCsvRow(
  val name: String,
  val propertyName: String,
  val serviceScope: String,
  val modelScope: String,
)

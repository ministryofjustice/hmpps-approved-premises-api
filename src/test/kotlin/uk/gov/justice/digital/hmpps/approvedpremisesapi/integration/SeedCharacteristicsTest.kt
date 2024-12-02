package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCharacteristicsTest : SeedTestBase() {

  @BeforeEach
  fun removeDefaultCharacteristicsFromDatabaseMigrations() {
    characteristicRepository.deleteAll()
  }

  @Test
  fun `Attempting to seed characteristic with missing characteristic_name field fails and logs error`() {
    withCsv(
      "invalid-characteristics-missing-name",
      "characteristic_property_name,characteristic_name,service_scope,model_scope\n" +
        "hasWideDoor,Is the door to this room at least 900mm wide?,approved-premises,room\n" +
        "hasWideDoor,,temporary-accommodation,room\n",
    )

    seedService.seedData(SeedFileType.CHARACTERISTICS, "invalid-characteristics-missing-name.csv")

    assertThat(characteristicRepository.count()).isEqualTo(0)

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Unable to complete Seed Job") &&
        it.throwable != null &&
        it.throwable.message!!.contains("The field: 'characteristic_name' is required")
    }
  }

  @Test
  fun `Attempting to seed characteristic with unknown scope field fails and logs error`() {
    withCsv(
      "invalid-characteristics-unknown-scope",
      "characteristic_property_name,characteristic_name,service_scope,model_scope\n" +
        "hasWideDoor,Is the door to this room at least 900mm wide?,foo,room\n" +
        "hasWideDoor,Is the entrance wide?,temporary-accommodation,bar\n",
    )

    seedService.seedData(SeedFileType.CHARACTERISTICS, "invalid-characteristics-unknown-scope.csv")

    assertThat(characteristicRepository.count()).isEqualTo(0)

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Unable to complete Seed Job") &&
        it.throwable != null &&
        it.throwable.message!!.contains("Your 'service_scope' value: 'foo' is not recognised")
    }
  }

  @Test
  fun `Attempting to seed characteristic missing either scope field fails and logs error`() {
    withCsv(
      "invalid-characteristics-missing-scope",
      "characteristic_property_name,characteristic_name,service_scope,model_scope\n" +
        "hasWideDoor,Is the door to this room at least 900mm wide?,,room\n" +
        "hasWideDoor,Is the entrance wide?,temporary-accommodation,\n",
    )

    seedService.seedData(SeedFileType.CHARACTERISTICS, "invalid-characteristics-missing-scope.csv")

    assertThat(characteristicRepository.count()).isEqualTo(0)

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Unable to complete Seed Job") &&
        it.throwable != null &&
        it.throwable.message!!.contains("The field: 'service_scope' is required")
    }
  }

  @Test
  fun `Seeding new characteristics twice (unique propertyName, serviceScope and modelScope) succeeds without dupes`() {
    withCsv(
      "valid-characteristics",
      "characteristic_property_name,characteristic_name,service_scope,model_scope\n" +
        "hasWideDoor,Is the door to this room at least 900mm wide?,approved-premises,room\n" +
        "hasWideDoor,Is the room entrance wide?,temporary-accommodation,room\n" +
        "isIap,Is this an IAP?,approved-premises,premises\n",
    )

    seedService.seedData(SeedFileType.CHARACTERISTICS, "valid-characteristics.csv")
    seedService.seedData(SeedFileType.CHARACTERISTICS, "valid-characteristics.csv")

    val apWideDoorRoom = characteristicRepository.findByPropertyNameAndScopes(
      propertyName = "hasWideDoor",
      serviceName = "approved-premises",
      modelName = "room",
    )
    val taWideDoorRoom = characteristicRepository.findByPropertyNameAndScopes(
      propertyName = "hasWideDoor",
      serviceName = "temporary-accommodation",
      modelName = "room",
    )
    val apIapPremises = characteristicRepository.findByPropertyNameAndScopes(
      propertyName = "isIap",
      serviceName = "approved-premises",
      modelName = "premises",
    )

    assertThat(apWideDoorRoom!!.name).isEqualTo("Is the door to this room at least 900mm wide?")
    assertThat(taWideDoorRoom!!.name).isEqualTo("Is the room entrance wide?")
    assertThat(apIapPremises!!.name).isEqualTo("Is this an IAP?")

    assertThat(characteristicRepository.count()).isEqualTo(3)
  }

  @Test
  fun `Updating a characteristic name succeeds`() {
    characteristicEntityFactory.produceAndPersist {
      withModelScope("room")
      withServiceScope("approved-premises")
      withPropertyName("hasWideDoor")
      withName("Is the door wide?")
    }

    val characteristic = characteristicRepository.findByPropertyNameAndScopes(
      propertyName = "hasWideDoor",
      serviceName = "approved-premises",
      modelName = "room",
    )

    assertThat(characteristicRepository.count()).isEqualTo(1)
    assertThat(characteristic!!.name).isEqualTo("Is the door wide?")

    withCsv(
      "valid-characteristics-update",
      "characteristic_property_name,characteristic_name,service_scope,model_scope\n" +
        "hasWideDoor,Is the DOOR wide?,approved-premises,room\n",
    )

    seedService.seedData(SeedFileType.CHARACTERISTICS, "valid-characteristics-update.csv")

    val updatedCharacteristic = characteristicRepository.findByPropertyNameAndScopes(
      propertyName = "hasWideDoor",
      serviceName = "approved-premises",
      modelName = "room",
    )

    assertThat(characteristicRepository.count()).isEqualTo(1)
    assertThat(updatedCharacteristic!!.name).isEqualTo("Is the DOOR wide?")
  }
}

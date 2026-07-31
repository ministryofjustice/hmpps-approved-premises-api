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
    cas1CharacteristicRepository.deleteAll()
  }

  @Test
  fun `Attempting to seed characteristic with missing characteristic_name field fails and logs error`() {
    seed(
      SeedFileType.characteristics,
      "characteristic_property_name,characteristic_name,service_scope,model_scope\n" +
        "hasWideDoor,Is the door to this room at least 900mm wide?,approved-premises,room\n" +
        "hasWideDoor,,temporary-accommodation,room\n",
    )

    assertThat(cas1CharacteristicRepository.count()).isEqualTo(0)

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Unable to complete Seed Job") &&
        it.throwable != null &&
        it.throwable.message!!.contains("The field: 'characteristic_name' is required")
    }
  }

  @Test
  fun `Attempting to seed characteristic with unknown scope field fails and logs error`() {
    seed(
      SeedFileType.characteristics,
      "characteristic_property_name,characteristic_name,service_scope,model_scope\n" +
        "hasWideDoor,Is the door to this room at least 900mm wide?,foo,room\n" +
        "hasWideDoor,Is the entrance wide?,temporary-accommodation,bar\n",
    )

    assertThat(cas1CharacteristicRepository.count()).isEqualTo(0)

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Unable to complete Seed Job") &&
        it.throwable != null &&
        it.throwable.message!!.contains("Your 'service_scope' value: 'foo' is not recognised")
    }
  }

  @Test
  fun `Attempting to seed characteristic missing either scope field fails and logs error`() {
    seed(
      SeedFileType.characteristics,
      "characteristic_property_name,characteristic_name,service_scope,model_scope\n" +
        "hasWideDoor,Is the door to this room at least 900mm wide?,,room\n" +
        "hasWideDoor,Is the entrance wide?,temporary-accommodation,\n",
    )

    assertThat(cas1CharacteristicRepository.count()).isEqualTo(0)

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Unable to complete Seed Job") &&
        it.throwable != null &&
        it.throwable.message!!.contains("The field: 'service_scope' is required")
    }
  }

  @Test
  fun `Attempting to seed a non approved-premises characteristic fails and logs error`() {
    seed(
      SeedFileType.characteristics,
      "characteristic_property_name,characteristic_name,service_scope,model_scope\n" +
        "hasWideDoor,Is the room entrance wide?,temporary-accommodation,room\n",
    )

    assertThat(cas1CharacteristicRepository.count()).isEqualTo(0)

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message.contains("Unable to complete Seed Job") &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "has service_scope 'temporary-accommodation' but only 'approved-premises' characteristics can be seeded",
        )
    }
  }

  @Test
  fun `Seeding new characteristics twice (unique propertyName and modelScope) succeeds without dupes`() {
    val csv = "characteristic_property_name,characteristic_name,service_scope,model_scope\n" +
      "hasWideDoor,Is the door to this room at least 900mm wide?,approved-premises,room\n" +
      "hasWideDoor,Is the premises entrance wide?,approved-premises,premises\n" +
      "isIap,Is this an IAP?,approved-premises,premises\n"

    seed(SeedFileType.characteristics, csv)
    seed(SeedFileType.characteristics, csv)

    val wideDoorRoom = cas1CharacteristicRepository.findByPropertyNameAndModelScope(
      propertyName = "hasWideDoor",
      modelName = "room",
    )
    val wideDoorPremises = cas1CharacteristicRepository.findByPropertyNameAndModelScope(
      propertyName = "hasWideDoor",
      modelName = "premises",
    )
    val iapPremises = cas1CharacteristicRepository.findByPropertyNameAndModelScope(
      propertyName = "isIap",
      modelName = "premises",
    )

    assertThat(wideDoorRoom!!.name).isEqualTo("Is the door to this room at least 900mm wide?")
    assertThat(wideDoorPremises!!.name).isEqualTo("Is the premises entrance wide?")
    assertThat(iapPremises!!.name).isEqualTo("Is this an IAP?")

    assertThat(cas1CharacteristicRepository.count()).isEqualTo(3)
  }

  @Test
  fun `Updating a characteristic name succeeds`() {
    cas1CharacteristicEntityFactory.produceAndPersist {
      withModelScope("room")
      withPropertyName("hasWideDoor")
      withName("Is the door wide?")
    }

    val characteristic = cas1CharacteristicRepository.findByPropertyNameAndModelScope(
      propertyName = "hasWideDoor",
      modelName = "room",
    )

    assertThat(cas1CharacteristicRepository.count()).isEqualTo(1)
    assertThat(characteristic!!.name).isEqualTo("Is the door wide?")

    seed(
      SeedFileType.characteristics,
      "characteristic_property_name,characteristic_name,service_scope,model_scope\n" +
        "hasWideDoor,Is the DOOR wide?,approved-premises,room\n",
    )

    val updatedCharacteristic = cas1CharacteristicRepository.findByPropertyNameAndModelScope(
      propertyName = "hasWideDoor",
      modelName = "room",
    )

    assertThat(cas1CharacteristicRepository.count()).isEqualTo(1)
    assertThat(updatedCharacteristic!!.name).isEqualTo("Is the DOOR wide?")
  }
}

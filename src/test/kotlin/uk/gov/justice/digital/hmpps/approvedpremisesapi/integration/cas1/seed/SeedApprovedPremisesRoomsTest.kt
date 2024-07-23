package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApprovedPremisesRoomsSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedApprovedPremisesRoomsTest : SeedTestBase() {

  @BeforeEach
  fun removeDefaultCharacteristicsFromDatabaseMigrations() {
    characteristicRepository.deleteAll()
  }

  @Test
  fun `Attempting to create an AP room with an incorrectly service-scoped characteristic logs an error`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withApCode("NEABC")
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(apAreaEntityFactory.produceAndPersist())
        },
      )
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }

    characteristicEntityFactory.produceAndPersist {
      withId(UUID.fromString("8e04628f-2cdd-4d9a-8ae7-27689d7daa73"))
      withPropertyName("isArsonSuitable")
      withModelScope("room")
      withServiceScope("temporary-accommodation")
    }

    withCsv(
      "invalid-ap-rooms-service-scope",
      approvedPremisesRoomsSeedCsvRowsToCsv(
        listOf(
          ApprovedPremisesRoomsSeedCsvRowFactory()
            .withApCode(premises.apCode)
            .withIsArsonSuitable("yes")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesRooms, "invalid-ap-rooms-service-scope")

    assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Error on row 1:" &&
          it.throwable != null &&
          it.throwable.message == "Characteristic 'isArsonSuitable' does not exist for AP room"
      }
  }

  @Test
  fun `Attempting to create an AP room with an incorrectly model-scoped characteristic logs an error`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withApCode("NEABC")
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(apAreaEntityFactory.produceAndPersist())
        },
      )
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }

    characteristicEntityFactory.produceAndPersist {
      withId(UUID.fromString("8e04628f-2cdd-4d9a-8ae7-27689d7daa73"))
      withPropertyName("isArsonSuitable")
      withModelScope("premises")
      withServiceScope("approved-premises")
    }

    withCsv(
      "invalid-ap-rooms-model-scope",
      approvedPremisesRoomsSeedCsvRowsToCsv(
        listOf(
          ApprovedPremisesRoomsSeedCsvRowFactory()
            .withApCode(premises.apCode)
            .withIsArsonSuitable("yes")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesRooms, "invalid-ap-rooms-model-scope")

    assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Error on row 1:" &&
          it.throwable != null &&
          it.throwable.message == "Characteristic 'isArsonSuitable' does not exist for AP room"
      }
  }

  @Test
  fun `Attempting to create an AP room without an associated premises logs an error`() {
    withCsv(
      "invalid-ap-rooms-missing-premises",
      approvedPremisesRoomsSeedCsvRowsToCsv(
        listOf(
          ApprovedPremisesRoomsSeedCsvRowFactory()
            .withApCode("NON-EXISTENT")
            .produce(),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesRooms, "invalid-ap-rooms-missing-premises")

    assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Error on row 1:" &&
          it.throwable != null &&
          it.throwable.message!!.contains(
            "Error: no premises with apCode 'NON-EXISTENT' found. " +
              "Please seed premises before rooms/beds",
          )
      }
  }

  @Test
  fun `Attempting to create an AP room with an incorrect boolean value logs an error`() {
    val csvRow = ApprovedPremisesRoomsSeedCsvRowFactory()
      .withIsArsonSuitable("false")
      .produce()

    withCsv(
      "new-ap-room-invalid-boolean",
      approvedPremisesRoomsSeedCsvRowsToCsv(
        listOf(
          csvRow,
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesRooms, "new-ap-room-invalid-boolean")

    assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Seed Job" &&
          it.throwable != null &&
          it.throwable.message!!.contains("'false' is not a recognised boolean for 'isArsonSuitable' (use yes | no)")
      }
  }

  @Test
  fun `Attempting to create an AP Room missing required headers lists missing fields`() {
    withCsv(
      "new-ap-room-missing-headers",
      "apCode,bedCode,roomNumber,bedCount,isSingle,isGroundFloor,isFullyFm,hasCrib7Bedding\n" +
        "HOPE,NESPU01,1,1,yes,yes,no,no",
    )

    seedService.seedData(SeedFileType.approvedPremisesRooms, "new-ap-room-missing-headers")

    val expectedErrorMessage = "The headers provided: " +
      "[apCode, bedCode, roomNumber, bedCount, isSingle, isGroundFloor, isFullyFm, hasCrib7Bedding] " +
      "did not include required headers: " +
      "[hasSmokeDetector, isTopFloorVulnerable, isGroundFloorNrOffice, hasNearbySprinkler, isArsonSuitable, " +
      "isArsonDesignated, hasArsonInsuranceConditions, isSuitedForSexOffenders, hasEnSuite, isWheelchairAccessible, " +
      "hasWideDoor, hasStepFreeAccess, hasFixedMobilityAids, hasTurningSpace, hasCallForAssistance, " +
      "isWheelchairDesignated, isStepFreeDesignated, notes]"

    assertThat(logEntries)
      .withFailMessage("-> logEntries actually contains: $logEntries")
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Seed Job" &&
          it.throwable != null &&
          it.throwable.message!!.contains(expectedErrorMessage)
      }
  }

  @Test
  fun `Creating new AP rooms with beds persists correctly`() {
    characteristicEntityFactory.produceAndPersist {
      withPropertyName("isArsonSuitable")
      withModelScope("room")
      withServiceScope("approved-premises")
    }

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withApCode("NEABC")
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(apAreaEntityFactory.produceAndPersist())
        },
      )
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }

    val rowRoom4A = ApprovedPremisesRoomsSeedCsvRowFactory()
      .withApCode(premises.apCode)
      .withBedCode("NEABC04")
      .withIsArsonSuitable("yes")
      .withRoomNumber("4")
      .produce()

    val rowRoom4B = ApprovedPremisesRoomsSeedCsvRowFactory()
      .withApCode(premises.apCode)
      .withBedCode("NEABC05")
      .withIsArsonSuitable("yes")
      .withRoomNumber("4")
      .produce()

    val room5 = ApprovedPremisesRoomsSeedCsvRowFactory()
      .withApCode(premises.apCode)
      .withBedCode("NEABC06")
      .withRoomNumber("5")
      .withNotes("This room is very small")
      .produce()

    withCsv(
      "new-ap-rooms",
      approvedPremisesRoomsSeedCsvRowsToCsv(
        listOf(
          rowRoom4A,
          rowRoom4B,
          room5,
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesRooms, "new-ap-rooms")

    val persistedRoom5 = roomRepository.findByCode("NEABC-5")
    val persistedRoom4 = roomRepository.findByCode("NEABC-4")

    assertThat(persistedRoom4).isNotNull
    assertThat(persistedRoom4!!.characteristics.map { it.propertyName }).contains("isArsonSuitable")
    assertThat(persistedRoom4.beds.count()).isEqualTo(2)

    assertThat(persistedRoom5).isNotNull
    assertThat(persistedRoom5!!.notes).isEqualTo("This room is very small")
    assertThat(persistedRoom5.beds.count()).isEqualTo(1)

    assertThat(bedRepository.findAll().map { it.code }.sortedBy { it })
      .isEqualTo(listOf("NEABC04", "NEABC05", "NEABC06"))
  }

  @Test
  fun `Updating an existing AP room and beds persists correctly`() {
    characteristicRepository.deleteAll()

    val arsonCharacteristic = characteristicEntityFactory.produceAndPersist {
      withPropertyName("isArsonSuitable")
      withModelScope("room")
      withServiceScope("approved-premises")
    }

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withApCode("NEABC")
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(apAreaEntityFactory.produceAndPersist())
        },
      )
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }

    // create a room with one bed

    val preExistingRoom = roomEntityFactory.produceAndPersist {
      withCode("NEABC-4")
      withPremises(premises)
      withNotes("This is small")
    }
    preExistingRoom.characteristics.add(arsonCharacteristic)
    roomRepository.save(preExistingRoom)

    bedEntityFactory.produceAndPersist {
      withRoom(preExistingRoom)
      withCode("NEABC04")
      withName("Original Name")
    }

    val reloadedRoom = roomRepository.findByCode("NEABC-4")

    assertThat(reloadedRoom!!.characteristics.map { it.propertyName }).isEqualTo(listOf("isArsonSuitable"))
    assertThat(reloadedRoom.notes).isEqualTo("This is small")
    assertThat(reloadedRoom.beds.count()).isEqualTo(1)

    // update the room with new notes, another characteristic, change the bed's name and add an additional bed

    characteristicEntityFactory.produceAndPersist {
      withPropertyName("isGroundFloor")
      withModelScope("room")
      withServiceScope("approved-premises")
    }

    val rowRoom4A = ApprovedPremisesRoomsSeedCsvRowFactory()
      .withApCode(premises.apCode)
      .withBedCode("NEABC04")
      .withIsArsonSuitable("yes")
      .withIsGroundFloor("yes")
      .withRoomNumber("4")
      .withBedCount("5")
      .withNotes("This is large")
      .produce()

    val rowRoom4B = ApprovedPremisesRoomsSeedCsvRowFactory()
      .withApCode(premises.apCode)
      .withBedCode("NEABC05")
      .withIsArsonSuitable("yes")
      .withIsGroundFloor("yes")
      .withRoomNumber("4")
      .withNotes("This is large")
      .produce()

    withCsv(
      "updated-ap-rooms",
      approvedPremisesRoomsSeedCsvRowsToCsv(
        listOf(
          rowRoom4A,
          rowRoom4B,
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesRooms, "updated-ap-rooms")

    val updatedRoom = roomRepository.findByCode("NEABC-4")

    assertThat(updatedRoom).isNotNull
    assertThat(updatedRoom!!.notes).isEqualTo("This is large")

    assertThat(
      updatedRoom.characteristics
        .map { it.propertyName }
        .sortedBy { it },
    ).isEqualTo(listOf("isArsonSuitable", "isGroundFloor"))

    assertThat(updatedRoom.beds.count()).isEqualTo(2)
    assertThat(updatedRoom.beds.filter { it.name == "4 - 5" }.size).isEqualTo(1)
  }

  private fun approvedPremisesRoomsSeedCsvRowsToCsv(rows: List<ApprovedPremisesRoomsSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "apCode",
        "bedCode",
        "roomNumber",
        "bedCount",
        "isSingle",
        "isGroundFloor",
        "isFullyFm",
        "hasCrib7Bedding",
        "hasSmokeDetector",
        "isTopFloorVulnerable",
        "isGroundFloorNrOffice",
        "hasNearbySprinkler",
        "isArsonSuitable",
        "isArsonDesignated",
        "hasArsonInsuranceConditions",
        "isSuitedForSexOffenders",
        "hasEnSuite",
        "isWheelchairAccessible",
        "hasWideDoor",
        "hasStepFreeAccess",
        "hasFixedMobilityAids",
        "hasTurningSpace",
        "hasCallForAssistance",
        "isWheelchairDesignated",
        "isStepFreeDesignated",
        "notes",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.apCode)
        .withQuotedField(it.bedCode)
        .withQuotedField(it.roomNumber)
        .withQuotedField(it.bedCount)
        .withQuotedField(it.isSingle)
        .withQuotedField(it.isGroundFloor)
        .withQuotedField(it.isFullyFm)
        .withQuotedField(it.hasCrib7Bedding)
        .withQuotedField(it.hasSmokeDetector)
        .withQuotedField(it.isTopFloorVulnerable)
        .withQuotedField(it.isGroundFloorNrOffice)
        .withQuotedField(it.hasNearbySprinkler)
        .withQuotedField(it.isArsonSuitable)
        .withQuotedField(it.isArsonDesignated)
        .withQuotedField(it.hasArsonInsuranceConditions)
        .withQuotedField(it.isSuitedForSexOffenders)
        .withQuotedField(it.hasEnSuite)
        .withQuotedField(it.isWheelchairAccessible)
        .withQuotedField(it.hasWideDoor)
        .withQuotedField(it.hasStepFreeAccess)
        .withQuotedField(it.hasFixedMobilityAids)
        .withQuotedField(it.hasTurningSpace)
        .withQuotedField(it.hasCallForAssistance)
        .withQuotedField(it.isWheelchairDesignated)
        .withQuotedField(it.isStepFreeDesignated)
        .withQuotedField(it.notes!!)
        .newRow()
    }

    return builder.build()
  }
}

class ApprovedPremisesRoomsSeedCsvRowFactory : Factory<ApprovedPremisesRoomsSeedCsvRow> {
  private var apCode: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bedCode: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var roomNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bedCount: Yielded<String> = { randomInt(1, 2).toString() }
  private var isSingle: Yielded<String> = { "no" }
  private var isGroundFloor: Yielded<String> = { "no" }
  private var isFullyFm: Yielded<String> = { "no" }
  private var hasCrib7Bedding: Yielded<String> = { "no" }
  private var hasSmokeDetector: Yielded<String> = { "no" }
  private var isTopFloorVulnerable: Yielded<String> = { "no" }
  private var isGroundFloorNrOffice: Yielded<String> = { "no" }
  private var hasNearbySprinkler: Yielded<String> = { "no" }
  private var isArsonSuitable: Yielded<String> = { "no" }
  private var isArsonDesignated: Yielded<String> = { "no" }
  private var hasArsonInsuranceConditions: Yielded<String> = { "no" }
  private var isSuitedForSexOffenders: Yielded<String> = { "no" }
  private var hasEnSuite: Yielded<String> = { "no" }
  private var isWheelchairAccessible: Yielded<String> = { "no" }
  private var hasWideDoor: Yielded<String> = { "no" }
  private var hasStepFreeAccess: Yielded<String> = { "no" }
  private var hasFixedMobilityAids: Yielded<String> = { "no" }
  private var hasTurningSpace: Yielded<String> = { "no" }
  private var hasCallForAssistance: Yielded<String> = { "no" }
  private var isWheelchairDesignated: Yielded<String> = { "no" }
  private var isStepFreeDesignated: Yielded<String> = { "no" }
  private var notes: Yielded<String?> = { "One bed in room - too small for double" }

  fun withApCode(apCode: String) = apply {
    this.apCode = { apCode }
  }

  fun withBedCode(bedCode: String) = apply {
    this.bedCode = { bedCode }
  }

  fun withRoomNumber(roomNumber: String) = apply {
    this.roomNumber = { roomNumber }
  }

  fun withBedCount(bedCount: String) = apply {
    this.bedCount = { bedCount }
  }

  fun withIsArsonSuitable(boolString: String) = apply {
    this.isArsonSuitable = { boolString }
  }

  fun withIsGroundFloor(boolString: String) = apply {
    this.isGroundFloor = { boolString }
  }

  fun withNotes(string: String) = apply {
    this.notes = { string }
  }

  override fun produce() = ApprovedPremisesRoomsSeedCsvRow(
    apCode = this.apCode(),
    bedCode = this.bedCode(),
    roomNumber = this.roomNumber(),
    bedCount = this.bedCount(),
    isSingle = this.isSingle(),
    isGroundFloor = this.isGroundFloor(),
    isFullyFm = this.isFullyFm(),
    hasCrib7Bedding = this.hasCrib7Bedding(),
    hasSmokeDetector = this.hasSmokeDetector(),
    isTopFloorVulnerable = this.isTopFloorVulnerable(),
    isGroundFloorNrOffice = this.isGroundFloorNrOffice(),
    hasNearbySprinkler = this.hasNearbySprinkler(),
    isArsonSuitable = this.isArsonSuitable(),
    isArsonDesignated = this.isArsonDesignated(),
    hasArsonInsuranceConditions = this.hasArsonInsuranceConditions(),
    isSuitedForSexOffenders = this.isSuitedForSexOffenders(),
    hasEnSuite = this.hasEnSuite(),
    isWheelchairAccessible = this.isWheelchairAccessible(),
    hasWideDoor = this.hasWideDoor(),
    hasStepFreeAccess = this.hasStepFreeAccess(),
    hasFixedMobilityAids = this.hasFixedMobilityAids(),
    hasTurningSpace = this.hasTurningSpace(),
    hasCallForAssistance = this.hasCallForAssistance(),
    isWheelchairDesignated = this.isWheelchairDesignated(),
    isStepFreeDesignated = this.isStepFreeDesignated(),
    notes = this.notes(),
  )
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.QuestionCriteriaMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.SiteSurveyImportException
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1RoomsFromSiteSurveyXlsxTest : SeedTestBase() {

  private lateinit var questionCriteriaMapping: QuestionCriteriaMapping

  @BeforeEach
  fun setup() {
    questionCriteriaMapping = QuestionCriteriaMapping(characteristicRepository)
  }

  fun MutableList<String>.addCharacteristics(numberOfRooms: Int = 1, activeCharacteristics: Map<String, List<Int>> = emptyMap()) {
    questionCriteriaMapping.questionToCharacterEntityMapping.keys.forEach { question ->
      this.add(question)
      val answers = MutableList(numberOfRooms) { "No" }
      activeCharacteristics[question]?.forEach {
        answers[it] = "Yes"
      }
      this.addAll(answers)
    }
  }

  @Test
  fun `Creating a new room and a new bed with a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }
    val premisesId = premises.id

    val header = listOf("Unique Reference Number for Bed", "SWABI01NEW")
    val rows = mutableListOf(
      "Room Number / Name",
      "1",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
    )
    rows.addCharacteristics(1, mapOf("Is this room located on the ground floor?" to listOf(0)))

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedXlsxService.seedExcelData(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      premisesId,
      "example.xlsx",
    )

    val newRoom = roomRepository.findByCode("Q999 - 1")
    assertThat(newRoom!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "isGroundFloor"
    }

    val newBed = bedRepository.findByCodeAndRoomId("SWABI01NEW", newRoom.id)
    assertThat(newBed!!.name).isEqualTo("1")
    assertThat(
      newBed.room.id == newRoom.id &&
        newBed.room.code == "Q999 - 1",
    )
  }

  @Test
  fun `Creating three new rooms and three new beds with a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }
    val premisesId = premises.id

    val header = listOf("Unique Reference Number for Bed", "SWABI01NEW", "SWABI02NEW", "SWABI03NEW")
    val rows = mutableListOf(
      "Room Number / Name",
      "1",
      "2",
      "3",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
      "1",
      "1",
    )
    rows.addCharacteristics(3, mapOf("Is this room located on the ground floor?" to listOf(1)))

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedXlsxService.seedExcelData(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      premisesId,
      "example.xlsx",
    )

    val room1 = roomRepository.findByCode("Q999 - 1")
    assertThat(room1!!.characteristics).isEmpty()

    val bed1 = bedRepository.findByCodeAndRoomId("SWABI01NEW", room1.id)
    assertThat(bed1!!.name).isEqualTo("1")
    assertThat(
      bed1.room.id == room1.id &&
        bed1.room.code == "Q999 - 1",
    )

    val room2 = roomRepository.findByCode("Q999 - 2")
    assertThat(room2!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "isGroundFloor"
    }

    val bed2 = bedRepository.findByCodeAndRoomId("SWABI02NEW", room2.id)
    assertThat(bed2!!.name).isEqualTo("1")
    assertThat(
      bed2.room.id == room2.id &&
        bed2.room.code == "Q999 - 2",
    )

    val room3 = roomRepository.findByCode("Q999 - 3")
    assertThat(room3!!.characteristics).isEmpty()

    val bed3 = bedRepository.findByCodeAndRoomId("SWABI03NEW", room3.id)
    assertThat(bed3!!.name).isEqualTo("1")
    assertThat(
      bed3.room.id == room3.id &&
        bed3.room.code == "Q999 - 1",
    )
  }

  @Test
  fun `Creating two new rooms and three new beds with a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }
    val premisesId = premises.id

    val header = listOf("Unique Reference Number for Bed", "SWABI01NEW", "SWABI02NEW", "SWABI03NEW")
    val rows = mutableListOf(
      "Room Number / Name",
      "1",
      "2",
      "2",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
      "1",
      "2",
    )
    rows.addCharacteristics(3, mapOf("Is this room located on the ground floor?" to listOf(1, 2)))

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedXlsxService.seedExcelData(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      premisesId,
      "example.xlsx",
    )

    val room1 = roomRepository.findByCode("Q999 - 1")
    assertThat(room1!!.characteristics).isEmpty()

    val bed1 = bedRepository.findByCodeAndRoomId("SWABI01NEW", room1.id)
    assertThat(bed1!!.name).isEqualTo("1")
    assertThat(
      bed1.room.id == room1.id &&
        bed1.room.code == "Q999 - 1",
    )

    val room2 = roomRepository.findByCode("Q999 - 2")
    assertThat(room2!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "isGroundFloor"
    }

    val bed2 = bedRepository.findByCodeAndRoomId("SWABI02NEW", room2.id)
    assertThat(bed2!!.name).isEqualTo("1")
    assertThat(
      bed2.room.id == room2.id &&
        bed2.room.code == "Q999 - 2",
    )

    val bed3 = bedRepository.findByCodeAndRoomId("SWABI03NEW", room2.id)
    assertThat(bed3!!.name).isEqualTo("2")
    assertThat(
      bed3.room.id == room2.id &&
        bed3.room.code == "Q999 - 2",
    )
  }

  @Test
  fun `Creating a new room and a new bed without a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }
    val premisesId = premises.id

    val header = listOf("Unique Reference Number for Bed", "SWABI01NEW")
    val rows = mutableListOf(
      "Room Number / Name",
      "1",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
    )
    rows.addCharacteristics(1)

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedXlsxService.seedExcelData(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      premisesId,
      "example.xlsx",
    )

    val newRoom = roomRepository.findByCode("Q999 - 1")
    assertThat(newRoom!!.characteristics).isEmpty()

    val newBed = bedRepository.findByCodeAndRoomId("SWABI01NEW", newRoom.id)
    assertThat(newBed!!.name).isEqualTo("1")
    assertThat(
      newBed.room.id == newRoom.id &&
        newBed.room.code == "Q999 - 1",
    )
  }

  @Test
  fun `Updating an existing room with a new bed and a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }
    val premisesId = premises.id
    val roomCode = "$qCode - 1"
    roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCode(roomCode)
    }

    val header = listOf("Unique Reference Number for Bed", "SWABI01")
    val rows = mutableListOf(
      "Room Number / Name",
      "1",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
    )
    rows.addCharacteristics(1, mapOf("Is this room located on the ground floor?" to listOf(0)))

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedXlsxService.seedExcelData(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      premisesId,
      "example.xlsx",
    )

    val updatedRoom = roomRepository.findByCode(roomCode)
    assertThat(updatedRoom!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "isGroundFloor"
    }

    val newBed = bedRepository.findByCodeAndRoomId("SWABI01", updatedRoom.id)
    assertThat(newBed!!.name).isEqualTo("1")
    assertThat(
      newBed.room.id == updatedRoom.id &&
        newBed.room.code == "Q999 - 1",
    )
  }

  @Test
  fun `Updating an existing room with an existing bed and a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }
    val premisesId = premises.id
    val roomCode = "$qCode - 1"
    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCode(roomCode)
    }
    bedEntityFactory.produceAndPersist {
      withRoom(room)
      withCode("SWABI01")
      withName("1")
    }

    val header = listOf("Unique Reference Number for Bed", "SWABI01")
    val rows = mutableListOf(
      "Room Number / Name",
      "1",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
    )
    rows.addCharacteristics(1, mapOf("Is this room located on the ground floor?" to listOf(0)))

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedXlsxService.seedExcelData(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      premisesId,
      "example.xlsx",
    )

    val updatedRoom = roomRepository.findByCode(roomCode)
    assertThat(updatedRoom!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "isGroundFloor"
    }

    val existingBed = bedRepository.findByCode("SWABI01")
    assertThat(existingBed!!.name).isEqualTo("1")
    assertThat(
      existingBed.room.id == updatedRoom.id &&
        existingBed.room.code == "Q999 - 1",
    )
  }

  @Test
  fun `Creating a new room and a new bed using an existing bed code fails`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }
    val premisesId = premises.id
    val roomCode = "$qCode - 1"
    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCode(roomCode)
    }
    bedEntityFactory.produceAndPersist {
      withRoom(room)
      withCode("SWABI02")
      withName("1")
    }

    val header = listOf("Unique Reference Number for Bed", "SWABI02")
    val rows = mutableListOf(
      "Room Number / Name",
      "2",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
    )
    rows.addCharacteristics(1)

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedXlsxService.seedExcelData(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      premisesId,
      "example.xlsx",
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job" &&
          it.throwable != null &&
          it.throwable.message!!.contains("Detail: Key (code)=(SWABI02) already exists.")
      }
  }

  @Test
  fun `Invalid questions throws exception, fails to process xlsx, rolls back transaction and logs an error`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
    }
    val premisesId = premises.id

    val header = listOf("Unique Reference Number for Bed", "SWABI01NEW")
    val rows = mutableListOf(
      "Room Number / Name",
      "1",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
    )
    rows.addCharacteristics(1)
    rows.replaceAll { if (it == "Is this room located on the ground floor?") "Bad question" else it }

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedXlsxService.seedExcelData(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      premisesId,
      "example.xlsx",
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job" &&
          it.throwable != null &&
          it.throwable.message == "Unable to process XLSX file" &&
          it.throwable.cause is SiteSurveyImportException &&
          it.throwable.cause!!.message == "Characteristic question 'Is this room located on the ground floor?' not found on sheet Sheet3."
      }
  }

  @Test
  fun `Invalid answer throws exception, fails to process xlsx, rolls back transaction and logs an error`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
    }
    val premisesId = premises.id

    val header = listOf("Unique Reference Number for Bed", "SWABI01NEW")
    val rows = mutableListOf(
      "Room Number / Name",
      "1",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
    )
    rows.addCharacteristics(1, mapOf("Is this room located on the ground floor?" to listOf(0)))
    rows.replaceAll { if (it == "Yes") "Bad answer" else it }

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedXlsxService.seedExcelData(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      premisesId,
      "example.xlsx",
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job" &&
          it.throwable != null &&
          it.throwable.message == "Unable to process XLSX file" &&
          it.throwable.cause is SiteSurveyImportException &&
          it.throwable.cause!!.message == "Expecting 'yes' or 'no' for question 'Is this room located on the ground floor?' but is 'Bad answer' on sheet Sheet3 (row = 4, col = 1)."
      }
  }

  @Test
  fun `Creating a new room for a premise that doesn't exist throws error`() {
    val header = listOf("Unique Reference Number for Bed", "SWABI01NEW")
    val rows = listOf("", "")
    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedXlsxService.seedExcelData(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      UUID.fromString("97d6b3f1-3121-4afb-a4a6-e4a84b533c18"),
      "example.xlsx",
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job" &&
          it.throwable != null &&
          it.throwable.message == "Unable to process XLSX file" &&
          it.throwable.cause is SiteSurveyImportException &&
          it.throwable.cause!!.message == "No premises with id '97d6b3f1-3121-4afb-a4a6-e4a84b533c18' found."
      }
  }
}

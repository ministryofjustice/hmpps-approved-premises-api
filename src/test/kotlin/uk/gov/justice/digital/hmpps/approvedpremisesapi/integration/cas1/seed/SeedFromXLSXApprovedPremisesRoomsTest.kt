package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.SiteSurveyImportException
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedFromXLSXApprovedPremisesRoomsTest : SeedTestBase() {
  @Test
  fun `Creating a new room and a new bed with a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
    }
    val premisesId = premises.id

    val header = listOf("Unique Reference Number for Bed", "SWABI01NEW")
    val rows = listOf(
      "Room Number / Name",
      "1",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
      "Is this room located on the ground floor?",
      "Yes",
    )
    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedService.seedExcelData(
      SeedFromExcelFileType.approvedPremisesRoomFromExcel,
      premisesId,
      "example.xlsx",
    )

    val newRoom = roomRepository.findByCode("SWABI01NEW")
    assertThat(newRoom!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "IsGroundFloor"
    }

    val newBed = bedRepository.findByCode("1")
    assertThat(newBed!!.name).isEqualTo("SWABI01NEW - 1")
    assertThat(
      newBed.room.id == newRoom.id &&
        newBed.room.code == newRoom.code,
    )
  }

  @Test
  fun `Updating an existing room with a new bed and a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
    }
    val premisesId = premises.id
    val roomCode = "SWABI01"
    roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCode(roomCode)
    }

    val header = listOf("Unique Reference Number for Bed", roomCode)
    val rows = listOf(
      "Room Number / Name",
      "1",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
      "Is this room located on the ground floor?",
      "Yes",
    )
    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedService.seedExcelData(
      SeedFromExcelFileType.approvedPremisesRoomFromExcel,
      premisesId,
      "example.xlsx",
    )

    val updatedRoom = roomRepository.findByCode(roomCode)
    assertThat(updatedRoom!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "IsGroundFloor"
    }

    val newBed = bedRepository.findByCode("1")
    assertThat(newBed!!.name).isEqualTo("SWABI01 - 1")
    assertThat(
      newBed.room.id == updatedRoom.id &&
        newBed.room.code == updatedRoom.code,
    )
  }

  @Test
  fun `Updating an existing room with an existing bed and a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
    }
    val premisesId = premises.id
    val roomCode = "SWABI01"
    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCode(roomCode)
    }
    bedEntityFactory.produceAndPersist {
      withRoom(room)
      withCode("1")
      withName("SWABI01 - 1")
    }

    val header = listOf("Unique Reference Number for Bed", roomCode)
    val rows = listOf(
      "Room Number / Name",
      "1",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
      "Is this room located on the ground floor?",
      "Yes",
    )
    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedService.seedExcelData(
      SeedFromExcelFileType.approvedPremisesRoomFromExcel,
      premisesId,
      "example.xlsx",
    )

    val updatedRoom = roomRepository.findByCode(roomCode)
    assertThat(updatedRoom!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "IsGroundFloor"
    }

    val existingBed = bedRepository.findByCode("1")
    assertThat(existingBed!!.name).isEqualTo("SWABI01 - 1")
    assertThat(
      existingBed.room.id == updatedRoom.id &&
        existingBed.room.code == updatedRoom.code,
    )
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
    val rows = listOf(
      "Room Number / Name",
      "1",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
      "Bad question",
      "Yes",
    )
    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedService.seedExcelData(
      SeedFromExcelFileType.approvedPremisesRoomFromExcel,
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
    val rows = listOf(
      "Room Number / Name",
      "1",
      "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
      "1",
      "Is this room located on the ground floor?",
      "Bad answer",
    )
    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedService.seedExcelData(
      SeedFromExcelFileType.approvedPremisesRoomFromExcel,
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
          it.throwable.cause!!.message == "Expecting 'yes' or 'no' for question 'Is this room located on the ground floor?' but is 'Bad answer' on sheet Sheet3."
      }
  }

  @Test
  fun `Creating a new room for a premise that doesn't exist throws error`() {
    val header = listOf("Unique Reference Number for Bed", "SWABI01NEW")
    val rows = listOf("", "")
    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet3", dataFrame)

    seedService.seedExcelData(
      SeedFromExcelFileType.approvedPremisesRoomFromExcel,
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

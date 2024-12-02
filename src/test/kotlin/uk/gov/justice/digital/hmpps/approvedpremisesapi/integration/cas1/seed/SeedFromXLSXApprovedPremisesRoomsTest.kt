package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedFromXLSXApprovedPremisesRoomsTest : SeedTestBase() {
  @Test
  fun `Creating a new room with a characteristic succeeds`() {
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

    val updatedRoom = roomRepository.findByCode("SWABI01NEW")
    assertThat(updatedRoom!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "IsGroundFloor"
    }
  }

  @Test
  fun `Updating an existing room with a characteristic succeeds`() {
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
          it.throwable != null // &&
        // it.throwable.message == "Unable to process XLSX file"
      }
  }

//  @Test
//  fun `Creating a new room for a premise that doesn't exist throws error`() {
//  }
//
//  @Test
//  fun `Removing a characteristic that exists succeeds`() {
//  }
//
//  @Test
//  fun `Removing a characteristic that doesn't exist succeeds`() {
//  }
//
//  @Test
//  fun `Adding a characteristic that exists succeeds`() {
//  }
//  @Test
//  fun `room exists but bed doesn't - creates bed`() {
//
//  }
}

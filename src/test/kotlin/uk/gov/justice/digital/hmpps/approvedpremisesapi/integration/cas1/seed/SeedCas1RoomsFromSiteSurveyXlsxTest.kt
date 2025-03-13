package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DataFrameUtils.createNameValueDataFrame
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DataFrameUtils.dataFrameForHeadersAndRows

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1RoomsFromSiteSurveyXlsxTest : SeedTestBase() {

  val questions = listOf(
    "Is this bed in a single room?",
    "Is this room located on the ground floor?",
    "Is the room using only furnishings and bedding supplied by FM?",
    "Does this room have Crib7 rated bedding?",
    "Is there a smoke/heat detector in the room?",
    "Is this room on the top floor with at least one external wall and not located directly next to a fire exit or a protected stairway?",
    "Is the room close to the admin/staff office on the ground floor with at least one external wall and not located directly next to a fire exit or a protected stairway?",
    "is there a water mist extinguisher in close proximity to this room?",
    "Is this room suitable for people who pose an arson risk? (Must answer yes to Q; 4, 6 & 7, and 9 or  10)",
    "If IAP - Is there any insurance conditions that prevent a person with arson convictions being placed?",
    "Is this room suitable for people convicted of sexual offences?",
    "Does this room have en-suite bathroom facilities?",
    "Are corridors leading to this room of sufficient width to accommodate a wheelchair? (at least 1.2m wide)",
    "Is the door to this room at least 900mm wide?",
    "Is there step free access to this room and in corridors leading to this room?",
    "Are there fixed mobility aids in this room?",
    "Does this room have at least a 1500mmx1500mm turning space?",
    "Is there provision for people to call for assistance from this room?",
    "Can this room be designated as suitable for wheelchair users?   Must answer yes to Q23-26 on previous sheet and Q17-19 & 21 on this sheet)",
    "Can this room be designated as suitable for people requiring step free access? (Must answer yes to Q23 and 25 on previous sheet and Q19 on this sheet)",
  )

  data class Answers(
    val question: String,
    val answersForEachRoom: List<String>,
  )

  fun MutableList<List<Any>>.addQuestionsAndAnswers(
    vararg answerOverrides: List<String>,
  ): MutableList<List<Any>> {
    assert(this.size == 3) { "List must have the initial 3 rows added" }

    // the first column is label with a subsequent column per bed
    val numberOfBeds = (this[0].size - 1)

    questions.forEach { question ->
      val answers = if (answerOverrides.any { it[0] == question }) {
        val overrides = answerOverrides.first { it[0] == question }
        overrides.subList(1, overrides.size)
      } else {
        MutableList(numberOfBeds) { "No" }
      }
      this.add(listOf(question) + answers)
    }
    return this
  }

  @Test
  fun `Creating a new room and a new bed with a characteristic succeeds, ensuring no redundant decimal points`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI01NEW"),
      listOf("Room Number / Name", 1),
      listOf("Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)", 1),
    ).addQuestionsAndAnswers(listOf("Is this room located on the ground floor?", "Yes"))

    val roomsSheet = dataFrameForHeadersAndRows(values)

    withXlsx(
      xlsxName = "example",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    val newRoom = roomRepository.findByCode("Q999-1")
    assertThat(newRoom).isNotNull
    assertThat(newRoom!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "isGroundFloor"
    }

    val newBed = bedRepository.findByCodeAndRoomId("SWABI01NEW", newRoom.id)
    assertThat(newBed!!.name).isEqualTo("1 - 1")
    assertThat(newBed.room.id).isEqualTo(newRoom.id)
    assertThat(newBed.room.code).isEqualTo("Q999-1")
  }

  @Test
  fun `Creating a new room and a new bed with a characteristic succeeds, retaining non-redundant decimal points`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI01NEW"),
      listOf("Room Number / Name", 1.1),
      listOf("Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)", 1.2),
    ).addQuestionsAndAnswers(listOf("Is this room located on the ground floor?", "Yes"))

    val roomsSheet = dataFrameForHeadersAndRows(values)

    withXlsx(
      xlsxName = "example",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    val newRoom = roomRepository.findByCode("Q999-1.1")
    assertThat(newRoom).isNotNull
    assertThat(newRoom!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "isGroundFloor"
    }

    val newBed = bedRepository.findByCodeAndRoomId("SWABI01NEW", newRoom.id)
    assertThat(newBed!!.name).isEqualTo("1.1 - 1.2")
    assertThat(newBed.room.id).isEqualTo(newRoom.id)
    assertThat(newBed.room.code).isEqualTo("Q999-1.1")
  }

  @Test
  fun `Creating three new rooms and three new beds with a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI01NEW", "SWABI02NEW", "SWABI03NEW"),
      listOf(
        "Room Number / Name",
        1.0,
        2.0,
        3.0,
      ),
      listOf(
        "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
        1.0,
        1.0,
        1.0,
      ),
    ).addQuestionsAndAnswers(listOf("Is this room located on the ground floor?", "No", "Yes", "No"))

    val roomsSheet = dataFrameForHeadersAndRows(values)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    val room1 = roomRepository.findByCode("Q999-1")
    assertThat(room1!!.characteristics).isEmpty()

    val bed1 = bedRepository.findByCodeAndRoomId("SWABI01NEW", room1.id)
    assertThat(bed1!!.name).isEqualTo("1 - 1")
    assertThat(
      bed1.room.id == room1.id &&
        bed1.room.code == "Q999-1",
    )

    val room2 = roomRepository.findByCode("Q999-2")
    assertThat(room2!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "isGroundFloor"
    }

    val bed2 = bedRepository.findByCodeAndRoomId("SWABI02NEW", room2.id)
    assertThat(bed2!!.name).isEqualTo("2 - 1")
    assertThat(
      bed2.room.id == room2.id &&
        bed2.room.code == "Q999-2",
    )

    val room3 = roomRepository.findByCode("Q999-3")
    assertThat(room3!!.characteristics).isEmpty()

    val bed3 = bedRepository.findByCodeAndRoomId("SWABI03NEW", room3.id)
    assertThat(bed3!!.name).isEqualTo("3 - 1")
    assertThat(
      bed3.room.id == room3.id &&
        bed3.room.code == "Q999-1",
    )
  }

  @Test
  fun `Creating one new room with two beds but different characteristics fails`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI01NEW", "SWABI02NEW"),
      listOf(
        "Room Number / Name",
        1.0,
        1.0,
      ),
      listOf(
        "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
        1.0,
        2.0,
      ),
    ).addQuestionsAndAnswers(listOf("Is this room located on the ground floor?", "No", "Yes"))

    val roomsSheet = dataFrameForHeadersAndRows(values)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job for 'example.xlsx' with message '1 or more beds in room 'Q999-1' have different characteristics.'"
      }
  }

  @Test
  fun `Creating two new rooms and three new beds with a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI01NEW", "SWABI02NEW", "SWABI03NEW"),
      listOf(
        "Room Number / Name",
        "1",
        "2",
        "2",
      ),
      listOf(
        "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
        "1",
        "1",
        "2",
      ),
    ).addQuestionsAndAnswers(listOf("Is this room located on the ground floor?", "No", "Yes", "Yes"))

    val roomsSheet = dataFrameForHeadersAndRows(values)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    val room1 = roomRepository.findByCode("Q999-1")
    assertThat(room1!!.characteristics).isEmpty()

    val bed1 = bedRepository.findByCodeAndRoomId("SWABI01NEW", room1.id)
    assertThat(bed1!!.name).isEqualTo("1 - 1")
    assertThat(
      bed1.room.id == room1.id &&
        bed1.room.code == "Q999-1",
    )

    val room2 = roomRepository.findByCode("Q999-2")
    assertThat(room2!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "isGroundFloor"
    }

    val bed2 = bedRepository.findByCodeAndRoomId("SWABI02NEW", room2.id)
    assertThat(bed2!!.name).isEqualTo("2 - 1")
    assertThat(
      bed2.room.id == room2.id &&
        bed2.room.code == "Q999-2",
    )

    val bed3 = bedRepository.findByCodeAndRoomId("SWABI03NEW", room2.id)
    assertThat(bed3!!.name).isEqualTo("2 - 2")
    assertThat(
      bed3.room.id == room2.id &&
        bed3.room.code == "Q999-2",
    )
  }

  @Test
  fun `Creating a new room and a new bed without a characteristic succeeds`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI01NEW"),
      listOf(
        "Room Number / Name",
        "1",
      ),
      listOf(
        "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
        "1",
      ),
    ).addQuestionsAndAnswers()

    val roomsSheet = dataFrameForHeadersAndRows(values)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    val newRoom = roomRepository.findByCode("Q999-1")
    assertThat(newRoom!!.characteristics).isEmpty()

    val newBed = bedRepository.findByCodeAndRoomId("SWABI01NEW", newRoom.id)
    assertThat(newBed!!.name).isEqualTo("1 - 1")
    assertThat(
      newBed.room.id == newRoom.id &&
        newBed.room.code == "Q999-1",
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
    val roomCode = "$qCode-1"
    roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCode(roomCode)
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI01"),
      listOf(
        "Room Number / Name",
        "1",
      ),
      listOf(
        "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
        "1",
      ),
    ).addQuestionsAndAnswers(listOf("Is this room located on the ground floor?", "Yes", "No", "No"))

    val roomsSheet = dataFrameForHeadersAndRows(values)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    val updatedRoom = roomRepository.findByCode(roomCode)
    assertThat(updatedRoom!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "isGroundFloor"
    }

    val newBed = bedRepository.findByCodeAndRoomId("SWABI01", updatedRoom.id)
    assertThat(newBed!!.name).isEqualTo("1 - 1")
    assertThat(
      newBed.room.id == updatedRoom.id &&
        newBed.room.code == "Q999-1",
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
    val roomCode = "$qCode-1"
    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCode(roomCode)
    }
    bedEntityFactory.produceAndPersist {
      withRoom(room)
      withCode("SWABI01")
      withName("1 - 1")
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI01"),
      listOf(
        "Room Number / Name",
        "1",
      ),
      listOf(
        "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
        "1",
      ),
    ).addQuestionsAndAnswers(listOf("Is this room located on the ground floor?", "Yes", "No", "No"))

    val roomsSheet = dataFrameForHeadersAndRows(values)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    val updatedRoom = roomRepository.findByCode(roomCode)
    assertThat(updatedRoom!!.characteristics).anyMatch {
      it.name == "Is this room located on the ground floor?" &&
        it.propertyName == "isGroundFloor"
    }

    val existingBed = bedRepository.findByCode("SWABI01")
    assertThat(existingBed!!.name).isEqualTo("1 - 1")
    assertThat(
      existingBed.room.id == updatedRoom.id &&
        existingBed.room.code == "Q999-1",
    )
  }

  @Test
  fun `Creating a new room with two beds with the same name fails`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }
    val roomCode = "$qCode-2"
    roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCode(roomCode)
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI02a", "SWABI02b"),
      listOf(
        "Room Number / Name",
        "2",
        "2",
      ),
      listOf(
        "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
        "1",
        "1",
      ),
    ).addQuestionsAndAnswers()

    val roomsSheet = dataFrameForHeadersAndRows(values)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job for 'example.xlsx' with message 'Bed name '2 - 1' is not unique.'"
      }
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
    val roomCode = "$qCode-1"
    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCode(roomCode)
    }
    bedEntityFactory.produceAndPersist {
      withRoom(room)
      withCode("SWABI02")
      withName("1")
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI02"),
      listOf(
        "Room Number / Name",
        "2",
      ),
      listOf(
        "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
        "1",
      ),
    ).addQuestionsAndAnswers()

    val roomsSheet = dataFrameForHeadersAndRows(values)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job for 'example.xlsx' with message 'Bed SWABI02 already exists in room Q999-1 but is being added to room Q999-2.'"
      }
  }

  @Test
  fun `Creating a new room and new beds using existing bed codes fails`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode(qCode)
    }
    val roomCode = "$qCode-1"
    val room1 = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCode(roomCode)
    }
    bedEntityFactory.produceAndPersist {
      withRoom(room1)
      withCode("SWABI01")
      withName("1")
    }
    bedEntityFactory.produceAndPersist {
      withRoom(room1)
      withCode("SWABI02")
      withName("2")
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI01", "SWABI02"),
      listOf(
        "Room Number / Name",
        "2",
        "2",
      ),
      listOf(
        "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
        "1",
        "2",
      ),
    ).addQuestionsAndAnswers()

    val roomsSheet = dataFrameForHeadersAndRows(values)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job for 'example.xlsx' with message 'Bed SWABI01 already exists in room Q999-1 but is being added to room Q999-2.," +
          "Bed SWABI02 already exists in room Q999-1 but is being added to room Q999-2.'"
      }
  }

  @Test
  fun `Invalid questions throws exception, fails to process xlsx, rolls back transaction and logs an error`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode("Q999")
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI01NEW"),
      listOf(
        "Room Number / Name",
        "1",
      ),
      listOf(
        "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
        "1",
      ),
    ).addQuestionsAndAnswers()

    values.replaceAll {
      if (it[0] == "Is this room located on the ground floor?") {
        listOf("Bad question") + it.subList(1, it.size)
      } else {
        it
      }
    }

    val roomsSheet = dataFrameForHeadersAndRows(values)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job for 'example.xlsx' with message 'Couldn't find a single answer for question 'Exact(label=Is this room located on the ground floor?)' on sheet Sheet3'"
      }
  }

  @Test
  fun `Invalid answer throws exception, fails to process xlsx, rolls back transaction and logs an error`() {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationRegion = probationRegionEntityFactory.produceAndPersist()
    val qCode = "Q999"
    approvedPremisesEntityFactory.produceAndPersist {
      withLocalAuthorityArea(localAuthorityArea)
      withProbationRegion(probationRegion)
      withQCode("Q999")
    }

    val values = mutableListOf<List<Any>>(
      listOf("Unique Reference Number for Bed", "SWABI01NEW"),
      listOf(
        "Room Number / Name",
        "2",
      ),
      listOf(
        "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
        "1",
      ),
    ).addQuestionsAndAnswers(listOf("Is this room located on the ground floor?", "Bad answer", "No", "No"))

    val roomsSheet = dataFrameForHeadersAndRows(values)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", qCode),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job for 'example.xlsx' with message 'Invalid value for Yes/No dropdown:" +
          " BAD ANSWER on sheet Sheet3. Question is Exact(label=Is this room located on the ground floor?)'"
      }
  }

  @Test
  fun `Creating a new room for a premise that doesn't exist throws error`() {
    val header = listOf("Unique Reference Number for Bed", "SWABI01NEW")
    val rows = listOf("", "")
    val roomsSheet = dataFrameOf(header, rows)

    createXlsxForSeeding(
      fileName = "example.xlsx",
      sheets = mapOf(
        "Sheet2" to createNameValueDataFrame("AP Identifier (Q No.)", "INVALIDQCODE"),
        "Sheet3" to roomsSheet,
      ),
    )

    seedXlsxService.seedFile(
      SeedFromExcelFileType.CAS1_IMPORT_SITE_SURVEY_ROOMS,
      "example.xlsx",
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Unable to complete Excel seed job for 'example.xlsx' with message 'No premises with qcode 'INVALIDQCODE' found.'" &&
          it.throwable != null &&
          it.throwable.message == "No premises with qcode 'INVALIDQCODE' found."
      }
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.diff.ListCompareAlgorithm
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ExcelSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1SiteSurveyBedFactory.Cas1SiteSurveyBed
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.Boolean

/**
 * This job seeds rooms and beds from a site survey xlsx Excel file.
 * The xlsx file should have two sheets:
 *  Sheet2: Premises details
 *  Sheet3: Room and bed characteristic details
 *
 * The job can be run by calling the run_seed_from_excel_job script with the following arguments:
 *  ./script/run_seed_from_excel_job cas1_import_site_survey_rooms 'site_survey_file.xlsx'
 */
@Component
@Suppress("LongParameterList")
class Cas1SeedRoomsFromSiteSurveyXlsxJob(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val roomRepository: RoomRepository,
  private val bedRepository: BedRepository,
  private val characteristicRepository: CharacteristicRepository,
) : ExcelSeedJob {

  companion object {
    val javers: Javers = JaversBuilder.javers().withListCompareAlgorithm(ListCompareAlgorithm.AS_SET).build()
    fun buildRoomCode(qCode: String, roomNumber: String) = "$qCode-$roomNumber"
  }

  private val log = LoggerFactory.getLogger(this::class.java)
  private val changesLog = LoggerFactory.getLogger("${this::class.java}.changes")

  override fun processXlsx(file: File) {
    val qCode = Cas1SiteSurveyPremiseFactory().getQCode(file)
    val premises = findExistingPremisesByQCodeOrThrow(qCode)
    log.info("Seeding Rooms for premise '${premises.name}' with QCode '$qCode'")

    val siteSurveyBedsInfo = Cas1SiteSurveyBedFactory().load(file)

    val rooms = resolveRooms(qCode, siteSurveyBedsInfo)
    checkRoomCharacteristics(rooms)

    val beds = resolveBeds(qCode, siteSurveyBedsInfo)
    checkBedNamesUnique(beds)

    rooms.forEach {
      val existingRoom = roomRepository.findByCode(it.roomCode)
      if (existingRoom == null) {
        createRoom(premises, it)
      } else {
        updateRoom(existingRoom, it)
      }
    }

    val bedErrors = mutableListOf<String>()
    beds.forEach {
      val existingBed = bedRepository.findByCode(it.bedCode)
      if (existingBed == null) {
        createBed(it)
      } else {
        if (existingBed.room.code != it.roomCode) {
          bedErrors.add("Bed ${it.bedCode} already exists in room ${existingBed.room.code} but is being added to room ${it.roomCode}.")
        } else if (existingBed.endDate != null) {
          updateBed(existingBed, newEndDate = null)
        }
      }
    }

    expireRemovedBeds(beds, premises)

    if (bedErrors.isNotEmpty()) {
      error(bedErrors.joinToString(","))
    }
  }

  private data class RoomInfo(
    val roomCode: String,
    val roomName: String,
    val characteristics: List<CharacteristicEntity>,
  )

  private fun resolveRooms(qCode: String, siteSurveyBeds: List<Cas1SiteSurveyBed>): List<RoomInfo> = siteSurveyBeds.map {
    RoomInfo(
      roomCode = buildRoomCode(qCode, it.roomNumber),
      roomName = it.roomNumber,
      characteristics = resolveCharacteristics(it),
    )
  }

  private fun checkRoomCharacteristics(rooms: List<RoomInfo>) {
    rooms.groupBy { room -> room.roomCode }.forEach { (_, rooms) ->
      rooms.all { it.characteristics == rooms.first().characteristics } || error("1 or more beds in room '${rooms.first().roomName}' have different characteristics.")
    }
  }

  private fun checkBedNamesUnique(beds: List<BedInfo>) {
    beds.groupBy { bed -> bed.bedName }.forEach { (bedName, beds) ->
      beds.size == 1 || error("Bed name '$bedName' is not unique.")
    }
  }

  private data class CharacteristicRequired(
    val propertyName: String,
    val value: Boolean,
  )

  @Suppress("TooGenericExceptionThrown")
  private fun resolveCharacteristics(bed: Cas1SiteSurveyBed): List<CharacteristicEntity> = listOf(
    CharacteristicRequired("isSingle", bed.isSingle),
    CharacteristicRequired("isGroundFloor", bed.isGroundFloor),
    CharacteristicRequired("isFullyFm", bed.isFullyFm),
    CharacteristicRequired("hasCrib7Bedding", bed.hasCrib7Bedding),
    CharacteristicRequired("hasSmokeDetector", bed.hasSmokeDetector),
    CharacteristicRequired("isTopFloorVulnerable", bed.isTopFloorVulnerable),
    CharacteristicRequired("isGroundFloorNrOffice", bed.isGroundFloorNrOffice),
    CharacteristicRequired("hasNearbySprinkler", bed.hasNearbySprinkler),
    CharacteristicRequired("isArsonSuitable", bed.isArsonSuitable),
    CharacteristicRequired("hasArsonInsuranceConditions", bed.hasArsonInsuranceConditions),
    CharacteristicRequired("isSuitedForSexOffenders", bed.isSuitedForSexOffenders),
    CharacteristicRequired("hasEnSuite", bed.hasEnSuite),
    CharacteristicRequired("isWheelchairAccessible", bed.isWheelchairAccessible),
    CharacteristicRequired("hasWideDoor", bed.hasWideDoor),
    CharacteristicRequired("hasStepFreeAccess", bed.hasStepFreeAccess),
    CharacteristicRequired("hasFixedMobilityAids", bed.hasFixedMobilityAids),
    CharacteristicRequired("hasTurningSpace", bed.hasTurningSpace),
    CharacteristicRequired("hasCallForAssistance", bed.hasCallForAssistance),
    CharacteristicRequired("isWheelchairDesignated", bed.isWheelchairDesignated),
    CharacteristicRequired("isStepFreeDesignated", bed.isStepFreeDesignated),
  ).filter { it.value }
    .map {
      characteristicRepository.findByPropertyNameAndScopes(propertyName = it.propertyName, serviceName = "approved-premises", modelName = "room")
        ?: throw RuntimeException("Characteristic '${it.propertyName}' does not exist for AP room")
    }

  private data class BedInfo(
    val bedName: String,
    val bedCode: String,
    val roomCode: String,
  )

  private fun resolveBeds(qCode: String, siteSurveyBeds: List<Cas1SiteSurveyBed>): List<BedInfo> = siteSurveyBeds.map {
    BedInfo(
      bedName = "${it.roomNumber} - ${it.bedNumber}",
      bedCode = it.uniqueBedRef,
      roomCode = buildRoomCode(qCode, it.roomNumber),
    )
  }

  private fun createRoom(premises: ApprovedPremisesEntity, room: RoomInfo) {
    roomRepository.save(
      RoomEntity(
        id = UUID.randomUUID(),
        name = room.roomName,
        code = room.roomCode,
        premises = premises,
        characteristics = room.characteristics.toMutableList(),
        notes = null,
        beds = mutableListOf(),
      ),
    )
    changesLog.info("Created new room with code ${room.roomCode} and name ${room.roomName} in premise ${premises.name}.")
  }

  private data class ApprovedPremisesRoomForComparison(
    val id: UUID,
    val name: String,
    val code: String?,
    val characteristicNames: List<String>,
  ) {
    companion object {
      fun fromEntity(entity: RoomEntity): ApprovedPremisesRoomForComparison = ApprovedPremisesRoomForComparison(
        id = entity.id,
        name = entity.name,
        code = entity.code,
        characteristicNames = entity.characteristics.map { it.name }.sorted(),
      )
    }
  }

  private data class ApprovedPremisesBedForComparison(
    val endDate: LocalDate?,
  ) {
    companion object {
      fun fromEntity(entity: BedEntity): ApprovedPremisesBedForComparison = ApprovedPremisesBedForComparison(
        endDate = entity.endDate,
      )
    }
  }

  private fun updateRoom(existingRoom: RoomEntity, newRoom: RoomInfo) {
    val beforeChange = ApprovedPremisesRoomForComparison.fromEntity(existingRoom)

    existingRoom.characteristics.clear()
    existingRoom.characteristics.addAll(newRoom.characteristics)
    roomRepository.save(existingRoom)

    val afterChange = ApprovedPremisesRoomForComparison.fromEntity(existingRoom)

    val diff = javers.compare(beforeChange, afterChange)
    if (diff.hasChanges()) {
      changesLog.info("Changes for existing room ${existingRoom.name} with code ${existingRoom.code}: ${diff.prettyPrint()}")
    } else {
      log.info("No changes for existing room ${existingRoom.name} with code ${existingRoom.code}.")
    }
  }

  private fun createBed(bed: BedInfo) {
    val room = roomRepository.findByCode(bed.roomCode)
      ?: error("Room with code '${bed.roomCode}' not found.")
    bedRepository.save(
      BedEntity(
        id = UUID.randomUUID(),
        name = bed.bedName,
        code = bed.bedCode,
        room = room,
        startDate = null,
        endDate = null,
        createdAt = null,
      ),
    )
    changesLog.info("Created new bed with code ${bed.bedCode} and name ${bed.bedName} in room code ${bed.roomCode}.")
  }

  private fun expireRemovedBeds(beds: List<BedInfo>, premises: ApprovedPremisesEntity) {
    val existingUnexpiredBeds = bedRepository.findByRoomPremisesIdAndEndDateIsNull(premises.id)
    val existingBedCodes = existingUnexpiredBeds.map { it.code }.toSet()
    val newBedCodes = beds.map { it.bedCode }.toSet()
    val expiredBedCodes = existingBedCodes.minus(newBedCodes)

    expiredBedCodes.forEach { bedCode ->
      updateBed(existingUnexpiredBeds.first { it.code == bedCode }, LocalDate.now())
    }
  }

  private fun updateBed(bed: BedEntity, newEndDate: LocalDate?) {
    val beforeChange = ApprovedPremisesBedForComparison.fromEntity(bed)

    val updatedBed = bedRepository.save(bed.apply { this.endDate = newEndDate })

    val afterChange = ApprovedPremisesBedForComparison.fromEntity(updatedBed)

    val diff = javers.compare(beforeChange, afterChange)
    if (diff.hasChanges()) {
      changesLog.info("Changes for bed ${bed.name} with code ${bed.code}: ${diff.prettyPrint()}")
    }
  }

  private fun findExistingPremisesByQCodeOrThrow(qCode: String): ApprovedPremisesEntity = approvedPremisesRepository.findByQCode(qCode)
    ?: error("No premises with qcode '$qCode' found.")
}

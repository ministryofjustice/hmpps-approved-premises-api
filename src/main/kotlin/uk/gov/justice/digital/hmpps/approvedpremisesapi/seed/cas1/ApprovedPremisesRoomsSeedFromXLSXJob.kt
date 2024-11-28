package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.name
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ExcelSeedJob
import java.util.UUID

class InvalidQuestionException(message: String) : Exception(message)

@Suppress("LongParameterList")
class ApprovedPremisesRoomsSeedFromXLSXJob(
  fileName: String,
  premisesId: UUID,
  sheetName: String,
  private val premisesRepository: PremisesRepository,
  private val roomRepository: RoomRepository,
  private val bedRepository: BedRepository,
  private val characteristicRepository: CharacteristicRepository,
  private val siteSurvey: SiteSurvey,
) : ExcelSeedJob(
  fileName = fileName,
  premisesId = premisesId,
  sheetName = sheetName,
) {
  private val log = LoggerFactory.getLogger(this::class.java)
  override fun processDataFrame(dataFrame: DataFrame<*>) {
    findExistingPremisesOrThrow(premisesId)

    val questions = dataFrame.getColumn(0).values().toList()
    if (questions.drop(2) != siteSurvey.questionToCharacterEntityMapping.keys.toList()) throw InvalidQuestionException("Site survey file $fileName contains wrong questions on sheet $sheetName")

    for (i in 1..<dataFrame.columnsCount()) {
      val roomCharacteristics = dataFrame.getColumn(i)
      val roomCode = roomCharacteristics.name
      val roomName = roomCharacteristics[0].toString()

      val room = roomRepository.findByCode(roomCode) ?: createRoom(
        roomCode,
        roomName,
      )

      val bedCode = roomCharacteristics[1].toString()
      val bedName = "$roomName - $bedCode"

      val bed = room?.beds?.find { it.code == bedCode } ?: createBed(
        bedName,
        bedCode,
        room!!,
      )

      val characteristics = mutableListOf<CharacteristicEntity>()

      questions.drop(2).forEachIndexed { i, question ->
        val characteristic = siteSurvey.questionToCharacterEntityMapping.getValue(question.toString())!!
        if (roomCharacteristics[i + 2].toString().equals("yes", true)) {
          characteristics.add(characteristic)
          log.info("Added characteristic ${characteristic.propertyName} to room code $roomCode.")
        } else {
          if (characteristics.remove(characteristic)) {
            log.info("Removed characteristic ${characteristic.propertyName} from room code $roomCode.")
          }
        }
      }
      room!!.characteristics.addAll(characteristics)
      roomRepository.save(room)
    }
  }

  private fun createRoom(roomCode: String, roomName: String): RoomEntity? {
    val room = roomRepository.save(
      RoomEntity(
        id = UUID.randomUUID(),
        name = roomName,
        code = roomCode,
        notes = null,
        premises = premisesRepository.findByIdOrNull(premisesId)!!,
        beds = mutableListOf(),
        characteristics = mutableListOf(),
      ),
    )
    log.info("Created new room ${room.name} with code ${room.code} and name ${room.name}.")
    return room
  }

  fun createBed(bedName: String, bedCode: String, room: RoomEntity): BedEntity {
    val bed = bedRepository.save(
      BedEntity(
        id = UUID.randomUUID(),
        name = bedName,
        code = bedCode,
        room = room,
        endDate = null,
        createdAt = null,
      ),
    )
    log.info("Created new bed ${bed.id} with code ${bed.code} and name ${bed.name}.")
    return bed
  }

  @Suppress("TooGenericExceptionThrown")
  private fun findExistingPremisesOrThrow(premisesId: UUID): PremisesEntity {
    return premisesRepository.findByIdOrNull(premisesId) ?: throw RuntimeException(
      "Error: no premises with id '$premisesId' found. ",
    )
  }
}

@Component
class SiteSurvey constructor(characteristicRepository: CharacteristicRepository) {
  private val questionToPropertyNameMapping = mapOf(
//    "Is this bed in a single room?" to "isSingle",
//    "Is this an IAP?" to "isIAP",
//    "Is this AP a PIPE?" to "isPIPE",
//    "Is this AP an Enhanced Security AP?" to "isESAP",
//    "Is this AP semi specialist Mental Health?" to "isSemiSpecialistMentalHealth",
//    "Is this a Recovery Focussed AP?" to "isRecoveryFocussed",
//    "Is this AP suitable for people at risk of criminal exploitation? N.B Enhanced Security sites answer No, other APs answer Yes." to "isSuitableForVulnerable",
//    "Does this AP accept people who have committed sexual offences against adults?" to "acceptsSexOffenders",
//    "Does this AP accept people who have committed sexual offences against children?" to "acceptsChildSexOffenders",
//    "Does this AP accept people who have committed non-sexual offences against children?" to "acceptsNonSexualChildOffenders",
//    "Does this AP accept people who have been convicted of hate crimes?" to "acceptsHateCrimeOffenders",
//    "Is this AP Catered (no for Self Catered)?" to "isCatered",
//    "Is there a step free entrance to the AP at least 900mm wide?" to "hasWideStepFreeAccess",
//    "Are corridors leading to communal areas at least 1.2m wide?" to "hasWideAccessToCommunalAreas",
//    "Do corridors leading to communal areas have step free access?" to "hasStepFreeAccessToCommunalAreas",
//    "Does this AP have bathroom facilities that have been adapted for wheelchair users?" to "hasWheelChairAccessibleBathrooms",
//    "Is there a lift at this AP?" to "hasLift",
//    "Does this AP have tactile & directional flooring?" to "hasTactileFlooring",
//    "Does this AP have signs in braille?" to "hasBrailleSignage",
//    "Does this AP have a hearing loop?" to "hasHearingLoop",
//    "Are there any additional restrictions on people that this AP can accommodate?" to "additionalRestrictions",
//    "Is the room using only furnishings and bedding supplied by FM?" to "isFullyFm",
//    "Does this room have Crib7 rated bedding?" to "hasCrib7Bedding",
//    "Is there a smoke/heat detector in the room?" to "hasSmokeDetector",
//    "Is this room on the top floor with at least one external wall and not located directly next to a fire exit or a protected stairway?" to "isTopFloorVulnerable",
//    "Is the room close to the admin/staff office on the ground floor with at least one external wall and not located directly next to a fire exit or a protected stairway?"
//    to "isGroundFloorNrOffice",
//    "is there a water mist extinguisher in close proximity to this room?" to "hasNearbySprinkler",
//    "Is this room suitable for people who pose an arson risk? (Must answer yes to Q; 6 & 7, and 9 or  10)" to "isArsonSuitable",
//    "Is this room currently a designated arson room?" to "isArsonDesignated",
//    "If IAP - Is there any insurance conditions that prevent a person with arson convictions being placed?" to "hasArsonInsuranceConditions",
//    "Is this room suitable for people convicted of sexual offences?" to "isSuitedForSexOffenders",
//    "Does this room have en-suite bathroom facilities?" to "hasEnSuite",
//    "Are corridors leading to this room of sufficient width to accommodate a wheelchair? (at least 1.2m wide)" to "isWheelchairAccessible",
//    "Is the door to this room at least 900mm wide?" to "hasWideDoor",
//    "Is there step free access to this room and in corridors leading to this room?" to "hasStepFreeAccess",
//    "Are there fixed mobility aids in this room?" to "hasFixedMobilityAids",
//    "Does this room have at least a 1500mmx1500mm turning space?" to "hasTurningSpace",
//    "Is there provision for people to call for assistance from this room?" to "hasCallForAssistance",
//    "Can this room be designated as suitable for wheelchair users?   Must answer yes to Q23-26 on previous sheet and Q17-21 on this sheet)" to "isWheelchairDesignated",
//    "Can this room be designated as suitable for people requiring step free access? (Must answer yes to Q23 and 25 on previous sheet and Q19 on this sheet)" to "isStepFreeDesignated",
    "Is this room located on the ground floor?" to "IsGroundFloor",
  )

  val questionToCharacterEntityMapping = questionToPropertyNameMapping.map { (key, value) ->
    Pair(key, characteristicRepository.findByPropertyName(value, ServiceName.approvedPremises.value))
  }.toMap()
}

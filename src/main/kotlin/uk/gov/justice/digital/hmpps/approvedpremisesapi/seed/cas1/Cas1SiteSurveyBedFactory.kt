package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.File

class Cas1SiteSurveyBedFactory {
  fun load(file: File) = toDataFame(file).toInternalModel()

  private fun toDataFame(file: File) = Cas1SiteSurveyDataFrame(
    dataFrame = DataFrame.readExcel(file, "Sheet3"),
    sheetName = "Sheet3",
  )

  private fun Cas1SiteSurveyDataFrame.toInternalModel(): List<Cas1SiteSurveyBed> {
    val beds = mutableListOf<Cas1SiteSurveyBed>()
    for (i in 1..<dataFrame.columnsCount()) {
      beds.add(
        Cas1SiteSurveyBed(
          uniqueBedRef = resolveAnswer("Unique Reference Number for Bed", i),
          roomNumber = this.resolveAnswer("Room Number / Name", i),
          bedNumber = this.resolveAnswer(
            "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
            i,
          ),
          isSingle = this.resolveAnswerYesNoDropDown("Is this bed in a single room?", i),
          isGroundFloor = this.resolveAnswerYesNoDropDown("Is this room located on the ground floor?", i),
          isFullyFm = this.resolveAnswerYesNoDropDown("Is the room using only furnishings and bedding supplied by FM?", i),
          hasCrib7Bedding = this.resolveAnswerYesNoDropDown("Does this room have Crib7 rated bedding?", i),
          hasSmokeDetector = this.resolveAnswerYesNoDropDown("Is there a smoke/heat detector in the room?", i),
          isTopFloorVulnerable = this.resolveAnswerYesNoDropDown(
            "Is this room on the top floor with at least one external wall and not located directly next to a fire exit or a protected stairway?",
            i,
          ),
          isGroundFloorNrOffice = this.resolveAnswerYesNoDropDown(
            "Is the room close to the admin/staff office on the ground floor with at least one external wall and not located directly next to a fire exit or a protected stairway?",
            i,
          ),
          hasNearbySprinkler = resolveAnswerYesNoDropDown("is there a water mist extinguisher in close proximity to this room?", i),
          isArsonSuitable = resolveAnswerYesNoDropDown("Is this room suitable for people who pose an arson risk? (Must answer yes to Q; 6 & 7, and 9 or  10)", i),
          hasArsonInsuranceConditions = resolveAnswerYesNoNaDropDown("If IAP - Is there any insurance conditions that prevent a person with arson convictions being placed?", i),
          isSuitedForSexOffenders = resolveAnswerYesNoDropDown("Is this room suitable for people convicted of sexual offences?", i),
          hasEnSuite = resolveAnswerYesNoDropDown("Does this room have en-suite bathroom facilities?", i),
          isWheelchairAccessible = resolveAnswerYesNoDropDown("Are corridors leading to this room of sufficient width to accommodate a wheelchair? (at least 1.2m wide)", i),
          hasWideDoor = resolveAnswerYesNoDropDown("Is the door to this room at least 900mm wide?", i),
          hasStepFreeAccess = resolveAnswerYesNoDropDown("Is there step free access to this room and in corridors leading to this room?", i),
          hasFixedMobilityAids = resolveAnswerYesNoDropDown("Are there fixed mobility aids in this room?", i),
          hasTurningSpace = resolveAnswerYesNoDropDown("Does this room have at least a 1500mmx1500mm turning space?", i),
          hasCallForAssistance = resolveAnswerYesNoDropDown("Is there provision for people to call for assistance from this room?", i),
          isWheelchairDesignated = resolveAnswerYesNoDropDown(
            "Can this room be designated as suitable for wheelchair users?   Must answer yes to Q23-26 on previous sheet and Q17-19 & 21 on this sheet)",
            i,
          ),
          isStepFreeDesignated = this.resolveAnswerYesNoDropDown(
            "Can this room be designated as suitable for people requiring step free access? (Must answer yes to Q23 and 25 on previous sheet and Q19 on this sheet)",
            i,
          ),
        ),
      )
    }
    return beds
  }

  data class Cas1SiteSurveyBed(
    val uniqueBedRef: String,
    val roomNumber: String,
    val bedNumber: String,
    val isSingle: Boolean,
    val isGroundFloor: Boolean,
    val isFullyFm: Boolean,
    val hasCrib7Bedding: Boolean,
    val hasSmokeDetector: Boolean,
    val isTopFloorVulnerable: Boolean,
    val isGroundFloorNrOffice: Boolean,
    val hasNearbySprinkler: Boolean,
    val isArsonSuitable: Boolean,
    val hasArsonInsuranceConditions: Boolean,
    val isSuitedForSexOffenders: Boolean,
    val hasEnSuite: Boolean,
    val isWheelchairAccessible: Boolean,
    val hasWideDoor: Boolean,
    val hasStepFreeAccess: Boolean,
    val hasFixedMobilityAids: Boolean,
    val hasTurningSpace: Boolean,
    val hasCallForAssistance: Boolean,
    val isWheelchairDesignated: Boolean,
    val isStepFreeDesignated: Boolean,
  )
}

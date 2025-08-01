package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed

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
      val uniqueBedRef = resolveAnswer(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Unique Reference Number for Bed"), i)

      // excel can have 'phantom' columns that aren't populated with any values
      // but are presented via dataframe regardless. Dataframe labels these columns
      // with the header 'untitled'
      if (uniqueBedRef == "untitled") {
        continue
      }

      beds.add(
        Cas1SiteSurveyBed(
          uniqueBedRef = uniqueBedRef,
          roomNumber = this.resolveAnswer(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Room Number / Name"), i),
          bedNumber = this.resolveAnswer(
            Cas1SiteSurveyDataFrame.QuestionToMatch.Exact(
              "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
            ),
            i,
          ),
          isSingle = this.resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Is this bed in a single room?"), i),
          isGroundFloor = this.resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Is this room located on the ground floor?"), i),
          isFullyFm = this.resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Is the room using only furnishings and bedding supplied by FM?"), i),
          hasCrib7Bedding = this.resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Does this room have Crib7 rated bedding?"), i),
          hasSmokeDetector = this.resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Is there a smoke/heat detector in the room?"), i),
          isTopFloorVulnerable = this.resolveAnswerYesNoDropDown(
            Cas1SiteSurveyDataFrame.QuestionToMatch.Exact(
              "Is this room on the top floor with at least one external wall and not located directly next to a fire exit or a protected stairway?",
            ),
            i,
          ),
          isGroundFloorNrOffice = this.resolveAnswerYesNoDropDown(
            Cas1SiteSurveyDataFrame.QuestionToMatch.Exact(
              "Is the room close to the admin/staff office on the ground floor with at least one external wall and not located directly next to a fire exit or a protected stairway?",
            ),
            i,
          ),
          hasNearbySprinkler = resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("is there a water mist extinguisher in close proximity to this room?"), i),
          isArsonSuitable = resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.StartsWith("Is this room suitable for people who pose an arson risk?"), i),
          hasArsonInsuranceConditions = resolveAnswerYesNoNaDropDown(
            Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("If IAP - Is there any insurance conditions that prevent a person with arson convictions being placed?"),
            i,
          ),
          isSuitedForSexOffenders = resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Is this room suitable for people convicted of sexual offences?"), i),
          hasEnSuite = resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Does this room have en-suite bathroom facilities?"), i),
          isWheelchairAccessible = resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Are corridors leading to this room of sufficient width to accommodate a wheelchair? (at least 1.2m wide)"), i),
          hasWideDoor = resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Is the door to this room at least 900mm wide?"), i),
          hasStepFreeAccess = resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Is there step free access to this room and in corridors leading to this room?"), i),
          hasFixedMobilityAids = resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Are there fixed mobility aids in this room?"), i),
          hasTurningSpace = resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Does this room have at least a 1500mmx1500mm turning space?"), i),
          hasCallForAssistance = resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.Exact("Is there provision for people to call for assistance from this room?"), i),
          isWheelchairDesignated = resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.StartsWith("Can this room be designated as suitable for wheelchair users?"), i),
          isStepFreeDesignated = this.resolveAnswerYesNoDropDown(Cas1SiteSurveyDataFrame.QuestionToMatch.StartsWith("Can this room be designated as suitable for people requiring step free access?"), i),
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

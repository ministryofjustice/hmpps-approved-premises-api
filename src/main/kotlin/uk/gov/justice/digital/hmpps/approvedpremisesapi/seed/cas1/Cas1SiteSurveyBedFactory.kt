package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.File

class Cas1SiteSurveyBedFactory {
  fun load(file: File) = toDataFame(file).toInternalModel()

  private fun toDataFame(file: File) = DataFrame.readExcel(file, "Sheet3")

  private fun String.dropDownYesNoToBoolean(): Boolean = when (this.uppercase()) {
    "YES" -> true
    "NO" -> false
    else -> error("Invalid value for Yes/No dropdown: $this")
  }

  private fun String.dropDownYesNoNaToBoolean(): Boolean = when (this.uppercase()) {
    "YES" -> true
    "NO" -> false
    "N/A" -> false
    else -> error("Invalid value for Yes/No/N/A dropdown: $this")
  }

  fun DataFrame<*>.toInternalModel(): List<Cas1SiteSurveyBed> {
    val beds = mutableListOf<Cas1SiteSurveyBed>()
    for (i in 1..<this.columnsCount()) {
      beds.add(
        Cas1SiteSurveyBed(
          uniqueBedRef = this.resolveAnswer("Unique Reference Number for Bed", i),
          roomNumber = this.resolveAnswer("Room Number / Name", i),
          bedNumber = this.resolveAnswer(
            "Bed Number (in this room i.e if this is a single room insert 1.  If this is a shared room separate entries will need to be made for bed 1 and bed 2)",
            i,
          ),
          isSingle = this.resolveAnswer("Is this bed in a single room?", i).dropDownYesNoToBoolean(),
          isGroundFloor = this.resolveAnswer("Is this room located on the ground floor?", i).dropDownYesNoToBoolean(),
          isFullyFm = this.resolveAnswer("Is the room using only furnishings and bedding supplied by FM?", i)
            .dropDownYesNoToBoolean(),
          hasCrib7Bedding = this.resolveAnswer("Does this room have Crib7 rated bedding?", i).dropDownYesNoToBoolean(),
          hasSmokeDetector = this.resolveAnswer("Is there a smoke/heat detector in the room?", i)
            .dropDownYesNoToBoolean(),
          isTopFloorVulnerable = this.resolveAnswer(
            "Is this room on the top floor with at least one external wall and not located directly next to a fire exit or a protected stairway?",
            i,
          ).dropDownYesNoToBoolean(),
          isGroundFloorNrOffice = this.resolveAnswer(
            "Is the room close to the admin/staff office on the ground floor with at least one external wall and not located directly next to a fire exit or a protected stairway?",
            i,
          ).dropDownYesNoToBoolean(),
          hasNearbySprinkler = this.resolveAnswer(
            "is there a water mist extinguisher in close proximity to this room?",
            i,
          ).dropDownYesNoToBoolean(),
          isArsonSuitable = this.resolveAnswer(
            "Is this room suitable for people who pose an arson risk? (Must answer yes to Q; 6 & 7, and 9 or  10)",
            i,
          ).dropDownYesNoToBoolean(),
          isArsonDesignated = this.resolveAnswer("Is this room currently a designated arson room?", i)
            .dropDownYesNoToBoolean(),
          hasArsonInsuranceConditions = this.resolveAnswer(
            "If IAP - Is there any insurance conditions that prevent a person with arson convictions being placed?",
            i,
          ).dropDownYesNoNaToBoolean(),
          isSuitedForSexOffenders = this.resolveAnswer(
            "Is this room suitable for people convicted of sexual offences?",
            i,
          ).dropDownYesNoToBoolean(),
          hasEnSuite = this.resolveAnswer("Does this room have en-suite bathroom facilities?", i)
            .dropDownYesNoToBoolean(),
          isWheelchairAccessible = this.resolveAnswer(
            "Are corridors leading to this room of sufficient width to accommodate a wheelchair? (at least 1.2m wide)",
            i,
          ).dropDownYesNoToBoolean(),
          hasWideDoor = this.resolveAnswer("Is the door to this room at least 900mm wide?", i).dropDownYesNoToBoolean(),
          hasStepFreeAccess = this.resolveAnswer(
            "Is there step free access to this room and in corridors leading to this room?",
            i,
          ).dropDownYesNoToBoolean(),
          hasFixedMobilityAids = this.resolveAnswer("Are there fixed mobility aids in this room?", i)
            .dropDownYesNoToBoolean(),
          hasTurningSpace = this.resolveAnswer("Does this room have at least a 1500mmx1500mm turning space?", i)
            .dropDownYesNoToBoolean(),
          hasCallForAssistance = this.resolveAnswer(
            "Is there provision for people to call for assistance from this room?",
            i,
          ).dropDownYesNoToBoolean(),
          isWheelchairDesignated = this.resolveAnswer(
            "Can this room be designated as suitable for wheelchair users?   Must answer yes to Q23-26 on previous sheet and Q17-19 & 21 on this sheet)",
            i,
          ).dropDownYesNoToBoolean(),
          isStepFreeDesignated = this.resolveAnswer(
            "Can this room be designated as suitable for people requiring step free access? (Must answer yes to Q23 and 25 on previous sheet and Q19 on this sheet)",
            i,
          ).dropDownYesNoToBoolean(),
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
    val isArsonDesignated: Boolean,
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

package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.File

class Cas1SiteSurveyPremiseFactory {

  fun load(file: File) = toDataFame(file).toInternalModel()

  fun getQCode(file: File) = toDataFame(file).getQCode()

  private fun toDataFame(file: File) = Cas1SiteSurveyDataFrame(
    dataFrame = DataFrame.readExcel(file, "Sheet2"),
    sheetName = "Sheet2",
  )

  private fun Cas1SiteSurveyDataFrame.getQCode(): String {
    ensureCorrectColumnCount()

    return resolveAnswer("AP Identifier (Q No.)")
  }

  private fun Cas1SiteSurveyDataFrame.toInternalModel(): Cas1SiteSurveyPremise {
    ensureCorrectColumnCount()

    return Cas1SiteSurveyPremise(
      name = resolveAnswer("Name of AP"),
      qCode = resolveAnswer("AP Identifier (Q No.)"),
      apArea = resolveAnswer("AP Area"),
      probationDeliveryUnit = resolveAnswer("Probation Delivery Unit"),
      probationRegion = resolveAnswer("Probation Region").dropDownToSiteSurveyProbationRegion(),
      localAuthorityArea = resolveAnswer("Local Authority Area"),
      townCity = resolveAnswer("Town / City"),
      address = resolveAnswer("Address"),
      postcode = resolveAnswer("Postcode"),
      maleFemale = resolveAnswer("Male / Female AP?").dropDownToMaleFemale(),
      iap = resolveAnswer("Is this an IAP?").dropDownYesNoToBoolean(),
      pipe = resolveAnswer("Is this AP a PIPE?").dropDownYesNoToBoolean(),
      enhancedSecuritySite = resolveAnswer("Is this AP an Enhanced Security Site?").dropDownYesNoToBoolean(),
      mentalHealth = resolveAnswer("Is this AP semi specialist - Mental Health?").dropDownYesNoToBoolean(),
      recoveryFocussed = resolveAnswer("Is this a Recovery Focussed AP?").dropDownYesNoToBoolean(),
      suitableForPeopleAtRiskOfCriminalExploitation =
      resolveAnswer("Is this AP suitable for people at risk of criminal exploitation? N.B Enhanced Security sites answer No, other AP's answer Yes.")
        .dropDownYesNoToBoolean(),
      willAcceptPeopleWhoHave = WillAcceptPeopleWhoHave(
        committedSexualOffencesAgainstAdults = resolveAnswer("Does this AP accept people who have committed sexual offences against adults?").dropDownYesNoToBoolean(),
        committedSexualOffencesAgainstChildren = resolveAnswer("Does this AP accept people who have committed sexual offences against children?").dropDownYesNoToBoolean(),
        committedNonSexualOffencesAgainstChildren = resolveAnswer("Does this AP accept people who have committed non-sexual offences against children?").dropDownYesNoToBoolean(),
        beenConvictedOfHateCrimes = resolveAnswer("Does this AP accept people who have been convicted of hate crimes?").dropDownYesNoToBoolean(),
      ),
      cateredOrSelfCatered = resolveAnswer("Is this AP Catered? Self catering AP's answer 'No'").dropDownYesNoToBoolean(),
      stepFreeEntrance = resolveAnswer("Is there a step free entrance to the AP at least 900mm wide?").dropDownYesNoToBoolean(),
      corridorsAtLeast1200CmWide = resolveAnswer("Are corridors leading to communal areas at least 1.2m wide?").dropDownYesNoToBoolean(),
      corridorsHaveStepFreeAccess = resolveAnswer("Do corridors leading to communal areas have step free access?").dropDownYesNoToBoolean(),
      bathroomFacilitiesAdaptedForWheelchairUsers = resolveAnswer("Does this AP have bathroom facilities that have been adapted for wheelchair users?").dropDownYesNoToBoolean(),
      hasALift = resolveAnswer("Is there a lift at this AP?").dropDownYesNoToBoolean(),
      hasTactileAndDirectionalFlooring = resolveAnswer("Does this AP have tactile & directional flooring?").dropDownYesNoToBoolean(),
      hasSignsInBraille = resolveAnswer("Does this AP have signs in braille?").dropDownYesNoToBoolean(),
      hasAHearingLoop = resolveAnswer("Does this AP have or has access to a hearing loop?").dropDownYesNoToBoolean(),
      additionalRestrictions = resolveAnswerOptional("Are there any additional restrictions on people that this AP can accommodate?"),
    )
  }

  private fun Cas1SiteSurveyDataFrame.ensureCorrectColumnCount() {
    val columnsCount = dataFrame.columnsCount()
    if (columnsCount < 2) {
      error("Inadequate number of columns. Expected at least 2 columns, got $columnsCount")
    }
  }

  private fun String.dropDownToMaleFemale() = if (this == "Male") {
    MaleFemale.MALE
  } else {
    MaleFemale.FEMALE
  }

  private fun String.dropDownYesNoToBoolean() = this.uppercase() == "YES"

  private fun String.dropDownToSiteSurveyProbationRegion() = when (this) {
    "London" -> SiteSurveyProbationRegion.LONDON
    "Kent Surrey & Sussex" -> SiteSurveyProbationRegion.KENT_SURREY_SUSSEX
    "East of England" -> SiteSurveyProbationRegion.EAST_OF_ENGLAND
    "East Midlands" -> SiteSurveyProbationRegion.EAST_MIDLANDS
    "West Midlands" -> SiteSurveyProbationRegion.WEST_MIDLANDS
    "Yorks & The Humber" -> SiteSurveyProbationRegion.YORKS_AND_HUMBER
    "North East" -> SiteSurveyProbationRegion.NORTH_EAST
    "North West" -> SiteSurveyProbationRegion.NORTH_WEST
    "Greater Manchester" -> SiteSurveyProbationRegion.GREATER_MANCHESTER
    "South West" -> SiteSurveyProbationRegion.SOUTH_WEST
    "South Central" -> SiteSurveyProbationRegion.SOUTH_CENTRAL
    "Wales" -> SiteSurveyProbationRegion.WALES
    else -> error("Could not resolve site survey probation region for '$this'")
  }
}

data class Cas1SiteSurveyPremise(
  val name: String,
  val qCode: String,
  val apArea: String,
  val probationDeliveryUnit: String,
  val probationRegion: SiteSurveyProbationRegion,
  val localAuthorityArea: String,
  val townCity: String,
  val address: String,
  val postcode: String,
  val maleFemale: MaleFemale,
  val iap: Boolean,
  val pipe: Boolean,
  val enhancedSecuritySite: Boolean,
  val mentalHealth: Boolean,
  val recoveryFocussed: Boolean,
  val suitableForPeopleAtRiskOfCriminalExploitation: Boolean,
  val willAcceptPeopleWhoHave: WillAcceptPeopleWhoHave,
  val cateredOrSelfCatered: Boolean,
  val stepFreeEntrance: Boolean,
  val corridorsAtLeast1200CmWide: Boolean,
  val corridorsHaveStepFreeAccess: Boolean,
  val bathroomFacilitiesAdaptedForWheelchairUsers: Boolean,
  val hasALift: Boolean,
  val hasTactileAndDirectionalFlooring: Boolean,
  val hasSignsInBraille: Boolean,
  val hasAHearingLoop: Boolean,
  val additionalRestrictions: String?,
)

data class WillAcceptPeopleWhoHave(
  val committedSexualOffencesAgainstAdults: Boolean,
  val committedSexualOffencesAgainstChildren: Boolean,
  val committedNonSexualOffencesAgainstChildren: Boolean,
  val beenConvictedOfHateCrimes: Boolean,
)

enum class MaleFemale {
  MALE,
  FEMALE,
}

enum class SiteSurveyProbationRegion {
  LONDON,
  KENT_SURREY_SUSSEX,
  EAST_OF_ENGLAND,
  EAST_MIDLANDS,
  WEST_MIDLANDS,
  YORKS_AND_HUMBER,
  NORTH_EAST,
  NORTH_WEST,
  GREATER_MANCHESTER,
  SOUTH_WEST,
  SOUTH_CENTRAL,
  WALES,
}

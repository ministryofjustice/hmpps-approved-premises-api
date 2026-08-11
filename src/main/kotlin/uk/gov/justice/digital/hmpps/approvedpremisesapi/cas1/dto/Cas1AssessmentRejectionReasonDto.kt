package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1AssessmentRejectionReasonDto(@get:JsonValue val value: String) {

  accommodationNeedOnly("accommodationNeedOnly"),
  needsCannotBeMet("needsCannotBeMet"),
  supervisionPeriodTooShort("supervisionPeriodTooShort"),
  notNecessaryOrProportionate("notNecessaryOrProportionate"),
  riskCanBeManagedOtherWay("riskCanBeManagedOtherWay"),
  insufficientMoveOnPlan("insufficientMoveOnPlan"),
  insufficientContingencyPlan("insufficientContingencyPlan"),
  informationNotProvided("informationNotProvided"),
  insufficientQuality("insufficientQuality"),
  inaccurateOrOutdatedInformation("inaccurateOrOutdatedInformation"),
  riskToCommunity("riskToCommunity"),
  riskToOthersInAP("riskToOthersInAP"),
  riskToStaff("riskToStaff"),
  riskToSelf("riskToSelf"),
  withdrawnByPp("withdrawnByPp"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: String): Cas1AssessmentRejectionReasonDto = entries.first { it.value == value }
  }
}

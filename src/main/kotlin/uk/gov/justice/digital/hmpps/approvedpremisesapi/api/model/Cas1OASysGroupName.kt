package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: RISK_MANAGEMENT_PLAN,OFFENCE_DETAILS,ROSH_SUMMARY,SUPPORTING_INFORMATION,RISK_TO_SELF
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class Cas1OASysGroupName(@get:JsonValue val value: kotlin.String) {

  RISK_MANAGEMENT_PLAN("riskManagementPlan"),
  OFFENCE_DETAILS("offenceDetails"),
  ROSH_SUMMARY("roshSummary"),
  SUPPORTING_INFORMATION("supportingInformation"),
  RISK_TO_SELF("riskToSelf"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1OASysGroupName = values().first { it -> it.value == value }
  }
}

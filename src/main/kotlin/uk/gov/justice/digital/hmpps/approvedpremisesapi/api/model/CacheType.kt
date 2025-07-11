package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: qCodeStaffMembers,userAccess,staffDetails,teamsManagingCase,ukBankHolidays,inmateDetails,crnGetCaseDetailCache
*/
@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class CacheType(@get:JsonValue val value: kotlin.String) {

  qCodeStaffMembers("qCodeStaffMembers"),
  userAccess("userAccess"),
  staffDetails("staffDetails"),
  teamsManagingCase("teamsManagingCase"),
  ukBankHolidays("ukBankHolidays"),
  inmateDetails("inmateDetails"),
  crnGetCaseDetailCache("crnGetCaseDetailCache"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): CacheType = values().first { it -> it.value == value }
  }
}

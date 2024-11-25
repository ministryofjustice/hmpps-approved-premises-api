package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: qCodeStaffMembers,userAccess,staffDetails,teamsManagingCase,ukBankHolidays,inmateDetails
*/
enum class CacheType(val value: kotlin.String) {

  @JsonProperty("qCodeStaffMembers")
  qCodeStaffMembers("qCodeStaffMembers"),

  @JsonProperty("userAccess")
  userAccess("userAccess"),

  @JsonProperty("staffDetails")
  staffDetails("staffDetails"),

  @JsonProperty("teamsManagingCase")
  teamsManagingCase("teamsManagingCase"),

  @JsonProperty("ukBankHolidays")
  ukBankHolidays("ukBankHolidays"),

  @JsonProperty("inmateDetails")
  inmateDetails("inmateDetails"),
}

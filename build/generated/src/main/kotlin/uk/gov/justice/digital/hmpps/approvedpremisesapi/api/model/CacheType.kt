package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: qCodeStaffMembers,userAccess,staffDetails,teamsManagingCase,ukBankHolidays,inmateDetails
*/
enum class CacheType(val value: kotlin.String) {

    @JsonProperty("qCodeStaffMembers") qCodeStaffMembers("qCodeStaffMembers"),
    @JsonProperty("userAccess") userAccess("userAccess"),
    @JsonProperty("staffDetails") staffDetails("staffDetails"),
    @JsonProperty("teamsManagingCase") teamsManagingCase("teamsManagingCase"),
    @JsonProperty("ukBankHolidays") ukBankHolidays("ukBankHolidays"),
    @JsonProperty("inmateDetails") inmateDetails("inmateDetails")
}


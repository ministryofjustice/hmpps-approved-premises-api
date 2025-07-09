package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: qCodeStaffMembers,userAccess,staffDetails,teamsManagingCase,ukBankHolidays,inmateDetails,crnGetCaseDetailCache
*/
enum class CacheType(@get:JsonValue val value: kotlin.String) {

    qCodeStaffMembers("qCodeStaffMembers"),
    userAccess("userAccess"),
    staffDetails("staffDetails"),
    teamsManagingCase("teamsManagingCase"),
    ukBankHolidays("ukBankHolidays"),
    inmateDetails("inmateDetails"),
    crnGetCaseDetailCache("crnGetCaseDetailCache");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): CacheType {
                return values().first{it -> it.value == value}
        }
    }
}


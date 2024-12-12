package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * A member of prison or CAS2 staff
 * @param staffIdentifier 
 * @param name 
 * @param username 
 */
data class Cas2StaffMember(

    @Schema(example = "1501234567", required = true, description = "")
    @get:JsonProperty("staffIdentifier", required = true) val staffIdentifier: kotlin.Long,

    @Schema(example = "John Smith", required = true, description = "")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @Schema(example = "SMITHJ_GEN", description = "")
    @get:JsonProperty("username") val username: kotlin.String? = null
) {

}


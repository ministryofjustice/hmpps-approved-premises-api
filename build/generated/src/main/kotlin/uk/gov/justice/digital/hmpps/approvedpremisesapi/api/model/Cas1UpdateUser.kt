package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param roles 
 * @param qualifications 
 * @param cruManagementAreaOverrideId 
 */
data class Cas1UpdateUser(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("roles", required = true) val roles: kotlin.collections.List<ApprovedPremisesUserRole>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("qualifications", required = true) val qualifications: kotlin.collections.List<UserQualification>,

    @Schema(example = "null", description = "")
    @get:JsonProperty("cruManagementAreaOverrideId") val cruManagementAreaOverrideId: java.util.UUID? = null
) {

}


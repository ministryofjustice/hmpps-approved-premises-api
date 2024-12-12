package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param qualifications 
 * @param roles 
 * @param apArea 
 * @param cruManagementArea CRU Management Area to use. This will be the same as cruManagementAreaDefault unless cruManagementAreaOverride is defined
 * @param cruManagementAreaDefault The CRU Management Area used if no override is defined. This is provided to support the user configuration page.
 * @param permissions 
 * @param cruManagementAreaOverride The CRU Management Area manually set on this user. This is provided to support the user configuration page.
 * @param version 
 */
data class ApprovedPremisesUser(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("qualifications", required = true) val qualifications: kotlin.collections.List<UserQualification>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("roles", required = true) val roles: kotlin.collections.List<ApprovedPremisesUserRole>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("apArea", required = true) val apArea: ApArea,

    @Schema(example = "null", required = true, description = "CRU Management Area to use. This will be the same as cruManagementAreaDefault unless cruManagementAreaOverride is defined")
    @get:JsonProperty("cruManagementArea", required = true) val cruManagementArea: NamedId,

    @Schema(example = "null", required = true, description = "The CRU Management Area used if no override is defined. This is provided to support the user configuration page.")
    @get:JsonProperty("cruManagementAreaDefault", required = true) val cruManagementAreaDefault: NamedId,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("service", required = true) override val service: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) override val id: java.util.UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true) override val name: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("deliusUsername", required = true) override val deliusUsername: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("region", required = true) override val region: ProbationRegion,

    @Schema(example = "null", description = "")
    @get:JsonProperty("permissions") val permissions: kotlin.collections.List<ApprovedPremisesUserPermission>? = null,

    @Schema(example = "null", description = "The CRU Management Area manually set on this user. This is provided to support the user configuration page.")
    @get:JsonProperty("cruManagementAreaOverride") val cruManagementAreaOverride: NamedId? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("version") val version: kotlin.Int? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("email") override val email: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("telephoneNumber") override val telephoneNumber: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("isActive") override val isActive: kotlin.Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("probationDeliveryUnit") override val probationDeliveryUnit: ProbationDeliveryUnit? = null
) : User{

}


package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class ApprovedPremisesUser(

  @get:JsonProperty("qualifications", required = true) val qualifications: kotlin.collections.List<UserQualification>,

  @get:JsonProperty("roles", required = true) val roles: kotlin.collections.List<ApprovedPremisesUserRole>,

  @get:JsonProperty("apArea", required = true) val apArea: ApArea,

  @Schema(example = "null", required = true, description = "CRU Management Area to use. This will be the same as cruManagementAreaDefault unless cruManagementAreaOverride is defined")
  @get:JsonProperty("cruManagementArea", required = true) val cruManagementArea: NamedId,

  @Schema(example = "null", required = true, description = "The CRU Management Area used if no override is defined. This is provided to support the user configuration page.")
  @get:JsonProperty("cruManagementAreaDefault", required = true) val cruManagementAreaDefault: NamedId,

  @get:JsonProperty("service", required = true) override val service: kotlin.String,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("name", required = true) override val name: kotlin.String,

  @get:JsonProperty("deliusUsername", required = true) override val deliusUsername: kotlin.String,

  @get:JsonProperty("region", required = true) override val region: ProbationRegion,

  @get:JsonProperty("permissions") val permissions: kotlin.collections.List<ApprovedPremisesUserPermission>? = null,

  @Schema(example = "null", description = "The CRU Management Area manually set on this user. This is provided to support the user configuration page.")
  @get:JsonProperty("cruManagementAreaOverride") val cruManagementAreaOverride: NamedId? = null,

  @get:JsonProperty("version") val version: kotlin.Int? = null,

  @get:JsonProperty("email") override val email: kotlin.String? = null,

  @get:JsonProperty("telephoneNumber") override val telephoneNumber: kotlin.String? = null,

  @get:JsonProperty("isActive") override val isActive: kotlin.Boolean? = null,

  @get:JsonProperty("probationDeliveryUnit") override val probationDeliveryUnit: ProbationDeliveryUnit? = null,
) : User

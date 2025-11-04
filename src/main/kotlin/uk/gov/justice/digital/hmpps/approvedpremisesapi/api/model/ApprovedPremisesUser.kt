package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
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

  val qualifications: kotlin.collections.List<UserQualification>,

  val roles: kotlin.collections.List<ApprovedPremisesUserRole>,

  val apArea: ApArea,

  @Schema(example = "null", required = true, description = "CRU Management Area to use. This will be the same as cruManagementAreaDefault unless cruManagementAreaOverride is defined")
  val cruManagementArea: NamedId,

  @Schema(example = "null", required = true, description = "The CRU Management Area used if no override is defined. This is provided to support the user configuration page.")
  val cruManagementAreaDefault: NamedId,

  override val service: kotlin.String,

  override val id: java.util.UUID,

  override val name: kotlin.String,

  override val deliusUsername: kotlin.String,

  override val region: ProbationRegion,

  val permissions: kotlin.collections.List<ApprovedPremisesUserPermission>? = null,

  @Schema(example = "null", description = "The CRU Management Area manually set on this user. This is provided to support the user configuration page.")
  val cruManagementAreaOverride: NamedId? = null,

  val version: kotlin.Int? = null,

  override val email: kotlin.String? = null,

  override val telephoneNumber: kotlin.String? = null,

  override val isActive: kotlin.Boolean? = null,

  override val probationDeliveryUnit: ProbationDeliveryUnit? = null,
) : User

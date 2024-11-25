package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param numTasksPending
 * @param numTasksCompleted7Days
 * @param numTasksCompleted30Days
 * @param qualifications
 * @param roles
 * @param apArea This is deprecated. Used cruManagementArea instead as this is used to group task management
 * @param cruManagementArea
 */
data class UserWithWorkload(

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
  @get:JsonProperty("numTasksPending") val numTasksPending: kotlin.Int? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("numTasksCompleted7Days") val numTasksCompleted7Days: kotlin.Int? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("numTasksCompleted30Days") val numTasksCompleted30Days: kotlin.Int? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("qualifications") val qualifications: kotlin.collections.List<UserQualification>? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("roles") val roles: kotlin.collections.List<ApprovedPremisesUserRole>? = null,

  @Schema(example = "null", description = "This is deprecated. Used cruManagementArea instead as this is used to group task management")
  @Deprecated(message = "")
  @get:JsonProperty("apArea") val apArea: ApArea? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("cruManagementArea") val cruManagementArea: NamedId? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("email") override val email: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("telephoneNumber") override val telephoneNumber: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isActive") override val isActive: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("probationDeliveryUnit") override val probationDeliveryUnit: ProbationDeliveryUnit? = null,
) : User {
}

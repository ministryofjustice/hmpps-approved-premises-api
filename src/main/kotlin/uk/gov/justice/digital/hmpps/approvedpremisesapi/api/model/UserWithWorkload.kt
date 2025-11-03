package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * This should be changed to embed the user as a property of this type, instead of 'extending' User. Currently this causes unmarshalling issues in integration tests, if using Jackson, due to discriminators not being logically correct
 * @param numTasksPending
 * @param numTasksCompleted7Days
 * @param numTasksCompleted30Days
 * @param qualifications
 * @param roles
 * @param apArea This is deprecated. Used cruManagementArea instead as this is used to group task management
 * @param cruManagementArea
 */
data class UserWithWorkload(

  @get:JsonProperty("service", required = true) override val service: kotlin.String,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("name", required = true) override val name: kotlin.String,

  @get:JsonProperty("deliusUsername", required = true) override val deliusUsername: kotlin.String,

  @get:JsonProperty("region", required = true) override val region: ProbationRegion,

  @get:JsonProperty("numTasksPending") val numTasksPending: kotlin.Int? = null,

  @get:JsonProperty("numTasksCompleted7Days") val numTasksCompleted7Days: kotlin.Int? = null,

  @get:JsonProperty("numTasksCompleted30Days") val numTasksCompleted30Days: kotlin.Int? = null,

  @get:JsonProperty("qualifications") val qualifications: kotlin.collections.List<UserQualification>? = null,

  @Schema(example = "null", description = "This is deprecated. Used cruManagementArea instead as this is used to group task management")
  @Deprecated(message = "")
  @get:JsonProperty("apArea") val apArea: ApArea? = null,

  @get:JsonProperty("cruManagementArea") val cruManagementArea: NamedId? = null,

  @get:JsonProperty("email") override val email: kotlin.String? = null,

  @get:JsonProperty("telephoneNumber") override val telephoneNumber: kotlin.String? = null,

  @get:JsonProperty("isActive") override val isActive: kotlin.Boolean? = null,

  @get:JsonProperty("probationDeliveryUnit") override val probationDeliveryUnit: ProbationDeliveryUnit? = null,
) : User

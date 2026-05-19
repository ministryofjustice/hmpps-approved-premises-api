package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class UserWithWorkload(

  @get:JsonProperty("service", required = true) override val service: String,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("name", required = true) override val name: String,

  @get:JsonProperty("deliusUsername", required = true) override val deliusUsername: String,

  @get:JsonProperty("region", required = true) override val region: ProbationRegion,

  @get:JsonProperty("numTasksPending") val numTasksPending: Int? = null,

  @get:JsonProperty("numTasksCompleted7Days") val numTasksCompleted7Days: Int? = null,

  @get:JsonProperty("numTasksCompleted30Days") val numTasksCompleted30Days: Int? = null,

  @get:JsonProperty("qualifications") val qualifications: List<UserQualification>? = null,

  @Schema(example = "null", description = "This is deprecated. Used cruManagementArea instead as this is used to group task management")
  @Deprecated(message = "")
  @get:JsonProperty("apArea") val apArea: ApArea? = null,

  @get:JsonProperty("cruManagementArea") val cruManagementArea: NamedId? = null,

  @get:JsonProperty("email") override val email: String? = null,

  @get:JsonProperty("telephoneNumber") override val telephoneNumber: String? = null,

  @get:JsonProperty("isActive") override val isActive: Boolean? = null,

  @get:JsonProperty("probationDeliveryUnit") override val probationDeliveryUnit: ProbationDeliveryUnit? = null,
) : User

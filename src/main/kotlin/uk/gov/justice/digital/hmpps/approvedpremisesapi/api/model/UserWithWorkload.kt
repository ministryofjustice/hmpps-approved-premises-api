package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

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

  override val service: kotlin.String,

  override val id: java.util.UUID,

  override val name: kotlin.String,

  override val deliusUsername: kotlin.String,

  override val region: ProbationRegion,

  val numTasksPending: kotlin.Int? = null,

  val numTasksCompleted7Days: kotlin.Int? = null,

  val numTasksCompleted30Days: kotlin.Int? = null,

  val qualifications: kotlin.collections.List<UserQualification>? = null,

  @Schema(example = "null", description = "This is deprecated. Used cruManagementArea instead as this is used to group task management")
  @Deprecated(message = "")
  val apArea: ApArea? = null,

  val cruManagementArea: NamedId? = null,

  override val email: kotlin.String? = null,

  override val telephoneNumber: kotlin.String? = null,

  override val isActive: kotlin.Boolean? = null,

  override val probationDeliveryUnit: ProbationDeliveryUnit? = null,
) : User

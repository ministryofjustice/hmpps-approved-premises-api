package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param tier
 * @param releaseType
 * @param placementType
 * @param dates
 * @param placementDates Placement apps only have one set of placement dates, use 'dates' instead
 * @param outcome
 */
data class PlacementApplicationTask(

  val tier: RiskTierEnvelope,

  val releaseType: ReleaseTypeOption,

  val placementType: PlacementType,

  val dates: PlacementDates,

  override val taskType: TaskType,

  @Schema(example = "6abb5fa3-e93f-4445-887b-30d081688f44", required = true, description = "")
  override val id: java.util.UUID,

  @Schema(example = "6abb5fa3-e93f-4445-887b-30d081688f44", required = true, description = "")
  override val applicationId: java.util.UUID,

  override val personSummary: PersonSummary,

  @Schema(example = "null", required = true, description = "Superseded by personSummary which provides 'name' as well as 'personType' and 'crn'.")
  override val personName: kotlin.String,

  override val crn: kotlin.String,

  @Schema(example = "null", required = true, description = "The Due date of the task - this is deprecated in favour of the `dueAt` field")
  override val dueDate: java.time.LocalDate,

  override val dueAt: java.time.Instant,

  override val status: TaskStatus,

  override val apType: ApType,

  @Schema(example = "null", description = "Placement apps only have one set of placement dates, use 'dates' instead")
  @Deprecated(message = "")
  val placementDates: kotlin.collections.List<PlacementDates>? = null,

  val outcome: PlacementApplicationDecision? = null,

  override val expectedArrivalDate: java.time.LocalDate? = null,

  override val allocatedToStaffMember: ApprovedPremisesUser? = null,

  override val apArea: ApArea? = null,

  override val probationDeliveryUnit: ProbationDeliveryUnit? = null,

  override val outcomeRecordedAt: java.time.Instant? = null,
) : Task

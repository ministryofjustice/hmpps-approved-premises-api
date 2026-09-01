package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1RequestedPlacementPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import java.time.Instant
import java.util.UUID

@Schema(description = "The API model used when constructing, submitting and approving a PlacementApplication. Once approved this is represented by a RequestForPlacement type")
data class PlacementApplication(
  val applicationId: UUID,
  @Schema(description = "If type is 'Additional', provides the PlacementApplication ID. If type is 'Initial' this field provides a PlacementRequest ID.")
  val id: UUID,
  val createdByUserId: UUID,
  val createdAt: Instant,
  @Schema(description = "If type is 'Additional', provides the PlacementApplication ID. If type is 'Initial' this field shouldn't be used.")
  val assessmentId: UUID,
  val assessmentCompletedAt: Instant,
  val applicationCompletedAt: Instant,
  val canBeWithdrawn: Boolean,
  val isWithdrawn: Boolean,
  val type: PlacementApplicationType,
  @Deprecated("use requestedPlacementPeriod")
  @Schema(deprecated = true, description = "Use requestedPlacementPeriod")
  val placementDates: List<PlacementDates>,
  val submittedAt: Instant? = null,
  val data: Any? = null,
  val document: Any? = null,
  val withdrawalReason: WithdrawPlacementRequestReason? = null,
  @Deprecated("use requestedPlacementPeriod")
  @Schema(deprecated = true, description = "Use requestedPlacementPeriod")
  val dates: PlacementDates? = null,
  val requestedPlacementPeriod: Cas1RequestedPlacementPeriod?,
)

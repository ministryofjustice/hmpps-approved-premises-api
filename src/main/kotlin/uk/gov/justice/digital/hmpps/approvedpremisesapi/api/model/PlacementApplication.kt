package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

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
  @Schema(deprecated = true, description = "Deprecated, use dates. Only populated with values after the placement application has been submitted")
  val placementDates: List<PlacementDates>,
  val submittedAt: Instant? = null,
  val data: Any? = null,
  val document: Any? = null,
  val withdrawalReason: WithdrawPlacementRequestReason? = null,
  @Schema(deprecated = true, description = "please use requestedPlacementPeriod, authorisedPlacementPeriod instead")
  val dates: PlacementDates? = null,
)

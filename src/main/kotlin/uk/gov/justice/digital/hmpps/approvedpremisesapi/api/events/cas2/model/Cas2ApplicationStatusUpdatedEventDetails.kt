package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import io.swagger.v3.oas.annotations.media.Schema

data class Cas2ApplicationStatusUpdatedEventDetails(

  val applicationId: java.util.UUID,

  @Schema(example = "https://community-accommodation-tier-2-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "")
  val applicationUrl: kotlin.String,

  val personReference: PersonReference,

  val newStatus: Cas2Status,

  val updatedBy: ExternalUser,

  val updatedAt: java.time.Instant,

  val applicationOrigin: kotlin.String = "homeDetentionCurfew",
)

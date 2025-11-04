package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param applicationId
 * @param applicationUrl
 * @param personReference
 * @param submittedAt
 * @param submittedBy
 * @param applicationOrigin
 * @param bailHearingDate
 * @param referringPrisonCode
 * @param preferredAreas
 * @param hdcEligibilityDate
 * @param conditionalReleaseDate
 */
data class Cas2ApplicationSubmittedEventDetails(

  val applicationId: java.util.UUID,

  @Schema(example = "https://community-accommodation-tier-2-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713", required = true, description = "")
  val applicationUrl: kotlin.String,

  val personReference: PersonReference,

  val submittedAt: java.time.Instant,

  val submittedBy: Cas2ApplicationSubmittedEventDetailsSubmittedBy,

  val applicationOrigin: kotlin.String = "homeDetentionCurfew",

  @Schema(example = "Thu Mar 30 01:00:00 BST 2023", description = "")
  val bailHearingDate: java.time.LocalDate? = null,

  @Schema(example = "BRI", description = "")
  val referringPrisonCode: kotlin.String? = null,

  @Schema(example = "Leeds | Bradford", description = "")
  val preferredAreas: kotlin.String? = null,

  @Schema(example = "Thu Mar 30 01:00:00 BST 2023", description = "")
  val hdcEligibilityDate: java.time.LocalDate? = null,

  @Schema(example = "Sun Apr 30 01:00:00 BST 2023", description = "")
  val conditionalReleaseDate: java.time.LocalDate? = null,
)

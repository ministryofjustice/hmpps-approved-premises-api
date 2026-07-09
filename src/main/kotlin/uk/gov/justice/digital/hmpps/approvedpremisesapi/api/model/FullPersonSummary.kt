package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto

data class FullPersonSummary(
  val name: String,
  val isRestricted: Boolean,
  override val crn: String,
  override val personType: PersonSummaryDiscriminator,
  @Schema(description = "The person's current tier, if available")
  val tier: TierDto?,
) : PersonSummary

package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto

class RestrictedPerson(
  override val crn: String,
  override val type: PersonType,
  @Schema(description = "The person's current tier, if available")
  val tier: TierDto?,
) : Person

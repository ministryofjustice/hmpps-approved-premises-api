package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto

class RestrictedPersonSummary(
  override val crn: String,
  override val personType: PersonSummaryDiscriminator,
  val tier: TierDto? = null,
) : PersonSummary

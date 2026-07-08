package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto

data class FullPersonSummary(
  val name: String,
  val isRestricted: Boolean,
  override val crn: String,
  override val personType: PersonSummaryDiscriminator,
  val tier: TierDto?,
) : PersonSummary

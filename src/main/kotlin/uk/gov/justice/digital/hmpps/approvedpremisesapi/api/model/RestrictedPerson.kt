package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto

class RestrictedPerson(
  override val crn: String,
  override val type: PersonType,
  val tier: TierDto?,
) : Person

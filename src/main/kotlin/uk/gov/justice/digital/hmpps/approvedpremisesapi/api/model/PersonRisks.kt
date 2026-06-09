package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.RiskTierEnvelopeDto

data class PersonRisks(
  val crn: String,
  val roshRisks: RoshRisksEnvelope,
  val tier: RiskTierEnvelopeDto,
  val flags: FlagsEnvelope,
  val mappa: MappaEnvelope? = null,
)

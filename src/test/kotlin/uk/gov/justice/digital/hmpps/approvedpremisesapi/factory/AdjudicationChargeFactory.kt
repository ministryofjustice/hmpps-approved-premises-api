package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AdjudicationCharge
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class AdjudicationChargeFactory : Factory<AdjudicationCharge> {
  private var oicChargeId: Yielded<String> = { randomStringUpperCase(6) }
  private var offenceCode: Yielded<String> = { randomStringUpperCase(4) }
  private var offenceDescription: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var findingCode: Yielded<String?> = { "PROVED" }

  fun withOidcChargeId(oicChargeId: String) = apply {
    this.oicChargeId = { oicChargeId }
  }

  fun withOffenceCode(offenceCode: String) = apply {
    this.offenceCode = { offenceCode }
  }

  fun withOffenceDescription(offenceDescription: String) = apply {
    this.offenceDescription = { offenceDescription }
  }

  fun withFindingCode(findingCode: String?) = apply {
    this.findingCode = { findingCode }
  }

  override fun produce(): AdjudicationCharge = AdjudicationCharge(
    oicChargeId = this.oicChargeId(),
    offenceCode = this.offenceCode(),
    offenceDescription = this.offenceDescription(),
    findingCode = this.findingCode(),
  )
}

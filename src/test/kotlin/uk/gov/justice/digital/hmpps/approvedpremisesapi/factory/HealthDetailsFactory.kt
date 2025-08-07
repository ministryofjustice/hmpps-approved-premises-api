package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.HealthDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.HealthDetailsInner

class HealthDetailsFactory : Factory<HealthDetails> {
  private var generalHealth: Yielded<Boolean> = { false }
  private var generalHealthSpecify: Yielded<String?> = { null }

  fun withGeneralHealth(generalHealth: Boolean, generalHealthSpecify: String?) = apply {
    this.generalHealth = { generalHealth }
    this.generalHealthSpecify = { generalHealthSpecify }
  }

  override fun produce() = HealthDetails(
    health = HealthDetailsInner(
      generalHealth = this.generalHealth(),
      generalHealthSpecify = this.generalHealthSpecify(),
    ),
  )
}

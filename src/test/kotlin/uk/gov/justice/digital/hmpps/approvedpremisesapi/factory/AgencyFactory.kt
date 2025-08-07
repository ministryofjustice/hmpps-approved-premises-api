package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.Agency

class AgencyFactory : Factory<Agency> {
  private var agencyId: Yielded<String> = { "AGNCY" }
  private var description: Yielded<String> = { "Agency Description" }
  private var agencyType: Yielded<String> = { "INST" }

  fun withAgencyId(agencyId: String) = apply {
    this.agencyId = { agencyId }
  }

  fun withDescription(description: String) = apply {
    this.description = { description }
  }

  fun withAgencyType(agencyType: String) = apply {
    this.agencyType = { agencyType }
  }

  override fun produce(): Agency = Agency(
    agencyId = this.agencyId(),
    description = this.description(),
    agencyType = this.agencyType(),
  )
}

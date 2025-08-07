package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.Agency

class AdjudicationsPageFactory : Factory<AdjudicationsPage> {
  private var results: Yielded<List<Adjudication>> = {
    listOf(
      AdjudicationFactory().produce(),
    )
  }

  private var agencies: Yielded<List<Agency>> = {
    listOf(
      AgencyFactory().produce(),
    )
  }

  fun withResults(results: List<Adjudication>) = apply {
    this.results = { results }
  }

  fun withAgencies(agencies: List<Agency>) = apply {
    this.agencies = { agencies }
  }

  override fun produce(): AdjudicationsPage = AdjudicationsPage(
    results = this.results(),
    agencies = this.agencies(),
  )
}

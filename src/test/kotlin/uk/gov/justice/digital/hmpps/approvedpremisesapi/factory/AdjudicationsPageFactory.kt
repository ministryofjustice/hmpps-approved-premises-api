package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.Results

class AdjudicationsPageFactory : Factory<AdjudicationsPage> {
  private var results: Yielded<Results> = {
    Results(
      content = listOf(
        AdjudicationFactory().produce(),
      ),
    )
  }

  private var agencies: Yielded<List<Agency>> = {
    listOf(
      AgencyFactory().produce(),
    )
  }

  fun withResults(content: List<Adjudication>) = apply {
    this.results = {
      Results(
        content = content,
      )
    }
  }

  fun withAgencies(agencies: List<Agency>) = apply {
    this.agencies = { agencies }
  }

  override fun produce(): AdjudicationsPage = AdjudicationsPage(
    results = this.results(),
    agencies = this.agencies(),
  )
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.AdjudicationsApiResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.Agency

class AdjudicationsApiFactory : Factory<AdjudicationsApiResponse> {
  private var results: Yielded<Page<Adjudication>> = {
    PageImpl(
      listOf(
        AdjudicationFactory().produce(),
      ),
    )
  }

  private var agencies: Yielded<List<Agency>> = {
    listOf(
      AgencyFactory().produce(),
    )
  }

  fun withResults(results: Page<Adjudication>) = apply {
    this.results = { results }
  }

  fun withAgencies(agencies: List<Agency>) = apply {
    this.agencies = { agencies }
  }

  override fun produce(): AdjudicationsApiResponse = AdjudicationsApiResponse(
    results = this.results(),
    agencies = this.agencies(),
  )
}

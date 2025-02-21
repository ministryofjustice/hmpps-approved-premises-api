package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria

class PageCriteriaFactory<S>(defaultSortBy: S) : Factory<PageCriteria<S>> {
  val sortBy: Yielded<S> = { defaultSortBy }
  val sortDirection: Yielded<SortDirection> = { SortDirection.desc }
  val page: Yielded<Int?> = { null }
  val perPage: Yielded<Int?> = { null }

  override fun produce(): PageCriteria<S> = PageCriteria(
    sortBy(),
    sortDirection(),
    page(),
    perPage(),
  )
}

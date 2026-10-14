package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.client.prisoneralerts

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisoneralertsapi.Alert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisoneralertsapi.AlertsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisoneralertsapi.AlertsPagePageable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisoneralertsapi.AlertsPageSort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PrisonerAlertFactory

class AlertsPageFactory : Factory<AlertsPage> {

  private var content = {
    listOf(
      PrisonerAlertFactory().produce(),
      PrisonerAlertFactory().produce(),
      PrisonerAlertFactory().produce(),
    )
  }

  fun withContent(content: List<Alert>) = apply {
    this.content = { content }
  }

  override fun produce() = AlertsPage(
    content = content(),
    totalElements = 3,
    totalPages = 1,
    first = true,
    last = true,
    size = 10,
    number = 0,
    numberOfElements = 3,
    pageable = AlertsPagePageable(
      offset = 0,
      pageNumber = 0,
      pageSize = 10,
      paged = true,
      unpaged = false,
      sort = AlertsPageSort(
        empty = false,
        sorted = true,
        unsorted = false,
      ),
    ),
    sort = AlertsPageSort(
      empty = false,
      sorted = true,
      unsorted = false,
    ),
    empty = false,
  )
}

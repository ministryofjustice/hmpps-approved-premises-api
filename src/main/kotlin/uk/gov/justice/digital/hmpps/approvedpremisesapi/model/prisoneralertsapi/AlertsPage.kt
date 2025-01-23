package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisoneralertsapi

data class AlertsPage(
  val totalElements: Int,
  val totalPages: Int,
  val first: Boolean,
  val last: Boolean,
  val size: Int,
  val content: List<Alert>,
  val number: Int,
  val sort: AlertsPageSort,
  val numberOfElements: Int,
  val pageable: AlertsPagePageable,
  val empty: Boolean,
)

data class AlertsPagePageable(
  val offset: Int,
  val sort: AlertsPageSort,
  val paged: Boolean,
  val unpaged: Boolean,
  val pageNumber: Int,
  val pageSize: Int,
)

data class AlertsPageSort(
  val empty: Boolean,
  val unsorted: Boolean,
  val sorted: Boolean,
)

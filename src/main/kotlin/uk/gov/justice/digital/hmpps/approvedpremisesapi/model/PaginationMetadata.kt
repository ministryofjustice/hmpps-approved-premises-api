package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import org.springframework.http.HttpHeaders

data class PaginationMetadata(val currentPage: Int, val totalPages: Int, val totalResults: Long, val pageSize: Int) {
  fun toHeaders(): HttpHeaders = HttpHeaders().apply {
    set("X-Pagination-CurrentPage", currentPage.toString())
    set("X-Pagination-TotalPages", totalPages.toString())
    set("X-Pagination-TotalResults", totalResults.toString())
    set("X-Pagination-PageSize", pageSize.toString())
  }
}

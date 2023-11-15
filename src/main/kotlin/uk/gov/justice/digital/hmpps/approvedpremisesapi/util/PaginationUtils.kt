package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata

fun getPageable(sortBy: String, sortDirection: SortDirection?, page: Int?): Pageable? {
  return if (page != null) {
    val sort = if (sortDirection == SortDirection.desc) {
      Sort.by(sortBy).descending()
    } else {
      Sort.by(sortBy).ascending()
    }
    PageRequest.of(page - 1, 10, sort)
  } else {
    null
  }
}

fun getPageableOrAllPages(sortBy: String, sortDirection: SortDirection?, page: Int?): Pageable? {
  return if (page != null) {
    getPageable(sortBy, sortDirection, page)
  } else {
    val sort = if (sortDirection == SortDirection.desc) {
      Sort.by(sortBy).descending()
    } else {
      Sort.by(sortBy).ascending()
    }
    PageRequest.of(0, Int.MAX_VALUE, sort)
  }
}

fun <T>getMetadata(response: Page<T>, page: Int?): PaginationMetadata? {
  return if (page != null) {
    PaginationMetadata(page, response.totalPages, response.totalElements, 10)
  } else {
    null
  }
}

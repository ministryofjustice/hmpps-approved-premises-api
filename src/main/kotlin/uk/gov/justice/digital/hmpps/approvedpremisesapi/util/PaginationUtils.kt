package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata

fun getPageable(sortBy: String, sortDirection: SortDirection?, page: Int?): Pageable? {
  return getPageableWithSize(sortBy, sortDirection, page, 10)
}

fun getPageableWithSize(sortBy: String, sortDirection: SortDirection?, page: Int?, pageSize: Int): Pageable? {
  return if (page != null) {
    val sort = if (sortDirection == SortDirection.desc) {
      Sort.by(sortBy).descending()
    } else {
      Sort.by(sortBy).ascending()
    }
    PageRequest.of(page - 1, pageSize, sort)
  } else {
    null
  }
}

fun getPageableOrAllPages(sortBy: String, sortDirection: SortDirection?, page: Int?, pageSize: Int): Pageable? {
  return if (page != null) {
    getPageableWithSize(sortBy, sortDirection, page, pageSize)
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
  return getMetadataWithSize(response, page, 10)
}

fun <T>getMetadataWithSize(response: Page<T>, page: Int?, pageSize: Int): PaginationMetadata? {
  return if (page != null) {
    PaginationMetadata(page, response.totalPages, response.totalElements, pageSize)
  } else {
    null
  }
}

fun getIndices(page: Int, taskCount: Int): Pair<Int, Int> {
  val pageSize = 10
  val endMarker = pageSize - 1
  var start = 0
  if (page > 0) {
    start = (page - 1) * pageSize
  }

  if (start > taskCount) {
    val lastPage = (taskCount / pageSize)
    start = lastPage * pageSize
  }

  var end = start + endMarker

  while (end > taskCount - 1) {
    end -= 1
  }
  return Pair(start, end)
}

fun getMetadata(
  page: Int,
  totalCount: Int,
): PaginationMetadata {
  val pageSize = 10
  val totalPages = (totalCount / pageSize) + 1
  var currentPage = page

  when {
    currentPage > totalPages -> currentPage = totalPages
    currentPage < 1 -> currentPage = 1
  }

  return PaginationMetadata(
    currentPage,
    totalPages,
    totalCount.toLong(),
    pageSize,
  )
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata
import javax.annotation.PostConstruct

private lateinit var config: PaginationConfig

@Component
class PaginationConfig(@Value("\${pagination.default-page-size}") val defaultPageSize: Int) {
  @PostConstruct
  fun postInit() {
    config = this
  }
}

data class PageCriteria<S>(
  val sortBy: S,
  val sortDirection: SortDirection,
  val page: Int?,
  val perPage: Int?,
) {
  fun <S2> withSortBy(sortBy: S2): PageCriteria<S2> {
    return PageCriteria(sortBy, this.sortDirection, this.page, this.perPage)
  }
}

fun getPageable(sortBy: String, sortDirection: SortDirection?, page: Int?): Pageable? {
  return getPageableWithSize(sortBy, sortDirection, page)
}

fun getPageableWithSize(sortBy: String, sortDirection: SortDirection?, page: Int?, pageSize: Int? = null): Pageable? {
  return if (page != null) {
    val sort = if (sortDirection == SortDirection.desc) {
      Sort.by(sortBy).descending()
    } else {
      Sort.by(sortBy).ascending()
    }
    PageRequest.of(page - 1, resolvePageSize(pageSize), sort)
  } else {
    null
  }
}

fun getPageableOrAllPages(sortBy: String, sortDirection: SortDirection?, page: Int?, pageSize: Int?): Pageable? {
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

fun getPageableOrAllPages(criteria: PageCriteria<String>): Pageable? =
  getPageableOrAllPages(
    criteria.sortBy,
    criteria.sortDirection,
    criteria.page,
    criteria.perPage,
  )

fun <T> getMetadata(response: Page<T>, pageCriteria: PageCriteria<*>): PaginationMetadata? = getMetadataWithSize(response, pageCriteria.page, pageCriteria.perPage)

fun <T> getMetadata(response: Page<T>, page: Int?): PaginationMetadata? {
  return getMetadataWithSize(response, page, 10)
}

fun <T> getMetadataWithSize(response: Page<T>, page: Int?, pageSize: Int?): PaginationMetadata? {
  return if (page != null) {
    PaginationMetadata(page, response.totalPages, response.totalElements, resolvePageSize(pageSize))
  } else {
    null
  }
}

fun resolvePageSize(perPage: Int?) = perPage ?: config.defaultPageSize

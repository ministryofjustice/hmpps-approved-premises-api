package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
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
  val perPage: Int? = null,
) {
  fun <S2> withSortBy(sortBy: S2): PageCriteria<S2> {
    return PageCriteria(sortBy, this.sortDirection, this.page, this.perPage)
  }
}

fun getPageable(sortBy: String, sortDirection: SortDirection?, page: Int?, pageSize: Int? = null): Pageable? {
  return if (page != null) {
    PageRequest.of(
      page - 1,
      resolvePageSize(pageSize),
      toSort(sortBy, sortDirection),
    )
  } else {
    null
  }
}

fun getPageable(criteria: PageCriteria<String>): Pageable? = getPageable(
  criteria.sortBy,
  criteria.sortDirection,
  criteria.page,
  criteria.perPage,
)

fun getPageableOrAllPages(sortBy: String, sortDirection: SortDirection?, page: Int?, pageSize: Int?): Pageable? {
  return if (page != null) {
    getPageable(sortBy, sortDirection, page, pageSize)
  } else {
    PageRequest.of(
      0,
      Int.MAX_VALUE,
      toSort(sortBy, sortDirection),
    )
  }
}

fun getPageableOrAllPages(criteria: PageCriteria<String>): Pageable? =
  getPageableOrAllPages(
    criteria.sortBy,
    criteria.sortDirection,
    criteria.page,
    criteria.perPage,
  )

fun <T> wrapWithMetadata(page: Page<T>, pageCriteria: PageCriteria<*>): Pair<List<T>, PaginationMetadata?> {
  return Pair(page.content, getMetadata(page, pageCriteria))
}

fun <T> getMetadata(response: Page<T>, pageCriteria: PageCriteria<*>): PaginationMetadata? =
  getMetadataWithSize(response, pageCriteria.page, pageCriteria.perPage)

fun <T> getMetadata(response: Page<T>, page: Int?, size: Int? = 10): PaginationMetadata? {
  return getMetadataWithSize(response, page, size)
}

fun <T> getMetadataWithSize(response: Page<T>, page: Int?, pageSize: Int?): PaginationMetadata? {
  return if (page != null) {
    PaginationMetadata(page, response.totalPages, response.totalElements, resolvePageSize(pageSize))
  } else {
    null
  }
}

private fun resolvePageSize(perPage: Int?) = perPage ?: config.defaultPageSize

private fun toSort(sortBy: String, sortDirection: SortDirection?): Sort =
  Sort.by(if (sortDirection == SortDirection.desc) DESC else ASC, sortBy)

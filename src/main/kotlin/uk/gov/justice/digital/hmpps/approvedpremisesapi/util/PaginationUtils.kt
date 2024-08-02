package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.data.jpa.domain.JpaSort
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PaginationMetadata

@Suppress("TooManyFunctions")
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

  @Deprecated("This function will ignore sorting if no page is defined. This is most likely not the expected behaviour", ReplaceWith("getPageableOrAllPages"))
  fun toPageable(sortByConverter: String) = getPageable(
    sortByConverter,
    this.sortDirection,
    this.page,
    this.perPage,
  )

  fun toPageableOrAllPages(unsafe: Boolean = false, sortByConverter: String) = getPageableOrAllPages(
    sortByConverter,
    this.sortDirection,
    this.page,
    this.perPage,
    unsafe = unsafe,
  )
}

@Deprecated("This function will ignore sorting if no page is defined. This is most likely not the expected behaviour", ReplaceWith("getPageableOrAllPages"))
fun getPageable(sortBy: String, sortDirection: SortDirection?, page: Int?, pageSize: Int? = null, unsafe: Boolean = false): Pageable? {
  return if (page != null) {
    PageRequest.of(
      page - 1,
      resolvePageSize(pageSize),
      toSort(sortBy, sortDirection, unsafe),
    )
  } else {
    null
  }
}

fun getPageableOrAllPages(sortBy: String, sortDirection: SortDirection?, page: Int?, pageSize: Int?, unsafe: Boolean = false): Pageable {
  return getPageableOrAllPages(listOf(sortBy), sortDirection, page, pageSize, unsafe)
}

fun getPageableOrAllPages(sortBy: List<String>, sortDirection: SortDirection?, page: Int?, pageSize: Int?, unsafe: Boolean = false): Pageable {
  val direction = sortDirection(sortDirection)
  var sort: Sort? = null
  sortBy.mapIndexed { index, entity ->
    sort = if (index == 0) {
      when {
        unsafe -> JpaSort.unsafe(direction, entity)
        else -> Sort.by(direction, entity)
      }
    } else {
      when {
        unsafe -> sort!!.and(JpaSort.unsafe(direction, entity))
        else -> sort!!.and(Sort.by(direction, entity))
      }
    }
  }

  return if (page != null) {
    PageRequest.of(
      page - 1,
      resolvePageSize(pageSize),
      sort,
    )
  } else {
    PageRequest.of(
      0,
      Int.MAX_VALUE,
      sort,
    )
  }
}

fun getPageableOrAllPages(criteria: PageCriteria<String>, unsafe: Boolean = false): Pageable? =
  getPageableOrAllPages(
    criteria.sortBy,
    criteria.sortDirection,
    criteria.page,
    criteria.perPage,
    unsafe,
  )

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

private fun toSort(sortBy: String, sortDirection: SortDirection?, unsafe: Boolean): Sort {
  val direction = sortDirection(sortDirection)

  return if (unsafe) JpaSort.unsafe(direction, sortBy) else Sort.by(direction, sortBy)
}

private fun sortDirection(sortDirection: SortDirection?) = when (sortDirection) {
  SortDirection.desc -> DESC
  else -> ASC
}

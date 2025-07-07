package uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema

@Parameters(
  value = [
    Parameter(
      name = PaginationHeaders.CURRENT_PAGE,
      `in` = ParameterIn.HEADER,
      description = "The current page number",
      required = false,
      content = [Content(schema = Schema(implementation = Int::class))],
    ),
    Parameter(
      name = PaginationHeaders.TOTAL_PAGES,
      `in` = ParameterIn.HEADER,
      description = "The total number of pages",
      required = false,
      content = [Content(schema = Schema(implementation = Int::class))],
    ),
    Parameter(
      name = PaginationHeaders.TOTAL_RESULTS,
      `in` = ParameterIn.HEADER,
      description = "The total number of results",
      required = false,
      content = [Content(schema = Schema(implementation = Int::class))],
    ),
    Parameter(
      name = PaginationHeaders.PAGE_SIZE,
      `in` = ParameterIn.HEADER,
      description = "The size of each page",
      required = false,
      content = [Content(schema = Schema(implementation = Int::class))],
    ),
  ],
)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class PaginationHeaders {
  companion object {
    const val CURRENT_PAGE = "X-Pagination-CurrentPage"
    const val TOTAL_PAGES = "X-Pagination-TotalPages"
    const val TOTAL_RESULTS = "X-Pagination-TotalResults"
    const val PAGE_SIZE = "X-Pagination-PageSIze"
  }
}

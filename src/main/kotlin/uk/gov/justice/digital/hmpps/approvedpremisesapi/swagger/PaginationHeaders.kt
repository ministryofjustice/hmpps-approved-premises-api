package uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger

import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ApiResponses(
  value = [
    ApiResponse(
      responseCode = "200",
      description = "Successful response",
      headers = [
        Header(
          name = PaginationHeaders.CURRENT_PAGE,
          description = "The current page number",
          schema = Schema(implementation = Int::class),
        ),

        Header(
          name = PaginationHeaders.TOTAL_PAGES,
          description = "The total number of pages",
          schema = Schema(implementation = Int::class),
        ),

        Header(
          name = PaginationHeaders.TOTAL_RESULTS,
          description = "The total number of results",
          schema = Schema(implementation = Int::class),
        ),

        Header(
          name = PaginationHeaders.PAGE_SIZE,
          description = "The size of each page",
          schema = Schema(implementation = Int::class),
        ),

      ],
    ),
  ],
)
internal annotation class PaginationHeaders {
  companion object {
    const val CURRENT_PAGE = "X-Pagination-CurrentPage"
    const val TOTAL_PAGES = "X-Pagination-TotalPages"
    const val TOTAL_RESULTS = "X-Pagination-TotalResults"
    const val PAGE_SIZE = "X-Pagination-PageSIze"
  }
}

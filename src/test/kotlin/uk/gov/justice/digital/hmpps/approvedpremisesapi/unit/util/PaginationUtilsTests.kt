package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.TestBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getIndices
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadataWithSize
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableWithSize
import java.time.OffsetDateTime

class PaginationUtilsTests {

  @Test
  fun `getIndices returns correct start and end value`() {
    val (start, end) = getIndices(3, 36)
    assertThat(start).isEqualTo(20)
    assertThat(end).isEqualTo(29)
  }

  @Test
  fun `getIndices returns correct start and end value when give page value zero and taskCount`() {
    val (start, end) = getIndices(0, 36)
    assertThat(start).isEqualTo(0)
    assertThat(end).isEqualTo(9)
  }

  @Test
  fun `getIndices returns correct start and end value when give page value negative number and taskCount`() {
    val (start, end) = getIndices(-2, 36)
    assertThat(start).isEqualTo(0)
    assertThat(end).isEqualTo(9)
  }

  @Test
  fun `getIndices returns correct start and end value when give page value one and taskCount`() {
    val (start, end) = getIndices(1, 36)
    assertThat(start).isEqualTo(0)
    assertThat(end).isEqualTo(9)
  }

  @Test
  fun `getIndices returns correct start and end value when give page value greater than taskCount`() {
    val (start, end) = getIndices(6, 36)
    assertThat(start).isEqualTo(30)
    assertThat(end).isEqualTo(35)
  }

  @Test
  fun `getMetadata gives correct PaginationMetadata with page 1 tasksCount 12`() {
    val page = 1
    val tasksCount = 12
    val metaData = getMetadata(page, tasksCount)
    assertThat(metaData.currentPage).isEqualTo(1)
    assertThat(metaData.pageSize).isEqualTo(10)
    assertThat(metaData.totalPages).isEqualTo(2)
    assertThat(metaData.totalResults).isEqualTo(12)
  }

  @Test
  fun `getMetadata gives correct PaginationMetadata with page zero tasksCount 12`() {
    val page = 0
    val tasksCount = 12
    val metaData = getMetadata(page, tasksCount)
    assertThat(metaData.currentPage).isEqualTo(1)
    assertThat(metaData.pageSize).isEqualTo(10)
    assertThat(metaData.totalPages).isEqualTo(2)
    assertThat(metaData.totalResults).isEqualTo(12)
  }

  @Test
  fun `getMetadata gives correct PaginationMetadata with page negative value tasksCount 12`() {
    val page = -2
    val tasksCount = 12
    val metaData = getMetadata(page, tasksCount)
    assertThat(metaData.currentPage).isEqualTo(1)
    assertThat(metaData.pageSize).isEqualTo(10)
    assertThat(metaData.totalPages).isEqualTo(2)
    assertThat(metaData.totalResults).isEqualTo(12)
  }

  @Test
  fun `getMetadata gives correct PaginationMetadata with page 3 tasksCount 47`() {
    val page = 3
    val tasksCount = 47
    val metaData = getMetadata(page, tasksCount)
    assertThat(metaData.currentPage).isEqualTo(3)
    assertThat(metaData.pageSize).isEqualTo(10)
    assertThat(metaData.totalPages).isEqualTo(5)
    assertThat(metaData.totalResults).isEqualTo(47)
  }

  @Test
  fun `getMetadata gives correct PaginationMetadata with page value greater than tasksCount 37`() {
    val page = 6
    val tasksCount = 37
    val metaData = getMetadata(page, tasksCount)
    assertThat(metaData.currentPage).isEqualTo(4)
    assertThat(metaData.pageSize).isEqualTo(10)
    assertThat(metaData.totalPages).isEqualTo(4)
    assertThat(metaData.totalResults).isEqualTo(37)
  }

  @Test
  fun `getPageableWithSize gives correct pageable value with page size as 50`() {
    val page = 1
    val pageSize = 50
    val sortBy = "id"
    val pageableWithSize = getPageableWithSize(sortBy, null, page, pageSize)

    assertThat(pageableWithSize?.pageSize).isEqualTo(pageSize)
    assertThat(pageableWithSize?.pageNumber).isEqualTo(page - 1)
    assertThat(pageableWithSize?.isPaged).isTrue()
    assertThat(pageableWithSize?.sort).isEqualTo(Sort.by(sortBy).ascending())
  }

  @Test
  fun `getPageable gives correct pageable value with default page size as 10`() {
    val page = 1
    val sortBy = "id"
    val pageableWithSize = getPageable(sortBy, null, page)

    assertThat(pageableWithSize?.pageSize).isEqualTo(10)
    assertThat(pageableWithSize?.pageNumber).isEqualTo(page - 1)
    assertThat(pageableWithSize?.isPaged).isTrue()
    assertThat(pageableWithSize?.sort).isEqualTo(Sort.by(sortBy).ascending())
  }

  @Test
  fun `getPageable gives correct pageable value with default page size 10 and ascending order`() {
    val page = 1
    val sortBy = "id"
    val pageableWithSize = getPageable(sortBy, SortDirection.desc, page)

    assertThat(pageableWithSize?.pageSize).isEqualTo(10)
    assertThat(pageableWithSize?.pageNumber).isEqualTo(page - 1)
    assertThat(pageableWithSize?.isPaged).isTrue()
    assertThat(pageableWithSize?.sort).isEqualTo(Sort.by(sortBy).descending())
  }

  @Test
  fun `getPageable gives null page when page number is not given`() {
    val sortBy = "id"
    val pageableWithSize = getPageable(sortBy, SortDirection.desc, null)

    assertThat(pageableWithSize).isNull()
  }

  @Test
  fun `getPageableOrAllPages gives correct pageable value with given page size 100`() {
    val page = 3
    val pageSize = 100
    val sortBy = "id"
    val pageableWithSize = getPageableOrAllPages(sortBy, null, page, pageSize)

    assertThat(pageableWithSize?.pageSize).isEqualTo(pageSize)
    assertThat(pageableWithSize?.pageNumber).isEqualTo(page - 1)
    assertThat(pageableWithSize?.isPaged).isTrue()
    assertThat(pageableWithSize?.sort).isEqualTo(Sort.by(sortBy).ascending())
  }

  @Test
  fun `getPageableOrAllPages gives correct default pageable value with maximum page size`() {
    val pageSize = 100
    val sortBy = "id"
    val pageableWithSize = getPageableOrAllPages(sortBy, SortDirection.asc, null, pageSize)

    assertThat(pageableWithSize?.pageSize).isEqualTo(Int.MAX_VALUE)
    assertThat(pageableWithSize?.pageNumber).isEqualTo(0)
    assertThat(pageableWithSize?.isPaged).isTrue()
    assertThat(pageableWithSize?.sort).isEqualTo(Sort.by(sortBy).ascending())
  }

  @Test
  fun `getMetadataWithSize return correct meta data with specified page size`() {
    val page = 3
    val pageSize = 100
    val results = buildBookingSearchResultPageWithPagination()

    val metadata = getMetadataWithSize(results, page, pageSize)

    assertThat(metadata?.pageSize).isEqualTo(pageSize)
    assertThat(metadata?.totalPages).isEqualTo(1)
    assertThat(metadata?.currentPage).isEqualTo(page)
  }

  @Test
  fun `getMetadataWithSize return correct meta data with default page size`() {
    val page = 1
    val results = buildBookingSearchResultPageWithPagination()

    val metadata = getMetadata(results, page)

    assertThat(metadata?.pageSize).isEqualTo(10)
    assertThat(metadata?.totalPages).isEqualTo(1)
    assertThat(metadata?.currentPage).isEqualTo(page)
  }

  @Test
  fun `getMetadataWithSize gives null when page is not provided`() {
    val results = buildBookingSearchResultPageWithPagination()

    val metadata = getMetadataWithSize(results, null, 100)

    assertThat(metadata).isNull()
  }

  private fun buildBookingSearchResultPageWithPagination() = PageImpl(
    listOf(
      TestBookingSearchResult()
        .withPersonCrn("crn1")
        .withBookingCreatedAt(OffsetDateTime.now().minusDays(3)),
    ),
  )
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getIndices
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getMetadata

class PaginationUtilsTests {

  @Test
  fun `getIndices returns correct start and end value`() {
    val (start, end) = getIndices(3, 36)
    Assertions.assertThat(start).isEqualTo(20)
    Assertions.assertThat(end).isEqualTo(29)
  }

  @Test
  fun `getIndices returns correct start and end value when give page value zero and taskCount`() {
    val (start, end) = getIndices(0, 36)
    Assertions.assertThat(start).isEqualTo(0)
    Assertions.assertThat(end).isEqualTo(9)
  }

  @Test
  fun `getIndices returns correct start and end value when give page value negative number and taskCount`() {
    val (start, end) = getIndices(-2, 36)
    Assertions.assertThat(start).isEqualTo(0)
    Assertions.assertThat(end).isEqualTo(9)
  }

  @Test
  fun `getIndices returns correct start and end value when give page value one and taskCount`() {
    val (start, end) = getIndices(1, 36)
    Assertions.assertThat(start).isEqualTo(0)
    Assertions.assertThat(end).isEqualTo(9)
  }

  @Test
  fun `getIndices returns correct start and end value when give page value greater than taskCount`() {
    val (start, end) = getIndices(6, 36)
    Assertions.assertThat(start).isEqualTo(30)
    Assertions.assertThat(end).isEqualTo(35)
  }

  @Test
  fun `getMetadata gives correct PaginationMetadata with page 1 tasksCount 12`() {
    val page = 1
    val tasksCount = 12
    val metaData = getMetadata(page, tasksCount)
    Assertions.assertThat(metaData.currentPage).isEqualTo(1)
    Assertions.assertThat(metaData.pageSize).isEqualTo(10)
    Assertions.assertThat(metaData.totalPages).isEqualTo(2)
    Assertions.assertThat(metaData.totalResults).isEqualTo(12)
  }

  @Test
  fun `getMetadata gives correct PaginationMetadata with page zero tasksCount 12`() {
    val page = 0
    val tasksCount = 12
    val metaData = getMetadata(page, tasksCount)
    Assertions.assertThat(metaData.currentPage).isEqualTo(1)
    Assertions.assertThat(metaData.pageSize).isEqualTo(10)
    Assertions.assertThat(metaData.totalPages).isEqualTo(2)
    Assertions.assertThat(metaData.totalResults).isEqualTo(12)
  }

  @Test
  fun `getMetadata gives correct PaginationMetadata with page negative value tasksCount 12`() {
    val page = -2
    val tasksCount = 12
    val metaData = getMetadata(page, tasksCount)
    Assertions.assertThat(metaData.currentPage).isEqualTo(1)
    Assertions.assertThat(metaData.pageSize).isEqualTo(10)
    Assertions.assertThat(metaData.totalPages).isEqualTo(2)
    Assertions.assertThat(metaData.totalResults).isEqualTo(12)
  }

  @Test
  fun `getMetadata gives correct PaginationMetadata with page 3 tasksCount 47`() {
    val page = 3
    val tasksCount = 47
    val metaData = getMetadata(page, tasksCount)
    Assertions.assertThat(metaData.currentPage).isEqualTo(3)
    Assertions.assertThat(metaData.pageSize).isEqualTo(10)
    Assertions.assertThat(metaData.totalPages).isEqualTo(5)
    Assertions.assertThat(metaData.totalResults).isEqualTo(47)
  }

  @Test
  fun `getMetadata gives correct PaginationMetadata with page value greater than tasksCount 37`() {
    val page = 6
    val tasksCount = 37
    val metaData = getMetadata(page, tasksCount)
    Assertions.assertThat(metaData.currentPage).isEqualTo(4)
    Assertions.assertThat(metaData.pageSize).isEqualTo(10)
    Assertions.assertThat(metaData.totalPages).isEqualTo(4)
    Assertions.assertThat(metaData.totalResults).isEqualTo(37)
  }
}

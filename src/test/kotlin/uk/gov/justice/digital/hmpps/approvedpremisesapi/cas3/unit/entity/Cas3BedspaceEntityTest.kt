package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import java.time.LocalDate

class Cas3BedspaceEntityTest {
  val futureDate = LocalDate.now().plusDays(1)
  val today = LocalDate.now()
  val pastDate = LocalDate.now().minusDays(1)

  @Nested
  inner class IsBedspaceOnline {
    @Test
    fun `returns true when startDate is null and endDate is null`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(null).withEndDate(null).produce()
      assertThat(bedspace.isBedspaceOnline()).isTrue
    }

    @Test
    fun `returns true when startDate is in the past and endDate is null`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(pastDate).withEndDate(null).produce()
      assertThat(bedspace.isBedspaceOnline()).isTrue
    }

    @Test
    fun `returns true when startDate is today and endDate is null`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(today).withEndDate(null).produce()
      assertThat(bedspace.isBedspaceOnline()).isTrue
    }

    @Test
    fun `returns false when startDate is in the future and endDate is null`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(futureDate).withEndDate(null).produce()
      assertThat(bedspace.isBedspaceOnline()).isFalse
    }

    @Test
    fun `returns true when startDate is in the past and endDate is in the future`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(pastDate).withEndDate(futureDate).produce()
      assertThat(bedspace.isBedspaceOnline()).isTrue
    }

    @Test
    fun `returns false when startDate is in the past and endDate is today`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(pastDate).withEndDate(today).produce()
      assertThat(bedspace.isBedspaceOnline()).isFalse
    }

    @Test
    fun `returns false when startDate is in the future and endDate is in the future`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(futureDate).withEndDate(futureDate.plusDays(10)).produce()
      assertThat(bedspace.isBedspaceOnline()).isFalse
    }
  }
}

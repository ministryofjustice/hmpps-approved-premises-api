package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import java.time.LocalDate

class Cas3BedspaceEntityTest {
  val futureDate = LocalDate.now().plusDays(1)
  val today = LocalDate.now()
  val pastDate = LocalDate.now().minusDays(1)

  @Nested
  inner class IsBedspaceUpcoming {

    @Test
    fun `returns true when startDate is after today`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(futureDate).produce()
      assertThat(bedspace.isBedspaceUpcoming()).isTrue
    }

    @Test
    fun `returns false when startDate is today`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(today).produce()
      assertThat(bedspace.isBedspaceUpcoming()).isFalse
    }

    @Test
    fun `returns false when startDate is before today`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(pastDate).produce()
      assertThat(bedspace.isBedspaceUpcoming()).isFalse
    }

    @Test
    fun `returns false when startDate is null`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(null).produce()
      assertThat(bedspace.isBedspaceUpcoming()).isFalse
    }
  }

  @Nested
  inner class IsBedspaceArchived {

    @Test
    fun `returns true when endDate is before today`() {
      val bedspace = Cas3BedspaceEntityFactory().withEndDate(pastDate).produce()
      assertThat(bedspace.isBedspaceArchived()).isTrue
    }

    @Test
    fun `returns true when endDate is today`() {
      val bedspace = Cas3BedspaceEntityFactory().withEndDate(today).produce()
      assertThat(bedspace.isBedspaceArchived()).isTrue
    }

    @Test
    fun `returns false when endDate is after today`() {
      val bedspace = Cas3BedspaceEntityFactory().withEndDate(futureDate).produce()
      assertThat(bedspace.isBedspaceArchived()).isFalse
    }

    @Test
    fun `returns false when endDate is null`() {
      val bedspace = Cas3BedspaceEntityFactory().withEndDate(null).produce()
      assertThat(bedspace.isBedspaceArchived()).isFalse
    }
  }

  @Nested
  inner class GetBedspaceStatus {

    @Test
    fun `returns upcoming when isBedspaceUpcoming is true`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(futureDate).withEndDate(null).produce()
      assertThat(bedspace.getBedspaceStatus()).isEqualTo(Cas3BedspaceStatus.upcoming)
    }

    @Test
    fun `returns archived when isBedspaceArchived is true`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(pastDate).withEndDate(pastDate).produce()
      assertThat(bedspace.getBedspaceStatus()).isEqualTo(Cas3BedspaceStatus.archived)
    }

    @Test
    fun `returns online when neither upcoming nor archived`() {
      val bedspace = Cas3BedspaceEntityFactory().withStartDate(pastDate).withEndDate(futureDate).produce()
      assertThat(bedspace.getBedspaceStatus()).isEqualTo(Cas3BedspaceStatus.online)
    }
  }
}

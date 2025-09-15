package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.BedspaceStatusHelper
import java.time.LocalDate

class BedspaceStatusHelperTest {
  val futureDate = LocalDate.now().plusDays(1)
  val today = LocalDate.now()
  val pastDate = LocalDate.now().minusDays(1)

  @Nested
  inner class IsCas3BedspaceActive {
    @Test
    fun `returns true when bedspaceEndDate is null`() {
      val result = BedspaceStatusHelper.isCas3BedspaceActive(null, today)
      assertThat(result).isTrue()
    }

    @Test
    fun `returns true when bedspaceEndDate is after archiveEndDate`() {
      val result = BedspaceStatusHelper.isCas3BedspaceActive(futureDate, today)
      assertThat(result).isTrue()
    }

    @Test
    fun `returns false when bedspaceEndDate is equal to archiveEndDate`() {
      val result = BedspaceStatusHelper.isCas3BedspaceActive(today, today)
      assertThat(result).isFalse()
    }

    @Test
    fun `returns false when bedspaceEndDate is before archiveEndDate`() {
      val result = BedspaceStatusHelper.isCas3BedspaceActive(pastDate, today)
      assertThat(result).isFalse()
    }
  }

  @Nested
  inner class IsBedspaceUpcoming {
    @Test
    fun `returns true when startDate is after today`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceUpcoming(futureDate)).isTrue
    }

    @Test
    fun `returns false when startDate is today`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceUpcoming(today)).isFalse
    }

    @Test
    fun `returns false when startDate is before today`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceUpcoming(pastDate)).isFalse
    }

    @Test
    fun `returns false when startDate is null`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceUpcoming(null)).isFalse
    }
  }

  @Nested
  inner class IsBedspaceArchived {
    @Test
    fun `returns true when endDate is before today`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceArchived(pastDate)).isTrue
    }

    @Test
    fun `returns true when endDate is today`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceArchived(today)).isTrue
    }

    @Test
    fun `returns false when endDate is after today`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceArchived(futureDate)).isFalse
    }

    @Test
    fun `returns false when endDate is null`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceArchived(null)).isFalse
    }
  }

  @Nested
  inner class IsBedspaceOnline {
    @Test
    fun `returns true when startDate is null and endDate is null`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceOnline(null, null)).isTrue
    }

    @Test
    fun `returns true when startDate is in the past and endDate is null`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceOnline(pastDate, null)).isTrue
    }

    @Test
    fun `returns true when startDate is today and endDate is null`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceOnline(today, null)).isTrue
    }

    @Test
    fun `returns false when startDate is in the future and endDate is null`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceOnline(futureDate, null)).isFalse
    }

    @Test
    fun `returns true when startDate is in the past and endDate is in the future`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceOnline(pastDate, futureDate)).isTrue
    }

    @Test
    fun `returns false when startDate is in the past and endDate is today`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceOnline(pastDate, today)).isFalse
    }

    @Test
    fun `returns false when startDate is in the future and endDate is in the future`() {
      assertThat(BedspaceStatusHelper.isCas3BedspaceOnline(futureDate, futureDate.plusDays(10))).isFalse
    }
  }

  @Nested
  inner class GetBedspaceStatus {

    @Test
    fun `returns upcoming when isBedspaceUpcoming is true`() {
      assertThat(BedspaceStatusHelper.getBedspaceStatus(futureDate, null)).isEqualTo(Cas3BedspaceStatus.upcoming)
    }

    @Test
    fun `returns archived when isBedspaceArchived is true`() {
      assertThat(BedspaceStatusHelper.getBedspaceStatus(pastDate, pastDate)).isEqualTo(Cas3BedspaceStatus.archived)
    }

    @Test
    fun `returns online when neither upcoming nor archived`() {
      assertThat(BedspaceStatusHelper.getBedspaceStatus(pastDate, futureDate)).isEqualTo(Cas3BedspaceStatus.online)
    }
  }
}

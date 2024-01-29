package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1WithdrawableDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1WithdrawableDateRepository.WithdrawableDateType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.Cas1WithdrawableDateTransformer
import java.time.LocalDate
import java.util.UUID

class Cas1WithdrawableDateTransformerTest {

  private val transformer = Cas1WithdrawableDateTransformer()

  class Cas1WithdrawableDateImpl(
    override val id: UUID,
    override val type: WithdrawableDateType,
    override val startDate: LocalDate,
    override val endDate: LocalDate,
  ) :
    Cas1WithdrawableDateRepository.Cas1WithdrawableDate

  @Test
  fun `Should successfully transform withdrawable of type placement request`() {
    val id = UUID.randomUUID()
    val startDate = LocalDate.now().minusDays(1)
    val endDate = LocalDate.now().plusDays(1)

    val result = transformer.transformJpaToAPI(
      listOf(
        Cas1WithdrawableDateImpl(
          id,
          WithdrawableDateType.PLACEMENT_REQUEST,
          startDate,
          endDate,
        ),
      ),
    )

    assertThat(result).hasSize(1)
    assertThat(result).contains(
      Withdrawable(
        id,
        WithdrawableType.placementRequest,
        listOf(DatePeriod(startDate, endDate)),
      ),
    )
  }

  @Test
  fun `Should successfully transform withdrawable of type placement application`() {
    val id = UUID.randomUUID()
    val startDate = LocalDate.now().minusDays(1)
    val endDate = LocalDate.now().plusDays(1)

    val result = transformer.transformJpaToAPI(
      listOf(
        Cas1WithdrawableDateImpl(
          id,
          WithdrawableDateType.PLACEMENT_APPLICATION,
          startDate,
          endDate,
        ),
      ),
    )

    assertThat(result).hasSize(1)
    assertThat(result).contains(
      Withdrawable(
        id,
        WithdrawableType.placementApplication,
        listOf(DatePeriod(startDate, endDate)),
      ),
    )
  }

  @Test
  fun `Should successfully transform withdrawable of type booking`() {
    val id = UUID.randomUUID()
    val startDate = LocalDate.now().minusDays(1)
    val endDate = LocalDate.now().plusDays(1)

    val result = transformer.transformJpaToAPI(
      listOf(
        Cas1WithdrawableDateImpl(
          id,
          WithdrawableDateType.BOOKING,
          startDate,
          endDate,
        ),
      ),
    )

    assertThat(result).hasSize(1)
    assertThat(result).contains(
      Withdrawable(
        id,
        WithdrawableType.booking,
        listOf(DatePeriod(startDate, endDate)),
      ),
    )
  }

  @Test
  fun `Should successfully flatten withdrawable with multiple entries into single entry`() {
    val id = UUID.randomUUID()
    val startDate1 = LocalDate.now().minusDays(1)
    val endDate1 = LocalDate.now().plusDays(1)
    val startDate2 = LocalDate.now().minusDays(2)
    val endDate2 = LocalDate.now().plusDays(2)
    val startDate3 = LocalDate.now().minusDays(3)
    val endDate3 = LocalDate.now().plusDays(3)

    val otherId = UUID.randomUUID()
    val otherStartDate = LocalDate.now().minusDays(10)
    val otherEndDate = LocalDate.now().plusDays(10)

    val result = transformer.transformJpaToAPI(
      listOf(
        Cas1WithdrawableDateImpl(
          id,
          WithdrawableDateType.PLACEMENT_APPLICATION,
          startDate1,
          endDate1,
        ),
        Cas1WithdrawableDateImpl(
          id,
          WithdrawableDateType.PLACEMENT_APPLICATION,
          startDate2,
          endDate2,
        ),
        Cas1WithdrawableDateImpl(
          otherId,
          WithdrawableDateType.PLACEMENT_REQUEST,
          otherStartDate,
          otherEndDate,
        ),
        Cas1WithdrawableDateImpl(
          id,
          WithdrawableDateType.PLACEMENT_APPLICATION,
          startDate3,
          endDate3,
        ),
      ),
    )

    assertThat(result).hasSize(2)
    assertThat(result).contains(
      Withdrawable(
        id,
        WithdrawableType.placementApplication,
        listOf(
          DatePeriod(startDate1, endDate1),
          DatePeriod(startDate2, endDate2),
          DatePeriod(startDate3, endDate3),
        ),
      ),
      Withdrawable(
        otherId,
        WithdrawableType.placementRequest,
        listOf(
          DatePeriod(otherStartDate, otherEndDate),
        ),
      ),
    )
  }
}

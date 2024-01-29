package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1WithdrawableDateRepository.Cas1WithdrawableDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1WithdrawableDateRepository.WithdrawableDateType

@Component
class Cas1WithdrawableDateTransformer {

  fun transformJpaToAPI(cas1WithdrawableDates: List<Cas1WithdrawableDate>): List<Withdrawable> {
    return cas1WithdrawableDates
      .groupingBy { it.id }
      .fold(
        initialValueSelector = { key, first -> Withdrawable(key, toType(first.type), emptyList()) },
        operation = { _, accumulator, element -> accumulator.copy(dates = accumulator.dates + toDatePeriod(element)) },
      ).values.toList()
  }

  private fun toDatePeriod(cas1WithdrawableDate: Cas1WithdrawableDate) =
    DatePeriod(cas1WithdrawableDate.startDate, cas1WithdrawableDate.endDate)

  private fun toType(type: WithdrawableDateType) = when (type) {
    WithdrawableDateType.PLACEMENT_REQUEST -> WithdrawableType.placementRequest
    WithdrawableDateType.PLACEMENT_APPLICATION -> WithdrawableType.placementApplication
    WithdrawableDateType.BOOKING -> WithdrawableType.booking
  }
}

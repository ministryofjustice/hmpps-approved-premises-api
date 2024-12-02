package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType

@Component
class WithdrawableTransformer {

  fun toApi(entity: WithdrawableEntity) = Withdrawable(
    entity.id,
    when (entity.type) {
      WithdrawableEntityType.Application -> WithdrawableType.APPLICATION
      WithdrawableEntityType.PlacementRequest -> WithdrawableType.PLACEMENT_REQUEST
      WithdrawableEntityType.PlacementApplication -> WithdrawableType.PLACEMENT_APPLICATION
      WithdrawableEntityType.Booking -> WithdrawableType.BOOKING
      WithdrawableEntityType.SpaceBooking -> WithdrawableType.SPACE_BOOKING
    },
    entity.dates.map { DatePeriod(it.startDate, it.endDate) },
  )
}

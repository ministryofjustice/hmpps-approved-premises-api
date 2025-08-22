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
      WithdrawableEntityType.Application -> WithdrawableType.application
      WithdrawableEntityType.PlacementApplication -> WithdrawableType.placementApplication
      WithdrawableEntityType.SpaceBooking -> WithdrawableType.spaceBooking
      WithdrawableEntityType.PlacementRequest -> error("It's not possible to withdraw a placement request directly")
    },
    entity.dates.map { DatePeriod(it.startDate, it.endDate) },
  )
}

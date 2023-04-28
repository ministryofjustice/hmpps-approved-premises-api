package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundEntity

@Component
class TurnaroundTransformer {
  fun transformJpaToApi(jpa: TurnaroundEntity) = Turnaround(
    id = jpa.id,
    bookingId = jpa.booking.id,
    workingDays = jpa.workingDayCount,
    createdAt = jpa.createdAt.toInstant(),
  )
}

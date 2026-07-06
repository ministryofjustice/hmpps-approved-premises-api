package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Turnaround

@Component
class Cas3TurnaroundTransformer {

  fun transformJpaToApi(jpa: Cas3v2TurnaroundEntity) = Cas3Turnaround(
    id = jpa.id,
    bookingId = jpa.booking.id,
    workingDays = jpa.workingDayCount,
    createdAt = jpa.createdAt.toInstant(),
  )
}

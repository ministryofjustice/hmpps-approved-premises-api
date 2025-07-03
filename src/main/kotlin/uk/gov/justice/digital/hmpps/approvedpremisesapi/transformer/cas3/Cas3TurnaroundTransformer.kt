package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2TurnaroundEntity as Cas3v2TurnaroundEntity

@Component
class Cas3TurnaroundTransformer {
  fun transformJpaToApi(jpa: Cas3TurnaroundEntity) = Turnaround(
    id = jpa.id,
    bookingId = jpa.booking.id,
    workingDays = jpa.workingDayCount,
    createdAt = jpa.createdAt.toInstant(),
  )

  fun transformJpaToApi(jpa: Cas3v2TurnaroundEntity) = Cas3Turnaround(
    id = jpa.id,
    bookingId = jpa.booking.id,
    workingDays = jpa.workingDayCount,
    createdAt = jpa.createdAt.toInstant(),
  )
}

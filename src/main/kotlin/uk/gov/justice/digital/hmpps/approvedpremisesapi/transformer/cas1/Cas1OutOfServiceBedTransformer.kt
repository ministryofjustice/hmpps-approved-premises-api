package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import java.time.Duration
import java.time.LocalDate

@Component
class Cas1OutOfServiceBedTransformer(
  private val cas1OutOfServiceBedReasonTransformer: Cas1OutOfServiceBedReasonTransformer,
  private val cas1OutOfServiceBedCancellationTransformer: Cas1OutOfServiceBedCancellationTransformer,
  private val cas1OutOfServiceBedRevisionTransformer: Cas1OutOfServiceBedRevisionTransformer,
) {
  fun transformJpaToApi(jpa: Cas1OutOfServiceBedEntity) = Cas1OutOfServiceBed(
    id = jpa.id,
    createdAt = jpa.createdAt.toInstant(),
    startDate = jpa.startDate,
    endDate = jpa.endDate,
    bed = NamedId(jpa.bed.id, jpa.bed.name),
    room = NamedId(jpa.bed.room.id, jpa.bed.room.name),
    premises = NamedId(jpa.premises.id, jpa.premises.name),
    apArea = NamedId(jpa.premises.probationRegion.apArea.id, jpa.premises.probationRegion.apArea.name),
    reason = cas1OutOfServiceBedReasonTransformer.transformJpaToApi(jpa.reason),
    daysLostCount = jpa.deriveDaysLost(),
    temporality = jpa.deriveTemporality(),
    status = jpa.deriveStatus(),
    referenceNumber = jpa.referenceNumber,
    notes = jpa.notes,
    cancellation = jpa.cancellation?.let { cas1OutOfServiceBedCancellationTransformer.transformJpaToApi(it) },
    revisionHistory = jpa.revisionHistory.map(cas1OutOfServiceBedRevisionTransformer::transformJpaToApi),
  )

  private fun Cas1OutOfServiceBedEntity.deriveStatus() = when (this.cancellation) {
    null -> Cas1OutOfServiceBedStatus.active
    else -> Cas1OutOfServiceBedStatus.cancelled
  }

  private fun Cas1OutOfServiceBedEntity.deriveDaysLost() = Duration
    .between(this.startDate.atStartOfDay(), this.endDate.plusDays(1).atStartOfDay())
    .toDays()
    .toInt()

  private fun Cas1OutOfServiceBedEntity.deriveTemporality(): Temporality {
    val now = LocalDate.now()

    return when {
      now > this.endDate -> Temporality.past
      now < this.startDate -> Temporality.future
      else -> Temporality.current
    }
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesLostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationLostBedEntity

@Component
class LostBedsTransformer(
  private val lostBedReasonTransformer: LostBedReasonTransformer,
  private val lostBedCancellationTransformer: LostBedCancellationTransformer,
) {
  fun transformJpaToApi(jpa: LostBedsEntity) = when (jpa) {
    is ApprovedPremisesLostBedsEntity -> ApprovedPremisesLostBed(
      id = jpa.id,
      startDate = jpa.startDate,
      endDate = jpa.endDate,
      numberOfBeds = jpa.numberOfBeds,
      reason = lostBedReasonTransformer.transformJpaToApi(jpa.reason),
      referenceNumber = jpa.referenceNumber,
      notes = jpa.notes,
      status = determineStatus(jpa),
      cancellation = jpa.cancellation?.let { lostBedCancellationTransformer.transformJpaToApi(it) },
    )
    is TemporaryAccommodationLostBedEntity -> TemporaryAccommodationLostBed(
      id = jpa.id,
      startDate = jpa.startDate,
      endDate = jpa.endDate,
      reason = lostBedReasonTransformer.transformJpaToApi(jpa.reason),
      referenceNumber = jpa.referenceNumber,
      bedId = jpa.bed.id,
      notes = jpa.notes,
      status = determineStatus(jpa),
      cancellation = jpa.cancellation?.let { lostBedCancellationTransformer.transformJpaToApi(it) },
    )
    else -> throw RuntimeException("Unsupported LostBedsEntity type: ${jpa::class.qualifiedName}")
  }

  private fun determineStatus(jpa: LostBedsEntity) = when {
    jpa.cancellation != null -> LostBedStatus.cancelled
    else -> LostBedStatus.active
  }
}

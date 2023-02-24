package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesLostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationLostBedEntity

@Component
class LostBedsTransformer(private val lostBedReasonTransformer: LostBedReasonTransformer) {
  fun transformJpaToApi(jpa: LostBedsEntity) = when (jpa) {
    is ApprovedPremisesLostBedsEntity -> ApprovedPremisesLostBed(
      id = jpa.id,
      startDate = jpa.startDate,
      endDate = jpa.endDate,
      numberOfBeds = jpa.numberOfBeds,
      reason = lostBedReasonTransformer.transformJpaToApi(jpa.reason),
      referenceNumber = jpa.referenceNumber,
      notes = jpa.notes,
    )
    is TemporaryAccommodationLostBedEntity -> TemporaryAccommodationLostBed(
      id = jpa.id,
      startDate = jpa.startDate,
      endDate = jpa.endDate,
      reason = lostBedReasonTransformer.transformJpaToApi(jpa.reason),
      referenceNumber = jpa.referenceNumber,
      bedId = jpa.bed.id,
      notes = jpa.notes,
    )
    else -> throw RuntimeException("Unsupported LostBedsEntity type: ${jpa::class.qualifiedName}")
  }
}

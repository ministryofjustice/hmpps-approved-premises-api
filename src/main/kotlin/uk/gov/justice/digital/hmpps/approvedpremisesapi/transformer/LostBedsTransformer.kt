package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity

@Component
class LostBedsTransformer(private val lostBedReasonTransformer: LostBedReasonTransformer) {
  fun transformJpaToApi(jpa: LostBedsEntity) = LostBed(
    id = jpa.id,
    startDate = jpa.startDate,
    endDate = jpa.endDate,
    numberOfBeds = jpa.numberOfBeds,
    reason = lostBedReasonTransformer.transformJpaToApi(jpa.reason),
    referenceNumber = jpa.referenceNumber,
    notes = jpa.notes
  )
}

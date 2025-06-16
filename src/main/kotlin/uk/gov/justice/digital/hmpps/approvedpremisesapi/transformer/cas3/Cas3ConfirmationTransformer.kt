package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2ConfirmationEntity

@Component
class Cas3ConfirmationTransformer {
  fun transformJpaToApi(jpa: Cas3ConfirmationEntity?): Confirmation? = jpa?.let {
    Confirmation(
      id = it.id,
      bookingId = it.booking.id,
      dateTime = it.dateTime.toInstant(),
      notes = it.notes,
      createdAt = jpa.createdAt.toInstant(),
    )
  }

  fun transformJpaToApi(jpa: Cas3v2ConfirmationEntity?): Confirmation? = jpa?.let {
    Confirmation(
      id = it.id,
      bookingId = it.booking.id,
      dateTime = it.dateTime.toInstant(),
      notes = it.notes,
      createdAt = jpa.createdAt.toInstant(),
    )
  }
}

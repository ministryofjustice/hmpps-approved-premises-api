package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Confirmation

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

  fun transformJpaToApi(jpa: Cas3v2ConfirmationEntity?) = jpa?.let {
    Cas3Confirmation(
      id = it.id,
      bookingId = it.booking.id,
      dateTime = it.dateTime.toInstant(),
      notes = it.notes,
      createdAt = jpa.createdAt.toInstant(),
    )
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity

@Component
class ConfirmationTransformer {
  fun transformJpaToApi(jpa: ConfirmationEntity?): Confirmation? = jpa?.let {
    Confirmation(
      id = it.id,
      bookingId = it.booking.id,
      dateTime = it.dateTime,
      notes = it.notes,
      createdAt = jpa.createdAt,
    )
  }
}

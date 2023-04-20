package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementRequestBookingConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity

@Component
class NewPlacementRequestBookingConfirmationTransformer {
  fun transformJpaToApi(jpa: BookingEntity) = NewPlacementRequestBookingConfirmation(
    arrivalDate = jpa.arrivalDate,
    departureDate = jpa.departureDate,
    premisesName = jpa.bed!!.room.premises.name,
  )
}

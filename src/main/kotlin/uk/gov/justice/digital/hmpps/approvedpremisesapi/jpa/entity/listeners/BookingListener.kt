package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus

@Component
class BookingListener {
  fun prePersist(booking: BookingEntity) {
    if (booking.application != null && booking.application is ApprovedPremisesApplicationEntity) {
      (booking.application as ApprovedPremisesApplicationEntity).status = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED
    }
  }
}

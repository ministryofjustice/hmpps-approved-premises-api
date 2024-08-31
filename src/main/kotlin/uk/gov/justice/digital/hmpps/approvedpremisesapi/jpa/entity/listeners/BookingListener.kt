package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity.Companion.isCas1
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus

@Component
class BookingListener {
  fun prePersist(booking: BookingEntity) {
    val application = booking.application
    if (application != null && isCas1(application)) {
      application.status = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED
    }
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus

@Service
class Cas1ApplicationStatusService(
  val applicationRepository: ApplicationRepository,
) {

  fun bookingMade(booking: BookingEntity) {
    val application = booking.application
    if (application != null) {
      (application as ApprovedPremisesApplicationEntity).status = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED
      applicationRepository.save(application)
    }
  }
}

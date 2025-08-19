package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus

@Service
class Cas1ApplicationStatusService(
  val applicationRepository: ApplicationRepository,
  val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
) {

  fun unsubmittedApplicationUpdated(application: ApprovedPremisesApplicationEntity) {
    application.status = if (application.isInapplicable == true) {
      ApprovedPremisesApplicationStatus.INAPPLICABLE
    } else {
      ApprovedPremisesApplicationStatus.STARTED
    }
    applicationRepository.save(application)
  }

  fun applicationWithdrawn(application: ApprovedPremisesApplicationEntity) {
    application.status = ApprovedPremisesApplicationStatus.WITHDRAWN
    applicationRepository.save(application)
  }

  fun assessmentCreated(assessment: ApprovedPremisesAssessmentEntity) {
    if (assessment.allocatedToUser == null) {
      (assessment.application as ApprovedPremisesApplicationEntity).status =
        ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT
    } else {
      (assessment.application as ApprovedPremisesApplicationEntity).status =
        ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT
    }
  }

  fun assessmentUpdated(assessment: ApprovedPremisesAssessmentEntity) {
    val application = assessment.application as ApprovedPremisesApplicationEntity

    if (application.status == ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION && assessment.decision == null) {
      return
    } else if (assessment.isWithdrawn) {
      return
    } else if (assessment.decision == null && assessment.data != null) {
      application.status = ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS
    } else if (assessment.decision == AssessmentDecision.ACCEPTED) {
      application.status = applicationGetStatusFromArrivalDate(application)
    } else if (assessment.decision == AssessmentDecision.REJECTED) {
      application.status = ApprovedPremisesApplicationStatus.REJECTED
    }
  }

  private fun applicationGetStatusFromArrivalDate(
    application: ApprovedPremisesApplicationEntity,
  ): ApprovedPremisesApplicationStatus {
    val releaseDate = application.arrivalDate
    if (releaseDate == null) {
      return ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST
    }
    return ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT
  }

  fun spaceBookingMade(spaceBooking: Cas1SpaceBookingEntity) {
    bookingMade(spaceBooking.application!!)
  }

  fun spaceBookingCancelled(spaceBooking: Cas1SpaceBookingEntity, isUserRequestedWithdrawal: Boolean = true) {
    if (!isUserRequestedWithdrawal || spaceBooking.application == null) {
      return
    }
    val application = spaceBooking.application!!
    val spaceBookingsForApplication = cas1SpaceBookingRepository.findAllByApplication(application)
    val anyActiveBookings = spaceBookingsForApplication.any { it.isActive() }
    if (!anyActiveBookings) {
      lastBookingCancelled(spaceBooking.application!!)
    }
  }

  private fun lastBookingCancelled(application: ApprovedPremisesApplicationEntity) {
    application.status = ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT
    applicationRepository.save(application)
  }

  private fun bookingMade(application: ApprovedPremisesApplicationEntity) {
    application.status = ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED
    applicationRepository.save(application)
  }
}

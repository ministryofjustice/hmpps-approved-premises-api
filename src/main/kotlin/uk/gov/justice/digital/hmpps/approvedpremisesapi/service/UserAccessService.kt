package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import java.util.UUID

@Service
class UserAccessService(
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val requestContextService: RequestContextService,
) {
  fun currentUserCanAccessRegion(service: ServiceName, probationRegionId: UUID?) = userCanAccessRegion(userService.getUserForRequest(), service, probationRegionId)

  fun userCanAccessRegion(user: UserEntity, service: ServiceName, probationRegionId: UUID?) = userHasAllRegionsAccess(user, service) || user.probationRegion.id == probationRegionId

  fun currentUserHasAllRegionsAccess(service: ServiceName) = userHasAllRegionsAccess(userService.getUserForRequest(), service)

  fun userHasAllRegionsAccess(user: UserEntity, service: ServiceName) = when (service) {
    // TODO: Revisit once Temporary Accommodation introduces user roles
    ServiceName.temporaryAccommodation -> user.hasRole(CAS3_REPORTER)
    // TODO: Revisit if Approved Premises introduces region-limited access
    else -> true
  }

  fun currentUserCanViewPremises(premises: PremisesEntity) = userCanViewPremises(userService.getUserForRequest(), premises)

  fun userCanViewPremises(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, ServiceName.temporaryAccommodation, premises.probationRegion.id) && user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  fun currentUserCanManagePremises(premises: PremisesEntity) = userCanManagePremises(userService.getUserForRequest(), premises)

  fun userCanManagePremises(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, ServiceName.temporaryAccommodation, premises.probationRegion.id) && user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  fun currentUserCanChangeBookingDate(premises: PremisesEntity) = when (premises) {
    is TemporaryAccommodationPremisesEntity -> currentUserCanManageCas3PremisesBookings(premises)
    else -> false
  }

  fun currentUserCanManageCas3PremisesBookings(premises: PremisesEntity) = userCanManageCas3PremisesBookings(userService.getUserForRequest(), premises)

  fun userCanViewBooking(user: UserEntity, booking: BookingEntity) = when (booking.premises) {
    is TemporaryAccommodationPremisesEntity -> userCanManageCas3PremisesBookings(user, booking.premises)
    else -> false
  }

  fun userCanManageCas3PremisesBookings(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, ServiceName.temporaryAccommodation, premises.probationRegion.id) && user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  fun userCanManagePremisesBookings(user: UserEntity, premises: Cas3PremisesEntity) = userCanAccessRegion(user, ServiceName.temporaryAccommodation, premises.probationDeliveryUnit.probationRegion.id) &&
    user.hasRole(UserRole.CAS3_ASSESSOR)

  /**
   * This function only checks if the user has the correct permissions to cancel the given booking.
   *
   * It doesn't consider if the booking is in a cancellable state
   */
  fun userMayCancelBooking(user: UserEntity, booking: BookingEntity) = when (booking.premises) {
    is TemporaryAccommodationPremisesEntity -> userCanManageCas3PremisesBookings(user, booking.premises)
    else -> false
  }

  @Deprecated("Use CAS3UserAccessService.canViewVoidBedspaces")
  fun currentUserCanManagePremisesVoidBedspaces(premises: PremisesEntity) = userCanManagePremisesVoidBedspaces(userService.getUserForRequest(), premises)

  @Deprecated("Use CAS3UserAccessService.canViewVoidBedspaces")
  fun userCanManagePremisesVoidBedspaces(user: UserEntity, premises: PremisesEntity) = when (premises) {
    is TemporaryAccommodationPremisesEntity -> userCanAccessRegion(user, ServiceName.temporaryAccommodation, premises.probationRegion.id) && user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  fun currentUserCanViewReport() = userCanViewReport(userService.getUserForRequest())

  fun userCanViewReport(user: UserEntity) = when (requestContextService.getServiceForRequest()) {
    ServiceName.temporaryAccommodation -> user.hasAnyRole(UserRole.CAS3_ASSESSOR, CAS3_REPORTER)
    else -> false
  }

  fun userCanViewApplication(user: UserEntity, application: ApplicationEntity): Boolean {
    if (user.id == application.createdByUser.id) {
      return true
    }

    return when (application) {
      is ApprovedPremisesApplicationEntity -> userCanViewApprovedPremisesApplicationCreatedBySomeoneElse(user, application)
      is TemporaryAccommodationApplicationEntity -> userCanViewTemporaryAccommodationApplicationCreatedBySomeoneElse(user, application)
      else -> false
    }
  }

  private fun userCanViewApprovedPremisesApplicationCreatedBySomeoneElse(
    user: UserEntity,
    application: ApprovedPremisesApplicationEntity,
  ) = offenderService.canAccessOffender(application.crn, user.cas1LaoStrategy())

  private fun userCanViewTemporaryAccommodationApplicationCreatedBySomeoneElse(
    user: UserEntity,
    application: TemporaryAccommodationApplicationEntity,
  ): Boolean = userCanAccessRegion(user, ServiceName.temporaryAccommodation, application.probationRegion.id) &&
    user.hasRole(UserRole.CAS3_ASSESSOR) &&
    application.submittedAt != null

  fun userCanReallocateTask(user: UserEntity): Boolean = when (requestContextService.getServiceForRequest()) {
    ServiceName.temporaryAccommodation -> user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> true
  }

  fun userCanDeallocateTask(user: UserEntity): Boolean = when (requestContextService.getServiceForRequest()) {
    ServiceName.temporaryAccommodation -> user.hasRole(UserRole.CAS3_ASSESSOR)
    else -> false
  }

  fun userCanViewAssessment(user: UserEntity, assessment: AssessmentEntity): Boolean = when (assessment) {
    is ApprovedPremisesAssessmentEntity ->
      true

    is TemporaryAccommodationAssessmentEntity ->
      user.hasRole(UserRole.CAS3_ASSESSOR) &&
        userCanAccessRegion(user, ServiceName.temporaryAccommodation, (assessment.application as TemporaryAccommodationApplicationEntity).probationRegion.id)

    else -> false
  }

  fun userCanAccessTemporaryAccommodationApplication(user: UserEntity, application: ApplicationEntity): Boolean = (user == application.createdByUser) &&
    userCanAccessRegion(user, ServiceName.temporaryAccommodation, (application as TemporaryAccommodationApplicationEntity).probationRegion.id) &&
    user.hasRole(UserRole.CAS3_REFERRER)
}

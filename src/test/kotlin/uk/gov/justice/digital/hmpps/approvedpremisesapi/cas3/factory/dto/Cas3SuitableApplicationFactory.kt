package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.dto

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ExternalCurrentApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ExternalSubmittedApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3StaffDto
import java.util.UUID

class Cas3SuitableApplicationFactory : Factory<Cas3ExternalCurrentApplicationDto> {

  private var applicationStatus: Yielded<ApplicationStatus> = { ApplicationStatus.rejected }
  private var submittedApplication: Yielded<Cas3ExternalSubmittedApplicationDto?> = { null }

  fun withApplicationStatus(status: ApplicationStatus) = apply {
    this.applicationStatus = { status }
  }

  fun withSubmittedApplication(application: Cas3ExternalSubmittedApplicationDto?) = apply {
    this.submittedApplication = { application }
  }

  override fun produce(): Cas3ExternalCurrentApplicationDto = Cas3ExternalCurrentApplicationDto(
    id = UUID.randomUUID(),
    applicationStatus = applicationStatus(),
    uiUrl = "localhost",
    submittedApplication = submittedApplication?.let { it() },
    applicationSubmittedDate = null,
    applicationSubmittedBy = Cas3StaffDto(name = "name", username = "username", staffCode = "code"),
    applicationRejectedReason = null,
    assessmentStatus = null,
    bookingStatus = null,
    bookingProvisionalOfferSentDate = null,
    previousBookings = null,
    premises = null,
  )
}

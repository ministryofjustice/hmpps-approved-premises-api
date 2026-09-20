package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.dto

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ExternalSubmittedApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import java.time.LocalDate

class Cas3ExternalSubmittedApplicationDtoFactory : Factory<Cas3ExternalSubmittedApplicationDto> {

  private var assessmentStatus: Yielded<TemporaryAccommodationAssessmentStatus?> = { null }
  private var assessmentRejectionReason: Yielded<String?> = { null }

  fun withAssessmentStatus(status: TemporaryAccommodationAssessmentStatus) = apply {
    this.assessmentStatus = { status }
  }

  fun withAssessmentRejectionReason(reason: String?) = apply {
    this.assessmentRejectionReason = { reason }
  }

  override fun produce(): Cas3ExternalSubmittedApplicationDto = Cas3ExternalSubmittedApplicationDto(
    submittedDate = LocalDate.now(),
    submittedBy = Cas3StaffDto(name = "name", username = "username", staffCode = "code"),
    assessmentStatus = assessmentStatus(),
    assessmentRejectionReason = assessmentRejectionReason(),
    latestBooking = null,
  )
}

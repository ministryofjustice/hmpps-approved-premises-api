package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ExternalLatestBookingDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ExternalPreviousBookingCancellationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ExternalPreviousBookingDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer

@Component
class Cas3ApplicationTransformer(
  private val jsonMapper: JsonMapper,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  @Value($$"${url-templates.frontend.cas3.referral-full}") private val cas3ApplicationFullUrlTemplate: String,
) {
  fun transformToCas3SuitableApplication(application: TemporaryAccommodationApplicationEntity, bookings: List<Cas3BookingEntity>?): Cas3SuitableApplication {
    val latestBooking = bookings?.firstOrNull()
    val latestAssessment = application.getLatestAssessment()

    return Cas3SuitableApplication(
      id = application.id,
      applicationStatus = application.getStatus(),
      applicationSubmittedDate = application.submittedAt?.toLocalDate(),
      applicationSubmittedBy = transformToStaffDto(application.createdByUser),
      applicationRejectedReason = latestAssessment?.referralRejectionReason?.name,
      assessmentStatus = latestAssessment?.deriveAssessmentStatus(),
      bookingStatus = latestBooking?.status,
      bookingProvisionalOfferSentDate = if (latestBooking?.status == Cas3BookingStatus.provisional) latestBooking.createdAt.toLocalDate() else null,
      previousBookings = bookings?.drop(1)?.map { transformToPreviousBookingDto(it) },
      premises = latestBooking?.premises?.let {
        transformToCas3PremisesSummary(latestBooking)
      },
      uiUrl = cas3ApplicationFullUrlTemplate.replace("#applicationId", application.id.toString()),
    )
  }

  fun transformToCas3PremisesSummary(booking: Cas3BookingEntity) = Cas3ExternalLatestBookingDto(
    startDate = booking.arrivalDate,
    endDate = booking.departureDate,
    name = booking.premises.name,
    addressLine1 = booking.premises.addressLine1,
    addressLine2 = booking.premises.addressLine2,
    town = booking.premises.town,
    postcode = booking.premises.postcode,
  )

  fun transformJpaToApi(applicationEntity: TemporaryAccommodationApplicationEntity, personInfo: PersonInfoResult): Cas3Application {
    val latestAssessment = applicationEntity.getLatestAssessment()

    return Cas3Application(
      id = applicationEntity.id,
      person = personTransformer.personInfoResultToPerson(personInfo),
      createdByUserId = applicationEntity.createdByUser.id,
      createdAt = applicationEntity.createdAt.toInstant(),
      submittedAt = applicationEntity.submittedAt?.toInstant(),
      arrivalDate = applicationEntity.arrivalDate?.toInstant(),
      data = if (applicationEntity.data != null) jsonMapper.readTree(applicationEntity.data) else null,
      document = if (applicationEntity.document != null) jsonMapper.readTree(applicationEntity.document) else null,
      risks = if (applicationEntity.riskRatings != null) {
        risksTransformer.transformDomainToApi(
          applicationEntity.riskRatings!!,
          applicationEntity.crn,
        )
      } else {
        null
      },
      status = applicationEntity.getStatus(),
      offenceId = applicationEntity.offenceId,
      assessmentId = latestAssessment?.id,
      assessmentDecision = transformJpaDecisionToApi(latestAssessment?.decision),
    )
  }

  fun transformDomainToCas3ApplicationSummary(
    domain: ApplicationSummary,
    personSummaryInfo: PersonSummaryInfoResult,
  ): Cas3ApplicationSummary {
    val riskRatings =
      if (domain.getRiskRatings() != null) jsonMapper.readValue<PersonRisks>(domain.getRiskRatings()!!) else null

    return Cas3ApplicationSummary(
      id = domain.getId(),
      personSummary = personTransformer.personSummaryInfoResultToPersonSummary(personSummaryInfo),
      createdByUserId = domain.getCreatedByUserId(),
      createdAt = domain.getCreatedAt(),
      submittedAt = domain.getSubmittedAt(),
      risks = if (riskRatings != null) risksTransformer.transformDomainToApi(riskRatings, domain.getCrn()) else null,
      status = getStatusFromSummary(domain as TemporaryAccommodationApplicationSummary),
    )
  }

  private fun getStatusFromSummary(entity: TemporaryAccommodationApplicationSummary): ApplicationStatus = when {
    entity.getLatestAssessmentHasClarificationNotesWithoutResponse() -> ApplicationStatus.requestedFurtherInformation
    entity.getLatestAssessmentDecision() == AssessmentDecision.REJECTED -> ApplicationStatus.rejected
    entity.getSubmittedAt() !== null -> ApplicationStatus.submitted
    else -> ApplicationStatus.inProgress
  }

  fun transformJpaDecisionToApi(decision: AssessmentDecision?) = when (decision) {
    AssessmentDecision.ACCEPTED -> uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision.accepted
    AssessmentDecision.REJECTED -> uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision.rejected
    null -> null
  }

  private fun transformToPreviousBookingDto(bookingEntity: Cas3BookingEntity): Cas3ExternalPreviousBookingDto = Cas3ExternalPreviousBookingDto(
    bookingStatus = bookingEntity.status,
    cancellation = if (bookingEntity.isCancelled) {
      Cas3ExternalPreviousBookingCancellationDto(
        cancellationDate = bookingEntity.cancellation?.createdAt?.toLocalDate(),
        cancellationReason = bookingEntity.cancellation?.reason?.name,
      )
    } else {
      null
    },
  )

  private fun transformToStaffDto(user: UserEntity) = Cas3StaffDto(user.name, user.deliusUsername, user.deliusStaffCode)
}

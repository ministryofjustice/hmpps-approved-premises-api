package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer

import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3AssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision as ApiAssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision as JpaAssessmentDecision

@Component
class Cas3AssessmentTransformer(
  private val applicationsTransformer: ApplicationsTransformer,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer,
  private val userTransformer: UserTransformer,
  private val jsonMapper: JsonMapper,
  private val bookingRepository: BookingRepository,
) {
  fun transformJpaToApi(
    jpa: TemporaryAccommodationAssessmentEntity,
    personInfo: PersonInfoResult,
  ) = Cas3Assessment(
    id = jpa.id,
    application = applicationsTransformer.transformJpaToApi(jpa.application, personInfo) as TemporaryAccommodationApplication,
    createdAt = jpa.createdAt.toInstant(),
    allocatedAt = jpa.allocatedAt?.toInstant(),
    data = if (jpa.data != null) jsonMapper.readTree(jpa.data) else null,
    clarificationNotes = jpa.clarificationNotes.map(assessmentClarificationNoteTransformer::transformJpaToApi),
    allocatedToStaffMember = jpa.allocatedToUser?.let {
      userTransformer.transformJpaToApi(
        it,
        ServiceName.temporaryAccommodation,
      ) as TemporaryAccommodationUser
    },
    submittedAt = jpa.submittedAt?.toInstant(),
    decision = transformJpaDecisionToApi(jpa.decision),
    rejectionRationale = jpa.rejectionRationale,
    status = jpa.deriveAssessmentStatus(),
    summaryData = jsonMapper.readTree(jpa.summaryData),
    releaseDate = jpa.releaseDate ?: jpa.typedApplication<TemporaryAccommodationApplicationEntity>().personReleaseDate,
    accommodationRequiredFromDate = jpa.accommodationRequiredFromDate ?: LocalDate.from(jpa.typedApplication<TemporaryAccommodationApplicationEntity>().arrivalDate),
  )

  fun transformDomainToApiSummary(ase: DomainAssessmentSummary, personInfo: PersonInfoResult): Cas3AssessmentSummary = Cas3AssessmentSummary(
    id = ase.id,
    applicationId = ase.applicationId,
    createdAt = ase.createdAt,
    arrivalDate = ase.arrivalDate,
    status = getAssessmentStatus(ase),
    decision = transformDomainSummaryDecisionToApi(ase.decision),
    risks = ase.riskRatings?.let {
      risksTransformer.transformDomainToApi(
        jsonMapper.readValue<PersonRisks>(it),
        ase.crn,
      )
    },
    person = personTransformer.transformModelToPersonApi(personInfo),
    probationDeliveryUnitName = ase.probationDeliveryUnitName,
  )

  fun transformAssessmentToCas3ReferralHistory(a: TemporaryAccommodationAssessmentEntity): List<Cas3ReferralHistory> {
    val application = a.typedApplication<TemporaryAccommodationApplicationEntity>()

    val bookings = bookingRepository.findAllCas3BookingEntity(application.id, ServiceName.temporaryAccommodation.value)

    val referralHistory = Cas3ReferralHistory(
      id = a.id,
      applicationId = application.id,
      createdAt = a.createdAt.toInstant(),
      applicationStatus = application.getStatus(),
      type = ServiceType.CAS3,
      referralRejectionReason = a.referralRejectionReason?.name ?: a.rejectionRationale,
      referralRejectionReasonDetail = a.referralRejectionReasonDetail,
      localAuthorityArea = application.dutyToReferLocalAuthorityAreaName,
      pdu = application.probationDeliveryUnit?.name,
      referredBy = transformToStaffDto(application.createdByUser),
      placementAddress = null,
      bookingStatus = null,
    )

    return bookings.map {
      referralHistory.copy(
        placementAddress = listOfNotNull(it.premises.addressLine1, it.premises.town, it.premises.postcode).joinToString(", "),
        bookingStatus = it.status,
      )
    }.ifEmpty { listOf(referralHistory) }
  }

  fun transformApiStatusToDomainSummaryState(status: AssessmentStatus) = when (status) {
    AssessmentStatus.cas1Completed -> DomainAssessmentSummaryStatus.COMPLETED
    AssessmentStatus.cas1AwaitingResponse -> DomainAssessmentSummaryStatus.AWAITING_RESPONSE
    AssessmentStatus.cas1InProgress -> DomainAssessmentSummaryStatus.IN_PROGRESS
    AssessmentStatus.cas1NotStarted -> DomainAssessmentSummaryStatus.NOT_STARTED
    AssessmentStatus.cas1Reallocated -> DomainAssessmentSummaryStatus.REALLOCATED
    AssessmentStatus.cas3InReview -> DomainAssessmentSummaryStatus.IN_REVIEW
    AssessmentStatus.cas3Unallocated -> DomainAssessmentSummaryStatus.UNALLOCATED
    AssessmentStatus.cas3Rejected -> DomainAssessmentSummaryStatus.REJECTED
    AssessmentStatus.cas3Closed -> DomainAssessmentSummaryStatus.CLOSED
    AssessmentStatus.cas3ReadyToPlace -> DomainAssessmentSummaryStatus.READY_TO_PLACE
  }

  private fun getAssessmentStatus(ase: DomainAssessmentSummary) = when {
    ase.decision == "REJECTED" -> TemporaryAccommodationAssessmentStatus.rejected
    ase.decision == "ACCEPTED" && ase.completed -> TemporaryAccommodationAssessmentStatus.closed
    ase.decision == "ACCEPTED" -> TemporaryAccommodationAssessmentStatus.readyToPlace
    ase.allocated -> TemporaryAccommodationAssessmentStatus.inReview
    else -> TemporaryAccommodationAssessmentStatus.unallocated
  }

  private fun transformDomainSummaryDecisionToApi(decision: String?) = when (decision) {
    "ACCEPTED" -> ApiAssessmentDecision.accepted
    "REJECTED" -> ApiAssessmentDecision.rejected
    else -> null
  }

  fun transformJpaDecisionToApi(decision: JpaAssessmentDecision?) = when (decision) {
    JpaAssessmentDecision.ACCEPTED -> ApiAssessmentDecision.accepted
    JpaAssessmentDecision.REJECTED -> ApiAssessmentDecision.rejected
    null -> null
  }

  private fun transformToStaffDto(user: UserEntity) = Cas3StaffDto(user.name, user.deliusUsername, user.deliusStaffCode)
}

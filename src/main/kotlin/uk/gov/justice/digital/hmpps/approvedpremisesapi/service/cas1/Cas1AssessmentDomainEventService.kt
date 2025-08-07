package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedAssessedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequested
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1AssessmentDomainEventService(
  private val domainEventService: Cas1DomainEventService,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val clock: Clock,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.assessment}") private val assessmentUrlTemplate: UrlTemplate,
) {

  fun assessmentAllocated(assessment: AssessmentEntity, allocatedToUser: UserEntity, allocatingUser: UserEntity?) {
    val allocatedToStaffDetails = when (val result = apDeliusContextApiClient.getStaffDetail(allocatedToUser.deliusUsername)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }

    val allocatingUserStaffDetails = allocatingUser?.let {
      when (val result = apDeliusContextApiClient.getStaffDetail(allocatingUser.deliusUsername)) {
        is ClientResult.Success -> result.body
        is ClientResult.Failure -> result.throwException()
      }
    }

    val triggerSource = if (allocatingUser == null) {
      TriggerSourceType.SYSTEM
    } else {
      TriggerSourceType.USER
    }

    val id = UUID.randomUUID()
    val occurredAt = Instant.now()

    domainEventService.saveAssessmentAllocatedEvent(
      SaveCas1DomainEvent(
        id = id,
        applicationId = assessment.application.id,
        assessmentId = assessment.id,
        crn = assessment.application.crn,
        nomsNumber = assessment.application.nomsNumber,
        occurredAt = occurredAt,
        triggerSource = triggerSource,
        data = AssessmentAllocatedEnvelope(
          id = id,
          timestamp = occurredAt,
          eventType = EventType.assessmentAllocated,
          eventDetails = AssessmentAllocated(
            assessmentId = assessment.id,
            assessmentUrl = assessmentUrlTemplate.resolve("id", assessment.id.toString()),
            applicationId = assessment.application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", assessment.application.id.toString()),
            allocatedAt = Instant.now(),
            personReference = PersonReference(
              crn = assessment.application.crn,
              noms = assessment.application.nomsNumber ?: "Unknown NOMS Number",
            ),
            allocatedTo = allocatedToStaffDetails.toStaffMember(),
            allocatedBy = allocatingUserStaffDetails?.let {
              allocatingUserStaffDetails.toStaffMember()
            },
          ),
        ),
      ),
    )
  }

  fun furtherInformationRequested(assessment: AssessmentEntity, clarificationNoteEntity: AssessmentClarificationNoteEntity) {
    val requesterStaffDetails = when (val result = apDeliusContextApiClient.getStaffDetail(clarificationNoteEntity.createdByUser.deliusUsername)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }

    val recipientStaffDetails = when (val result = apDeliusContextApiClient.getStaffDetail(assessment.application.createdByUser.deliusUsername)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }

    val id = UUID.randomUUID()
    val occurredAt = clarificationNoteEntity.createdAt.toInstant()

    val data = FurtherInformationRequestedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = EventType.informationRequestMade,
      eventDetails = FurtherInformationRequested(
        assessmentId = assessment.id,
        assessmentUrl = assessmentUrlTemplate.resolve("id", assessment.id.toString()),
        applicationId = assessment.application.id,
        applicationUrl = applicationUrlTemplate.resolve("id", assessment.application.id.toString()),
        personReference = PersonReference(
          crn = assessment.application.crn,
          noms = assessment.application.nomsNumber ?: "Unknown NOMS Number",
        ),
        requestedAt = Instant.now(),
        requester = requesterStaffDetails.toStaffMember(),
        recipient = recipientStaffDetails.toStaffMember(),
        requestId = clarificationNoteEntity.id,
      ),
    )

    val domainEvent = SaveCas1DomainEvent(
      id = id,
      applicationId = assessment.application.id,
      assessmentId = assessment.id,
      crn = assessment.application.crn,
      nomsNumber = assessment.application.nomsNumber,
      occurredAt = occurredAt,
      data = data,
    )

    domainEventService.saveFurtherInformationRequestedEvent(domainEvent)
  }

  fun assessmentAccepted(
    application: ApprovedPremisesApplicationEntity,
    assessment: AssessmentEntity,
    offenderDetails: OffenderDetailSummary,
    placementDates: PlacementDates?,
    apType: ApType?,
    acceptingUser: UserEntity,
  ) {
    val domainEventId = UUID.randomUUID()
    val acceptedAt = assessment.submittedAt!!

    val staffDetails = when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(acceptingUser.deliusUsername)) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    domainEventService.saveApplicationAssessedDomainEvent(
      SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        assessmentId = assessment.id,
        crn = application.crn,
        nomsNumber = offenderDetails.otherIds.nomsNumber,
        occurredAt = acceptedAt.toInstant(),
        data = ApplicationAssessedEnvelope(
          id = domainEventId,
          timestamp = acceptedAt.toInstant(),
          eventType = EventType.applicationAssessed,
          eventDetails = ApplicationAssessed(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate
              .resolve("id", application.id.toString()),
            assessmentId = assessment.id,
            personReference = PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
            assessedAt = acceptedAt.toInstant(),
            assessedBy = ApplicationAssessedAssessedBy(
              staffMember = staffDetails.toStaffMember(),
              probationArea = ProbationArea(
                code = staffDetails.probationArea.code,
                name = staffDetails.probationArea.description,
              ),
              cru = Cru(
                name = acceptingUser.apArea?.name ?: "Unknown CRU",
              ),
            ),
            decision = assessment.decision.toString(),
            decisionRationale = assessment.rejectionRationale,
            arrivalDate = placementDates?.expectedArrival?.toLocalDateTime()?.toInstant(),
          ),
        ),
        metadata = mapOf(
          MetaDataName.CAS1_REQUESTED_AP_TYPE to apType?.asApprovedPremisesType()?.name,
        ),
        schemaVersion = 2,
      ),
    )
  }

  fun assessmentRejected(
    application: ApprovedPremisesApplicationEntity,
    assessment: AssessmentEntity,
    offenderDetails: OffenderDetailSummary,
    rejectingUser: UserEntity,
  ) {
    val domainEventId = UUID.randomUUID()
    val rejectedAt = OffsetDateTime.now(clock)

    val staffDetails = when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(rejectingUser.deliusUsername)) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    domainEventService.saveApplicationAssessedDomainEvent(
      SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        assessmentId = assessment.id,
        crn = application.crn,
        nomsNumber = offenderDetails.otherIds.nomsNumber,
        occurredAt = rejectedAt.toInstant(),
        data = ApplicationAssessedEnvelope(
          id = domainEventId,
          timestamp = rejectedAt.toInstant(),
          eventType = EventType.applicationAssessed,
          eventDetails = ApplicationAssessed(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate
              .resolve("id", application.id.toString()),
            assessmentId = assessment.id,
            personReference = PersonReference(
              crn = assessment.application.crn,
              noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
            assessedAt = rejectedAt.toInstant(),
            assessedBy = ApplicationAssessedAssessedBy(
              staffMember = staffDetails.toStaffMember(),
              probationArea = ProbationArea(
                code = staffDetails.probationArea.code,
                name = staffDetails.probationArea.description,
              ),
              cru = Cru(
                name = rejectingUser.apArea?.name ?: "Unknown CRU",
              ),
            ),
            decision = assessment.decision.toString(),
            decisionRationale = assessment.rejectionRationale,
            arrivalDate = null,
          ),
        ),
        schemaVersion = 2,
      ),
    )
  }
}

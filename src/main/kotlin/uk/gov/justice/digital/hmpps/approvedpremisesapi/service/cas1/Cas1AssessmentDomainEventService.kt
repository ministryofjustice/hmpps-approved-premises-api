package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.Instant
import java.util.UUID

@Service
class Cas1AssessmentDomainEventService(
  private val domainEventService: DomainEventService,
  private val communityApiClient: CommunityApiClient,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.assessment}") private val assessmentUrlTemplate: UrlTemplate,
) {

  fun assessmentAllocated(assessment: AssessmentEntity, allocatedToUser: UserEntity?, actingUser: UserEntity) {

    val allocatedToStaffDetails = allocatedToUser?.let {
      when (val result = communityApiClient.getStaffUserDetails(allocatedToUser.deliusUsername)) {
        is ClientResult.Success -> result.body
        is ClientResult.Failure -> result.throwException()
      }
    }

    val actingUserStaffDetails = when (val result = communityApiClient.getStaffUserDetails(actingUser.deliusUsername)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }

    val id = UUID.randomUUID()
    val occurredAt = Instant.now()

    domainEventService.saveAssessmentAllocatedEvent(
      DomainEvent(
        id = id,
        applicationId = assessment.application.id,
        assessmentId = assessment.id,
        crn = assessment.application.crn,
        occurredAt = occurredAt,
        data = AssessmentAllocatedEnvelope(
          id = id,
          timestamp = occurredAt,
          eventType = "approved-premises.assessment.allocated",
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
            allocatedTo = allocatedToStaffDetails?.let {
              StaffMember(
                staffCode = allocatedToStaffDetails.staffCode,
                staffIdentifier = allocatedToStaffDetails.staffIdentifier,
                forenames = allocatedToStaffDetails.staff.forenames,
                surname = allocatedToStaffDetails.staff.surname,
                username = allocatedToStaffDetails.username,
              )
            },
            allocatedBy = StaffMember(
              staffCode = actingUserStaffDetails.staffCode,
              staffIdentifier = actingUserStaffDetails.staffIdentifier,
              forenames = actingUserStaffDetails.staff.forenames,
              surname = actingUserStaffDetails.staff.surname,
              username = actingUserStaffDetails.username,
            ),
          ),
        ),
      ),
    )
  }
}
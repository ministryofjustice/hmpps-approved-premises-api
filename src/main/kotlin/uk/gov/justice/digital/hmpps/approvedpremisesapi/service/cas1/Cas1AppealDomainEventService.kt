package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.util.UUID

@Service
class Cas1AppealDomainEventService(
  private val domainEventService: DomainEventService,
  private val communityApiClient: CommunityApiClient,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-appeal}") private val applicationAppealUrlTemplate: UrlTemplate,
) {

  fun appealRecordCreated(appeal: AppealEntity) {
    val id = UUID.randomUUID()
    val timestamp = appeal.createdAt.toInstant()

    val staffDetails = when (val result = communityApiClient.getStaffUserDetails(appeal.createdBy.deliusUsername)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }

    domainEventService.saveAssessmentAppealedEvent(
      DomainEvent(
        id = id,
        applicationId = appeal.application.id,
        assessmentId = null,
        bookingId = null,
        crn = appeal.application.crn,
        occurredAt = timestamp,
        data = AssessmentAppealedEnvelope(
          id = id,
          timestamp = timestamp,
          eventType = "approved-premises.assessment.appealed",
          eventDetails = AssessmentAppealed(
            applicationId = appeal.application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", appeal.application.id.toString()),
            appealId = appeal.id,
            appealUrl = applicationAppealUrlTemplate.resolve(
              mapOf("applicationId" to appeal.application.id.toString(), "appealId" to appeal.id.toString()),
            ),
            personReference = PersonReference(
              crn = appeal.application.crn,
              noms = appeal.application.nomsNumber!!,
            ),
            deliusEventNumber = (appeal.application as ApprovedPremisesApplicationEntity).eventNumber,
            createdAt = timestamp,
            createdBy = StaffMember(
              staffCode = staffDetails.staffCode,
              staffIdentifier = staffDetails.staffIdentifier,
              forenames = staffDetails.staff.forenames,
              surname = staffDetails.staff.surname,
              username = staffDetails.username,
            ),
            appealDetail = appeal.appealDetail,
            decision = parseDecision(appeal.decision),
            decisionDetail = appeal.decisionDetail,
          ),
        ),
      ),
    )
  }

  private fun parseDecision(value: String): AppealDecision =
    AppealDecision.entries.firstOrNull { it.value == value }
      ?: throw IllegalArgumentException("Unknown appeal decision type '$value'")
}

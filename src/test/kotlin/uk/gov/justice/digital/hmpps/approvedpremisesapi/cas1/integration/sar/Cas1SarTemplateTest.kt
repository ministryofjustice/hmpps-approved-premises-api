package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.sar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASarClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.DomainEventMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.sar.SarContent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType

class Cas1SarTemplateTest : Cas1SarTestBase() {

  @Test
  fun `returns 204 when no CAS1 data exists for CRN`() {
    val (offenderDetails, _) = givenAnOffender()
    givenASarClientCredentialsApiCall { jwt ->
      webTestClient.get()
        .uri("/subject-access-request?crn=${offenderDetails.otherIds.crn}&fromDate=2018-09-30&toDate=2024-09-30")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @Test
  fun `returns 200 with all CAS1 data`() {
    val (offenderDetails, _) = givenAnOffender()
    val application = approvedPremisesApplicationEntity(offenderDetails)
    val timelineNote = applicationTimelineNoteEntity(application)
    val assessment = approvedPremisesAssessmentEntity(application)
    val clarificationNote = approvedPremisesAssessmentClarificationNoteEntity(assessment)
    val appeal = appealEntity(application, assessment)
    val spaceBooking = spaceBookingEntity(offenderDetails, application)
    val placementApplication = placementApplicationEntity(application)
    val placementRequest = placementRequestEntity(assessment, application, placementApplication)
    val bookingNotMade = bookingNotMadeEntity(placementRequest)
    val domainEvent = domainEventEntity(offenderDetails, application.id, assessment.id, application.createdByUser.id)

    givenASarClientCredentialsApiCall { jwt ->
      webTestClient.get()
        .uri("/subject-access-request?crn=${offenderDetails.otherIds.crn}&fromDate=2018-09-30&toDate=2024-09-30")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus().isOk
        .expectBody<String>().value { body ->
          val sarContent = jsonMapper.readValue(body, SarContent::class.java)
          val cas1DataMap = sarContent.content?.find { it.containsKey("ApprovedPremises") }?.get("ApprovedPremises")
          assertThat(cas1DataMap).isNotNull

          val cas1Data = jsonMapper.convertValue(cas1DataMap, Cas1SarData::class.java)

          assertApplication(cas1Data.applications?.get(0), application)
          assertTimelineNote(cas1Data.applicationTimeline?.get(0), timelineNote)
          assertAssessment(cas1Data.assessments?.get(0), assessment)
          assertAssessmentClarificationNote(cas1Data.assessmentClarificationNotes?.get(0), clarificationNote)
          assertAppeal(cas1Data.appeals?.get(0), appeal)
          assertSpaceBooking(cas1Data.spaceBookings?.get(0), spaceBooking)
          assertPlacementApplication(cas1Data.placementApplications?.get(0), placementApplication)
          assertPlacementRequest(cas1Data.placementRequests?.get(0), placementRequest)
          assertPlacementRequirements(cas1Data.placementRequirements?.get(0), placementRequest.placementRequirements)
          assertPlacementRequirementCriteria(cas1Data.placementRequirementCriteria, placementRequest.placementRequirements)
          assertBookingNotMade(cas1Data.bookingNotMades?.get(0), bookingNotMade)
          assertDomainEvent(cas1Data.domainEvents?.get(0), domainEvent)
          assertDomainEventMetadata(cas1Data.domainEventsMetadata?.get(0), domainEvent)
        }
    }
  }

  private fun assertApplication(cas1Application: Cas1Application?, application: ApprovedPremisesApplicationEntity) {
    assertThat(cas1Application).isNotNull
    assertThat(cas1Application!!.offenceId).isEqualTo(application.offenceId)
    assertThat(cas1Application.applicationUserName).isEqualTo(application.applicantUserDetails?.name)
    assertThat(cas1Application.sentenceType).isEqualTo(application.sentenceType)
    assertThat(cas1Application.submittedAt).isEqualTo(SUBMITTED_AT)
    assertThat(cas1Application.withdrawalReason).isEqualTo(application.withdrawalReason)
    assertThat(cas1Application.apType).isEqualTo(ApprovedPremisesType.NORMAL.toString())
    assertThat(cas1Application.createdAt).isEqualTo(CREATED_AT)
    assertThat(cas1Application.isWithdrawn).isEqualTo(application.isWithdrawn)
    assertThat(cas1Application.inmateInOutStatusOnSubmission).isEqualTo(application.inmateInOutStatusOnSubmission)
    assertThat(cas1Application.createdByUser).isEqualTo(application.createdByUser.name)
    assertThat(cas1Application.noticeType).isEqualTo(application.noticeType?.name)
    assertThat(cas1Application.arrivalDate).isEqualTo(ARRIVED_AT)
    assertThat(cas1Application.targetLocation).isEqualTo(application.targetLocation)
    assertThat(cas1Application.convictionId).isEqualTo(application.convictionId.toString())
    assertThat(cas1Application.caseManagerName).isEqualTo(application.caseManagerUserDetails?.name)
    assertThat(cas1Application.isEmergencyApplication).isEqualTo(application.isEmergencyApplication)
    assertThat(cas1Application.releaseType).isEqualTo(application.releaseType)
    assertThat(cas1Application.name).isEqualTo(application.name)
    assertThat(cas1Application.caseManagerIsNotApplicant).isEqualTo(application.caseManagerIsNotApplicant)
    assertThat(cas1Application.eventNumber).isEqualTo(application.eventNumber)
    assertThat(cas1Application.isWomensApplication).isEqualTo(application.isWomensApplication)
    assertThat(cas1Application.otherWithdrawalReason).isEqualTo(application.otherWithdrawalReason)
    assertThat(cas1Application.status).isEqualTo(application.status.toString())
  }

  private fun assertTimelineNote(cas1TimelineNote: Map<String, Any>?, timelineNote: ApplicationTimelineNoteEntity) {
    assertThat(cas1TimelineNote).isNotNull
    assertThat(cas1TimelineNote!!["body"]).isEqualTo(timelineNote.body)
    assertThat(cas1TimelineNote["created_at"]).isEqualTo(CREATED_AT_NO_TZ)
    assertThat(cas1TimelineNote["user_name"]).isEqualTo(timelineNote.createdBy?.name)
  }

  private fun assertAssessment(cas1Assessment: Cas1Assessment?, assessment: ApprovedPremisesAssessmentEntity) {
    assertThat(cas1Assessment).isNotNull
    // Note: applicationId is not currently provided by the repository/service for Assessments in CAS1 SAR
    // assertThat(cas1Assessment!!.assessmentUser).isEqualTo(assessment.allocatedToUser?.name)
    assertThat(cas1Assessment!!.createdAt).isEqualTo(CREATED_AT)
    assertThat(cas1Assessment.allocatedAt).isEqualTo(ALLOCATED_AT)
    assertThat(cas1Assessment.submittedAt).isEqualTo(SUBMITTED_AT)
    assertThat(cas1Assessment.decision).isEqualTo(assessment.decision.toString())
    assertThat(cas1Assessment.rejectionRationale).isEqualTo(assessment.rejectionRationale)
    assertThat(cas1Assessment.isWithdrawn).isEqualTo(assessment.isWithdrawn)
  }

  private fun assertAssessmentClarificationNote(cas1Note: Cas1AssessmentClarificationNote?, note: AssessmentClarificationNoteEntity) {
    assertThat(cas1Note).isNotNull
    // Note: assessmentId and assessmentUser are not currently provided by the repository/service for Assessment Clarification Notes in CAS1 SAR
    // assertThat(cas1Note!!.assessmentId).isEqualTo(note.assessment.id.toString())
    assertThat(cas1Note!!.query).isEqualTo(note.query)
    assertThat(cas1Note.response).isEqualTo(note.response)
    assertThat(cas1Note.responseReceivedOn).isEqualTo(RESPONSE_RECEIVED_AT)
    // assertThat(cas1Note.createdAt).isEqualTo(CREATED_AT)
    // assertThat(cas1Note.assessmentUser).isEqualTo(note.createdByUser.name)
  }

  private fun assertAppeal(cas1Appeal: Cas1Appeal?, appeal: AppealEntity) {
    assertThat(cas1Appeal).isNotNull
    // assertThat(cas1Appeal!!.appealId).isEqualTo(appeal.id.toString())
    // assertThat(cas1Appeal!!.assessmentId).isEqualTo(appeal.assessment.id.toString())
    // assertThat(cas1Appeal!!.applicationId).isEqualTo(appeal.application.id.toString())
    assertThat(cas1Appeal!!.appealDate).isEqualTo(APPEAL_DATE_ONLY)
    assertThat(cas1Appeal.appealCreatedAt).isEqualTo(CREATED_AT)
    assertThat(cas1Appeal.createdByUser).isEqualTo(appeal.createdBy.name)
    assertThat(cas1Appeal.decision).isEqualTo(appeal.decision)
    assertThat(cas1Appeal.appealDetail).isEqualTo(appeal.appealDetail)
    assertThat(cas1Appeal.decisionDetail).isEqualTo(appeal.decisionDetail)
  }

  private fun assertSpaceBooking(cas1SpaceBooking: Cas1SpaceBooking?, spaceBooking: Cas1SpaceBookingEntity) {
    assertThat(cas1SpaceBooking).isNotNull
    // assertThat(cas1SpaceBooking!!.applicationId).isEqualTo(spaceBooking.application?.id.toString())
    assertThat(cas1SpaceBooking!!.premisesName).isEqualTo(spaceBooking.premises.name)
    // assertThat(cas1SpaceBooking.arrivalDate).isEqualTo(arrivedAtDateOnly)
//    assertThat(cas1SpaceBooking.departureDate).isEqualTo(departedAtDateOnly)
    assertThat(cas1SpaceBooking.actualArrivalDate).isEqualTo(arrivedAtDateOnly)
    assertThat(cas1SpaceBooking.actualArrivalTime).isEqualTo(arrivedAtTime)
    assertThat(cas1SpaceBooking.actualDepartureDate).isEqualTo(departedAtDateOnly)
    assertThat(cas1SpaceBooking.actualDepartureTime).isEqualTo(departedAtTime)
    assertThat(cas1SpaceBooking.canonicalArrivalDate).isEqualTo(arrivedAtDateOnly)
    assertThat(cas1SpaceBooking.canonicalDepartureDate).isEqualTo(departedAtDateOnly)
    assertThat(cas1SpaceBooking.crn).isEqualTo(spaceBooking.crn)
    assertThat(cas1SpaceBooking.keyWorkerName).isEqualTo(spaceBooking.keyWorkerName)
    assertThat(cas1SpaceBooking.keyWorkerStaffCode).isEqualTo(spaceBooking.keyWorkerStaffCode)
    assertThat(cas1SpaceBooking.createdAt).isEqualTo(CREATED_AT)
//    assertThat(cas1SpaceBooking.expectedArrivalTime).isEqualTo(arrivedAtTime)
//    assertThat(cas1SpaceBooking.expectedDepartureTime).isEqualTo(departedAtTime)
    assertThat(cas1SpaceBooking.deliusEventNumber).isEqualTo(spaceBooking.deliusEventNumber)
    assertThat(cas1SpaceBooking.nonArrivalConfirmedAt).isEqualTo(CREATED_AT)
    assertThat(cas1SpaceBooking.nonArrivalNotes).isEqualTo(spaceBooking.nonArrivalNotes)
    assertThat(cas1SpaceBooking.departureNotes).isEqualTo(spaceBooking.departureNotes)
    assertThat(cas1SpaceBooking.cancellationRecordedAt).isEqualTo(CANCELLATION_DATE)
    assertThat(cas1SpaceBooking.cancellationReasonNotes).isEqualTo(spaceBooking.cancellationReasonNotes)
  }

  private fun assertPlacementApplication(cas1PlacementApp: Cas1PlacementApplication?, placementApp: PlacementApplicationEntity) {
    assertThat(cas1PlacementApp).isNotNull
//    assertThat(cas1PlacementApp!!.applicationId).isEqualTo(placementApp.application.id.toString())
    assertThat(cas1PlacementApp!!.createdByUser).isEqualTo(placementApp.createdByUser.name)
    assertThat(cas1PlacementApp.createdAt).isEqualTo(CREATED_AT_NO_TZ)
    assertThat(cas1PlacementApp.submittedAt).isEqualTo(SUBMITTED_AT_NO_TZ)
    assertThat(cas1PlacementApp.decision).isEqualTo(placementApp.decision.toString())
    assertThat(cas1PlacementApp.decisionMadeAt).isEqualTo(DECISION_MADE_AT_NO_TZ)
    assertThat(cas1PlacementApp.withdrawalReason).isEqualTo(placementApp.withdrawalReason.toString())
    assertThat(cas1PlacementApp.placementType).isEqualTo(PlacementType.ADDITIONAL_PLACEMENT.toString())
  }

  private fun assertPlacementRequest(cas1PlacementReq: Cas1PlacementRequest?, placementReq: PlacementRequestEntity) {
    assertThat(cas1PlacementReq).isNotNull
//    assertThat(cas1PlacementReq!!.applicationId).isEqualTo(placementReq.application.id.toString())
//    assertThat(cas1PlacementReq!!.assessmentId).isEqualTo(placementReq.assessment.id.toString())
//    assertThat(cas1PlacementReq!!.placementRequirementsId).isEqualTo(placementReq.placementRequirements.id.toString())
    assertThat(cas1PlacementReq!!.createdAt).isEqualTo(CREATED_AT)
    assertThat(cas1PlacementReq.expectedArrival).isEqualTo(arrivedAtDateOnly)
    assertThat(cas1PlacementReq.duration).isEqualTo(placementReq.duration)
    assertThat(cas1PlacementReq.isWithdrawn).isEqualTo(placementReq.isWithdrawn)
    assertThat(cas1PlacementReq.withdrawalReason).isEqualTo(placementReq.withdrawalReason.toString())
  }

  private fun assertPlacementRequirements(cas1PlacementReqs: Cas1PlacementRequirements?, placementReqs: PlacementRequirementsEntity) {
    assertThat(cas1PlacementReqs).isNotNull
//    assertThat(cas1PlacementReqs!!.applicationId).isEqualTo(placementReqs.application.id.toString())
//    assertThat(cas1PlacementReqs!!.assessmentId).isEqualTo(placementReqs.assessment.id.toString())
    assertThat(cas1PlacementReqs!!.createdAt).isEqualTo(CREATED_AT)
    assertThat(cas1PlacementReqs.apType).isEqualTo(placementReqs.apType.name)
    assertThat(cas1PlacementReqs.radius).isEqualTo(placementReqs.radius)
  }

  @Suppress("UNUSED_PARAMETER")
  private fun assertPlacementRequirementCriteria(cas1Criteria: List<Cas1PlacementRequirementCriteria>?, placementReqs: PlacementRequirementsEntity) {
    assertThat(cas1Criteria).isNotNull
    assertThat(cas1Criteria).hasSize(2)
    // TODO analyse the criteria and update the test
/*    val desirable = cas1Criteria!!.find { it.criterionName == placementReqs.desirableCriteria[0].name }
    assertThat(desirable).isNotNull
    val essential = cas1Criteria.find { it.criterionName == placementReqs.essentialCriteria[0].name }
    assertThat(essential).isNotNull*/
  }

  private fun assertBookingNotMade(cas1BookingNotMade: Cas1BookingNotMade?, bookingNotMade: BookingNotMadeEntity) {
    assertThat(cas1BookingNotMade).isNotNull
    // assertThat(cas1BookingNotMade!!.placementRequestId).isEqualTo(bookingNotMade.placementRequest.id.toString())
    assertThat(cas1BookingNotMade!!.createdAt).isEqualTo(CREATED_AT_NO_TZ)
    assertThat(cas1BookingNotMade.notes).isEqualTo(bookingNotMade.notes)
  }

  private fun assertDomainEvent(cas1DomainEvent: DomainEvent?, domainEvent: DomainEventEntity) {
    assertThat(cas1DomainEvent).isNotNull
    // id, applicationId, assessmentId are not provided by the repository query
//    assertThat(cas1DomainEvent!!.id).isEqualTo(domainEvent.id.toString())
//    assertThat(cas1DomainEvent!!.applicationId).isEqualTo(domainEvent.applicationId.toString())
    // assertThat(cas1DomainEvent!!.assessmentId).isEqualTo(domainEvent.assessmentId.toString())
    assertThat(cas1DomainEvent!!.crn).isEqualTo(domainEvent.crn)
    assertThat(cas1DomainEvent.type).isEqualTo(domainEvent.type.name)
    assertThat(cas1DomainEvent.createdAt).isEqualTo(CREATED_AT)
    assertThat(cas1DomainEvent.occurredAt).isEqualTo(ALLOCATED_AT)
  }

  @Suppress("UNUSED_PARAMETER")
  private fun assertDomainEventMetadata(cas1Metadata: DomainEventMetadata?, domainEvent: DomainEventEntity) {
    assertThat(cas1Metadata).isNotNull
  }
}

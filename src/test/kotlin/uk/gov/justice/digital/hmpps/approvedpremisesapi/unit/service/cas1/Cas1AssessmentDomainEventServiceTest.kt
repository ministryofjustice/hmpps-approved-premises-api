package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserQualificationAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1AssessmentDomainEventServiceTest {

  private val domainEventService = mockk<DomainEventService>()
  private val communityApiClient = mockk<CommunityApiClient>()

  val service = Cas1AssessmentDomainEventService(
    domainEventService,
    communityApiClient,
    UrlTemplate("http://frontend/applications/#id"),
    UrlTemplate("http://frontend/assessments/#id"),
  )

  @Nested
  inner class AssessmentAllocated {

    private val assessment = createAssessment()

    private val assigneeUser = UserEntityFactory()
      .withDeliusUsername("Assignee User")
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .produce()
      .apply {
        roles += UserRoleAssignmentEntityFactory()
          .withUser(this)
          .withRole(UserRole.CAS1_ASSESSOR)
          .produce()

        qualifications += UserQualificationAssignmentEntityFactory()
          .withUser(this)
          .withQualification(UserQualification.PIPE)
          .produce()
      }

    private val allocatingUser = UserEntityFactory()
      .withDeliusUsername("Acting User")
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }.produce()

    @Test
    fun `assessmentAllocated raises domain event`() {
      val assigneeUserStaffDetails = StaffUserDetailsFactory().produce()
      every { communityApiClient.getStaffUserDetails(assigneeUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        assigneeUserStaffDetails,
      )

      val allocatingUserStaffDetails = StaffUserDetailsFactory().produce()
      every { communityApiClient.getStaffUserDetails(allocatingUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        allocatingUserStaffDetails,
      )

      every { domainEventService.saveAssessmentAllocatedEvent(any()) } just Runs

      service.assessmentAllocated(assessment, assigneeUser, allocatingUser)

      verify(exactly = 1) {
        domainEventService.saveAssessmentAllocatedEvent(
          match {
            val envelope = it.data
            val eventDetails = envelope.eventDetails

            val rootDomainEventDataMatches = (
              it.assessmentId == assessment.id &&
                it.applicationId == assessment.application.id &&
                it.crn == assessment.application.crn
              )

            val envelopeMatches = envelope.eventType == EventType.assessmentAllocated

            val allocatedToUserDetailsMatch =
              assertStaffMemberDetailsMatch(eventDetails.allocatedTo, assigneeUserStaffDetails)
            val allocatedByUserDetailsMatch =
              assertStaffMemberDetailsMatch(eventDetails.allocatedBy, allocatingUserStaffDetails)

            val eventDetailsMatch = (
              eventDetails.assessmentId == assessment.id &&
                eventDetails.assessmentUrl == "http://frontend/assessments/${assessment.id}" &&
                eventDetails.personReference.crn == assessment.application.crn &&
                eventDetails.personReference.noms == assessment.application.nomsNumber!! &&
                allocatedToUserDetailsMatch &&
                allocatedByUserDetailsMatch
              )

            rootDomainEventDataMatches && envelopeMatches && eventDetailsMatch
          },
        )
      }
    }

    @Test
    fun `assessmentAllocated allocating user is optional`() {
      val assigneeUserStaffDetails = StaffUserDetailsFactory().produce()
      every { communityApiClient.getStaffUserDetails(assigneeUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        assigneeUserStaffDetails,
      )

      every { domainEventService.saveAssessmentAllocatedEvent(any()) } just Runs

      service.assessmentAllocated(assessment, assigneeUser, allocatingUser = null)

      verify(exactly = 1) {
        domainEventService.saveAssessmentAllocatedEvent(
          withArg {
            Assertions.assertThat(it.data.eventDetails.allocatedBy).isNull()
          },
        )
      }
    }
  }

  @Nested
  inner class FurtherInformationRequested {
    @BeforeEach
    fun setup() {
      clearAllMocks()
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = [true, false])
    fun `it raises a Further Information Requested Domain Event`(emit: Boolean?) {
      val assessment = createAssessment()
      val clarificationNoteEntity = AssessmentClarificationNoteEntityFactory()
        .withAssessment(assessment)
        .withCreatedBy(
          UserEntityFactory()
            .withUnitTestControlProbationRegion()
            .produce(),
        )
        .produce()

      val requesterStaffDetails = StaffUserDetailsFactory().produce()
      val recipientStaffDetails = StaffUserDetailsFactory().produce()

      every { communityApiClient.getStaffUserDetails(clarificationNoteEntity.createdByUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        requesterStaffDetails,
      )
      every { communityApiClient.getStaffUserDetails(assessment.application.createdByUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        recipientStaffDetails,
      )

      val emitValue: Boolean

      if (emit !== null) {
        emitValue = emit
        every { domainEventService.saveFurtherInformationRequestedEvent(any(), emit) } just Runs
        service.furtherInformationRequested(assessment, clarificationNoteEntity, emit)
      } else {
        emitValue = true
        every { domainEventService.saveFurtherInformationRequestedEvent(any(), true) } just Runs
        service.furtherInformationRequested(assessment, clarificationNoteEntity)
      }

      verify(exactly = 1) {
        domainEventService.saveFurtherInformationRequestedEvent(
          match {
            val envelope = it.data
            val eventDetails = envelope.eventDetails

            val rootDomainEventDataMatches = (
              it.assessmentId == assessment.id &&
                it.applicationId == assessment.application.id &&
                it.crn == assessment.application.crn
              )

            val envelopeMatches = envelope.eventType == EventType.informationRequestMade

            val requesterUserDetailsMatch =
              assertStaffMemberDetailsMatch(eventDetails.requester, requesterStaffDetails)
            val recipientUserDetailsMatch =
              assertStaffMemberDetailsMatch(eventDetails.recipient, recipientStaffDetails)

            val eventDetailsMatch = (
              eventDetails.assessmentId == assessment.id &&
                eventDetails.assessmentUrl == "http://frontend/assessments/${assessment.id}" &&
                eventDetails.applicationUrl == "http://frontend/applications/${assessment.application.id}" &&
                eventDetails.personReference.crn == assessment.application.crn &&
                eventDetails.personReference.noms == assessment.application.nomsNumber!! &&
                eventDetails.requestId == clarificationNoteEntity.id
              )

            rootDomainEventDataMatches && envelopeMatches && eventDetailsMatch && requesterUserDetailsMatch && recipientUserDetailsMatch
          },
          emitValue,
        )
      }
    }
  }

  private fun createAssessment(): ApprovedPremisesAssessmentEntity {
    val allocatedUser: UserEntity = UserEntityFactory()
      .withDefaultProbationRegion()
      .produce()

    val schema = ApprovedPremisesAssessmentJsonSchemaEntity(
      id = UUID.randomUUID(),
      addedAt = OffsetDateTime.now(),
      schema = "{}",
    )

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withCrn(Cas1AssessmentEmailServiceTest.Constants.CRN)
          .withCreatedByUser(UserEntityFactory().withDefaultProbationRegion().produce())
          .produce(),
      )
      .withAssessmentSchema(schema)
      .withData("{\"test\": \"data\"}")
      .withAllocatedToUser(allocatedUser)
      .withSubmittedAt(null)
      .withReallocatedAt(null)
      .withIsWithdrawn(false)
      .produce()

    return assessment
  }

  private fun assertStaffMemberDetailsMatch(staffMember: StaffMember?, staffDetails: StaffUserDetails?) = when {
    staffMember == null -> staffDetails == null
    else ->
      staffDetails != null &&
        staffMember.staffCode == staffDetails.staffCode &&
        staffMember.staffIdentifier == staffDetails.staffIdentifier &&
        staffMember.forenames == staffDetails.staff.forenames &&
        staffMember.surname == staffDetails.staff.surname &&
        staffMember.username == staffDetails.username
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedAssessedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserQualificationAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1AssessmentDomainEventServiceTest {

  private val domainEventService = mockk<Cas1DomainEventService>()
  private val apDeliusContextApiClient = mockk<ApDeliusContextApiClient>()

  val service = Cas1AssessmentDomainEventService(
    domainEventService,
    apDeliusContextApiClient,
    Clock.systemDefaultZone(),
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
      val assigneeUserStaffDetails = StaffDetailFactory.staffDetail()
      every { apDeliusContextApiClient.getStaffDetail(assigneeUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        assigneeUserStaffDetails,
      )

      val allocatingUserStaffDetails = StaffDetailFactory.staffDetail()
      every { apDeliusContextApiClient.getStaffDetail(allocatingUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        allocatingUserStaffDetails,
      )

      every { domainEventService.saveAssessmentAllocatedEvent(any()) } just Runs

      service.assessmentAllocated(assessment, assigneeUser, allocatingUser)

      verify(exactly = 1) {
        domainEventService.saveAssessmentAllocatedEvent(
          match {
            val envelope = it.data
            val triggerSourceMatches = it.triggerSource == TriggerSourceType.USER

            val eventDetails = envelope.eventDetails

            val rootDomainEventDataMatches = (
              it.assessmentId == assessment.id &&
                it.applicationId == assessment.application.id &&
                it.crn == assessment.application.crn &&
                it.nomsNumber == assessment.application.nomsNumber
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

            triggerSourceMatches && rootDomainEventDataMatches && envelopeMatches && eventDetailsMatch
          },
        )
      }
    }

    @Test
    fun `assessmentAllocated allocating user is system`() {
      val assigneeUserStaffDetails = StaffDetailFactory.staffDetail()
      every { apDeliusContextApiClient.getStaffDetail(assigneeUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        assigneeUserStaffDetails,
      )

      every { domainEventService.saveAssessmentAllocatedEvent(any()) } just Runs

      service.assessmentAllocated(assessment, assigneeUser, allocatingUser = null)

      verify(exactly = 1) {
        domainEventService.saveAssessmentAllocatedEvent(
          withArg {
            assertThat(it.data.eventDetails.allocatedBy).isNull()
            assertThat(it.triggerSource).isEqualTo(TriggerSourceType.SYSTEM)
          },
        )
      }
    }
  }

  @Nested
  inner class AssessmentAccepted {

    @Test
    fun `assessmentAccepted raises domain event`() {
      val user = UserEntityFactory().withYieldedProbationRegion {
        ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
      }.withYieldedApArea {
        ApAreaEntityFactory()
          .withName("South West & South Central").produce()
      }.produce()

      val assessmentId = UUID.randomUUID()

      val assessmentSchema = ApprovedPremisesAssessmentJsonSchemaEntity(
        id = UUID.randomUUID(),
        addedAt = OffsetDateTime.now(),
        schema = "{}",
      )

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withCreatedByUser(
              UserEntityFactory()
                .withYieldedProbationRegion {
                  ProbationRegionEntityFactory()
                    .withYieldedApArea { ApAreaEntityFactory().produce() }
                    .produce()
                }
                .produce(),
            )
            .produce(),
        )
        .withAllocatedToUser(user)
        .withAssessmentSchema(assessmentSchema)
        .withData("{\"test\": \"data\"}")
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val application = assessment.application as ApprovedPremisesApplicationEntity
      val offenderDetails = OffenderDetailsSummaryFactory().produce()
      val staffUserDetails = StaffDetailFactory.staffDetail(
        probationArea = uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea(code = "N26", description = "description"),
      )

      val placementDates = PlacementDates(
        expectedArrival = LocalDate.now(),
        duration = 12,
      )
      val apType = ApType.normal

      every { apDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )
      every { domainEventService.saveApplicationAssessedDomainEvent(any()) } just Runs

      service.assessmentAccepted(application, assessment, offenderDetails, placementDates, apType, user)

      verify(exactly = 1) {
        apDeliusContextApiClient.getStaffDetail(user.deliusUsername)
      }

      val domainEventArgument = slot<Cas1DomainEvent<ApplicationAssessedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.saveApplicationAssessedDomainEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(assessment.application.id)
      assertThat(domainEvent.crn).isEqualTo(assessment.application.crn)
      assertThat(domainEvent.schemaVersion).isEqualTo(2)

      val eventDetails = domainEvent.data.eventDetails
      assertThat(eventDetails.applicationId).isEqualTo(assessment.application.id)
      assertThat(eventDetails.applicationUrl).isEqualTo("http://frontend/applications/${assessment.application.id}")
      assertThat(eventDetails.assessmentId).isEqualTo(assessment.id)
      assertThat(eventDetails.personReference.crn).isEqualTo(offenderDetails.otherIds.crn)
      assertThat(eventDetails.personReference.noms).isEqualTo(offenderDetails.otherIds.nomsNumber!!)
      assertThat(eventDetails.deliusEventNumber).isEqualTo((assessment.application as ApprovedPremisesApplicationEntity).eventNumber)
      val expectedAssessor = ApplicationAssessedAssessedBy(
        staffUserDetails.toStaffMember(),
        probationArea = ProbationArea(
          code = staffUserDetails.probationArea.code,
          name = staffUserDetails.probationArea.description,
        ),
        cru = Cru(
          name = "South West & South Central",
        ),
      )
      assertThat(eventDetails.assessedBy).isEqualTo(expectedAssessor)
      assertThat(eventDetails.decision).isEqualTo("ACCEPTED")
      assertThat(eventDetails.decisionRationale).isNull()
      assertThat(domainEvent.metadata).containsEntry(MetaDataName.CAS1_REQUESTED_AP_TYPE, "NORMAL")
    }
  }

  @Nested
  inner class AssessmentRejected {

    @Test
    fun `raise domain event()`() {
      val user = UserEntityFactory().withYieldedProbationRegion {
        ProbationRegionEntityFactory().withYieldedApArea { ApAreaEntityFactory().produce() }.produce()
      }.withYieldedApArea {
        ApAreaEntityFactory()
          .withName("South West & South Central").produce()
      }.produce()

      val assessmentId = UUID.randomUUID()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(
          UserEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .produce(),
        )
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withId(assessmentId)
        .withApplication(application)
        .withAllocatedToUser(user)
        .withAssessmentSchema(
          ApprovedPremisesAssessmentJsonSchemaEntity(
            id = UUID.randomUUID(),
            addedAt = OffsetDateTime.now(),
            schema = "{}",
          ),
        )
        .withData("{\"test\": \"data\"}")
        .withDecision(AssessmentDecision.REJECTED)
        .withRejectionRationale("reasoning")
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(assessment.application.crn)
        .produce()

      val staffUserDetails = StaffDetailFactory.staffDetail(code = "N26")

      every { apDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      every { domainEventService.saveApplicationAssessedDomainEvent(any()) } just Runs

      service.assessmentRejected(
        application,
        assessment,
        offenderDetails,
        user,
      )

      val domainEventArgument = slot<Cas1DomainEvent<ApplicationAssessedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.saveApplicationAssessedDomainEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(assessment.application.id)
      assertThat(domainEvent.assessmentId).isEqualTo(assessment.id)
      assertThat(domainEvent.crn).isEqualTo(assessment.application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(offenderDetails.otherIds.nomsNumber)
      assertThat(domainEvent.schemaVersion).isEqualTo(2)

      val data = domainEvent.data.eventDetails
      assertThat(data.applicationId).isEqualTo(assessment.application.id)
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${assessment.application.id}")
      assertThat(data.assessmentId).isEqualTo(assessment.id)
      assertThat(
        data.personReference,
      ).isEqualTo(
        PersonReference(offenderDetails.otherIds.crn, offenderDetails.otherIds.nomsNumber!!),
      )
      assertThat(data.deliusEventNumber).isEqualTo((assessment.application as ApprovedPremisesApplicationEntity).eventNumber)
      assertThat(data.assessedBy).isEqualTo(
        ApplicationAssessedAssessedBy(
          staffMember = StaffMember(
            staffCode = staffUserDetails.code,
            forenames = staffUserDetails.name.forenames(),
            surname = staffUserDetails.name.surname,
            username = staffUserDetails.username,
          ),
          probationArea = ProbationArea(
            code = staffUserDetails.probationArea.code,
            name = staffUserDetails.probationArea.description,
          ),
          cru = Cru(
            name = "South West & South Central",
          ),
        ),
      )
      assertThat(data.decision).isEqualTo("REJECTED")
      assertThat(data.decisionRationale).isEqualTo("reasoning")
    }
  }

  @Nested
  inner class FurtherInformationRequested {
    @BeforeEach
    fun setup() {
      clearAllMocks()
    }

    @Test
    fun `it raises a Further Information Requested Domain Event`() {
      val assessment = createAssessment()
      val clarificationNoteEntity = AssessmentClarificationNoteEntityFactory()
        .withAssessment(assessment)
        .withCreatedBy(
          UserEntityFactory()
            .withUnitTestControlProbationRegion()
            .produce(),
        )
        .produce()

      val requesterStaffDetails = StaffDetailFactory.staffDetail()
      val recipientStaffDetails = StaffDetailFactory.staffDetail()

      every { apDeliusContextApiClient.getStaffDetail(clarificationNoteEntity.createdByUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        requesterStaffDetails,
      )
      every { apDeliusContextApiClient.getStaffDetail(assessment.application.createdByUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        recipientStaffDetails,
      )

      every { domainEventService.saveFurtherInformationRequestedEvent(any()) } just Runs
      service.furtherInformationRequested(assessment, clarificationNoteEntity)

      verify(exactly = 1) {
        domainEventService.saveFurtherInformationRequestedEvent(
          match {
            val envelope = it.data
            val eventDetails = envelope.eventDetails

            val rootDomainEventDataMatches = (
              it.assessmentId == assessment.id &&
                it.applicationId == assessment.application.id &&
                it.crn == assessment.application.crn &&
                it.nomsNumber == assessment.application.nomsNumber
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
      .withReallocatedAt(null)
      .withIsWithdrawn(false)
      .produce()

    return assessment
  }

  private fun assertStaffMemberDetailsMatch(staffMember: StaffMember?, staffDetails: StaffDetail?) = when {
    staffMember == null -> staffDetails == null
    else ->
      staffDetails != null &&
        staffMember.staffCode == staffDetails.code &&
        staffMember.forenames == staffDetails.name.forenames() &&
        staffMember.surname == staffDetails.name.surname &&
        staffMember.username == staffDetails.username
  }
}

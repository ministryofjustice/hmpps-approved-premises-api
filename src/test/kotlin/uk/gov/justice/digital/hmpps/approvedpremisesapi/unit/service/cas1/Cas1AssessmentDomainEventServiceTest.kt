package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
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

    @Test
    fun `assessmentAllocated raises domain event`() {
      val assessment = createAssessment()

      val assigneeUser = UserEntityFactory()
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

      val actingUser = UserEntityFactory()
        .withDeliusUsername("Acting User")
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }.produce()

      val assigneeUserStaffDetails = StaffUserDetailsFactory().produce()
      every { communityApiClient.getStaffUserDetails(assigneeUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        assigneeUserStaffDetails,
      )

      val actingUserStaffDetails = StaffUserDetailsFactory().produce()
      every { communityApiClient.getStaffUserDetails(actingUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        actingUserStaffDetails,
      )

      every { domainEventService.saveAssessmentAllocatedEvent(any()) } just Runs

      service.assessmentAllocated(assessment, assigneeUser, actingUser)

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

            val envelopeMatches = envelope.eventType == "approved-premises.assessment.allocated"

            val allocatedToUserDetailsMatch = assertStaffMemberDetailsMatch(eventDetails.allocatedTo, assigneeUserStaffDetails)
            val allocatedByUserDetailsMatch = assertStaffMemberDetailsMatch(eventDetails.allocatedBy, actingUserStaffDetails)

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

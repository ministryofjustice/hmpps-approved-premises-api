package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Region
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ProbationAreaFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.WithdrawnByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.Period
import java.util.UUID

class Cas1ApplicationDomainEventServiceTest {
  private val mockOffenderService = mockk<OffenderService>()
  private val mockDomainEventService = mockk<DomainEventService>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockDomainEventTransformer = mockk<DomainEventTransformer>()
  private val mockCommunityApiClient = mockk<CommunityApiClient>()

  private val service = Cas1ApplicationDomainEventService(
    mockDomainEventService,
    mockOffenderService,
    mockCommunityApiClient,
    mockApDeliusContextApiClient,
    mockDomainEventTransformer,
    UrlTemplate("http://frontend/applications/#id"),
  )

  val applicationId: UUID = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
  val username = "SOMEPERSON"
  val user = UserEntityFactory()
    .withDeliusUsername(this.username)
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

  @Test
  fun `applicationSubmitted success`() {
    val situation = SituationOption.bailSentence

    val submitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
      translatedDocument = {},
      isPipeApplication = true,
      isWomensApplication = false,
      isEmergencyApplication = false,
      isEsapApplication = false,
      targetLocation = "SW1A 1AA",
      releaseType = ReleaseTypeOption.licence,
      type = "CAS1",
      sentenceType = SentenceTypeOption.nonStatutory,
      situation = situation,
      applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
      caseManagerIsNotApplicant = false,
    )

    val application = ApprovedPremisesApplicationEntityFactory()
      .withApplicationSchema(newestSchema)
      .withId(applicationId)
      .withCreatedByUser(user)
      .withSubmittedAt(null)
      .produce()
      .apply {
        schemaUpToDate = true
      }

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withGender("male")
      .withCrn(application.crn)
      .produce()

    every {
      mockOffenderService.getOffenderByCrn(
        application.crn,
        user.deliusUsername,
        true,
      )
    } returns AuthorisableActionResult.Success(
      offenderDetails,
    )

    val risks = PersonRisksFactory()
      .withMappa(
        RiskWithStatus(
          status = RiskStatus.Retrieved,
          value = Mappa(
            level = "CAT C1/LEVEL L1",
            lastUpdated = LocalDate.now(),
          ),
        ),
      )
      .produce()

    every {
      mockOffenderService.getRiskByCrn(
        application.crn,
        any(),
        user.deliusUsername,
      )
    } returns AuthorisableActionResult.Success(
      risks,
    )

    val staffUserDetails = StaffUserDetailsFactory()
      .withTeams(
        listOf(
          StaffUserTeamMembershipFactory()
            .produce(),
        ),
      )
      .produce()

    val caseDetails = CaseDetailFactory().produce()

    every { mockApDeliusContextApiClient.getCaseDetail(application.crn) } returns ClientResult.Success(
      status = HttpStatus.OK,
      body = caseDetails,
    )

    val domainEventStaffMember = StaffMemberFactory().produce()
    every { mockDomainEventTransformer.toStaffMember(staffUserDetails) } returns domainEventStaffMember

    val domainEventProbationArea = ProbationAreaFactory().produce()
    every { mockDomainEventTransformer.toProbationArea(staffUserDetails) } returns domainEventProbationArea

    every { mockDomainEventService.saveApplicationSubmittedDomainEvent(any()) } just Runs

    every { mockCommunityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
      status = HttpStatus.OK,
      body = staffUserDetails,
    )

    service.applicationSubmitted(
      application,
      submitApprovedPremisesApplication,
      username,
      "jwt",
    )

    verify(exactly = 1) {
      mockDomainEventService.saveApplicationSubmittedDomainEvent(
        match {
          val data = (it.data as ApplicationSubmittedEnvelope).eventDetails

          it.applicationId == application.id &&
            it.crn == application.crn &&
            data.applicationId == application.id &&
            data.applicationUrl == "http://frontend/applications/${application.id}" &&
            data.personReference == PersonReference(
            crn = offenderDetails.otherIds.crn,
            noms = offenderDetails.otherIds.nomsNumber!!,
          ) &&
            data.deliusEventNumber == application.eventNumber &&
            data.releaseType == submitApprovedPremisesApplication.releaseType.toString() &&
            data.age == Period.between(offenderDetails.dateOfBirth, LocalDate.now()).years &&
            data.gender == ApplicationSubmitted.Gender.male &&
            data.submittedBy == ApplicationSubmittedSubmittedBy(
            staffMember = domainEventStaffMember,
            probationArea = domainEventProbationArea,
            team = Team(
              code = caseDetails.case.manager.team.code,
              name = caseDetails.case.manager.team.name,
            ),
            ldu = Ldu(
              code = caseDetails.case.manager.team.ldu.code,
              name = caseDetails.case.manager.team.ldu.name,
            ),
            region = Region(
              code = staffUserDetails.probationArea.code,
              name = staffUserDetails.probationArea.description,
            ),
          ) &&
            data.mappa == risks.mappa.value!!.level &&
            data.sentenceLengthInMonths == null &&
            data.offenceId == application.offenceId
        },
      )
    }
  }

  @Test
  fun `applicationWithdrawn success`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withWithdrawalReason("alternative_identified_placement_no_longer_required")
      .withOtherWithdrawalReason("the other reason")
      .produce()

    val domainEventWithdrawnBy = WithdrawnByFactory().produce()
    every { mockDomainEventTransformer.toWithdrawnBy(user) } returns domainEventWithdrawnBy
    every { mockDomainEventService.saveApplicationWithdrawnEvent(any()) } just Runs

    service.applicationWithdrawn(application, user)

    verify(exactly = 1) {
      mockDomainEventService.saveApplicationWithdrawnEvent(
        match {
          val data = it.data.eventDetails

          it.applicationId == application.id &&
            it.crn == application.crn &&
            data.applicationId == application.id &&
            data.applicationUrl == "http://frontend/applications/${application.id}" &&
            data.personReference == PersonReference(
            crn = application.crn,
            noms = application.nomsNumber!!,
          ) &&
            data.deliusEventNumber == application.eventNumber &&
            data.withdrawalReason == "alternative_identified_placement_no_longer_required" &&
            data.otherWithdrawalReason == "the other reason"
        },
      )
    }
  }
}

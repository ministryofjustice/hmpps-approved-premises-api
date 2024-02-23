package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AppealEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AppealService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AppealEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.addRoleForUnitTest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.util.Optional
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AppealDecision as DomainEventApiAppealDecision

class AppealServiceTest {
  private val appealRepository = mockk<AppealRepository>()
  private val assessmentService = mockk<AssessmentService>()
  private val domainEventService = mockk<DomainEventService>()
  private val communityApiClient = mockk<CommunityApiClient>()
  private val cas1AppealEmailService = mockk<Cas1AppealEmailService>()
  private val applicationUrlTemplate = mockk<UrlTemplate>()
  private val applicationAppealUrlTemplate = mockk<UrlTemplate>()

  private val appealService = AppealService(
    appealRepository,
    assessmentService,
    domainEventService,
    communityApiClient,
    cas1AppealEmailService,
    applicationUrlTemplate,
    applicationAppealUrlTemplate,
  )

  private val probationRegion = ProbationRegionEntityFactory()
    .withApArea(
      ApAreaEntityFactory()
        .produce(),
    )
    .produce()

  private val createdByUser = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  private val staffUserDetails = StaffUserDetailsFactory()
    .withUsername(createdByUser.deliusUsername)
    .produce()

  private val application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(createdByUser)
    .produce()

  private val assessment = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(application)
    .produce()

  private val appealId = UUID.randomUUID()

  @Nested
  inner class GetAppeal {
    @Test
    fun `Returns NotFound if the appeal does not exist`() {
      every { appealRepository.findById(any()) } returns Optional.empty()

      val result = appealService.getAppeal(UUID.randomUUID(), application)

      assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    }

    @Test
    fun `Returns NotFound if the appeal is not for the given application`() {
      val appeal = AppealEntityFactory()
        .produce()

      every { appealRepository.findById(appeal.id) } returns Optional.of(appeal)

      val result = appealService.getAppeal(appeal.id, application)

      assertThat(result).isInstanceOf(AuthorisableActionResult.NotFound::class.java)
    }

    @Test
    fun `Returns Success containing expected appeal`() {
      val appeal = AppealEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      every { appealRepository.findById(appeal.id) } returns Optional.of(appeal)

      val result = appealService.getAppeal(appeal.id, application)

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isEqualTo(appeal)
    }
  }

  @Nested
  inner class CreateAppeal {
    @Test
    fun `Returns Unauthorised if the creating user does not have the CAS1_APPEALS_MANAGER role`() {
      val result = appealService.createAppeal(
        LocalDate.now(),
        "Some information about why the appeal is being made",
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Unauthorised::class.java)
    }

    @Test
    fun `Returns FieldValidationError if the appeal date is in the future`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val result = appealService.createAppeal(
        LocalDate.now().plusDays(1),
        "Some information about why the appeal is being made",
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
      assertThat(resultEntity.validationMessages).containsEntry("$.appealDate", "mustNotBeFuture")
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = ["  ", "\t", "\n"])
    fun `Returns FieldValidationError if the appeal detail is blank`(appealDetail: String) {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val result = appealService.createAppeal(
        LocalDate.now(),
        appealDetail,
        AppealDecision.accepted,
        "Some information about the decision made",
        application,
        assessment,
        createdByUser,
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
      assertThat(resultEntity.validationMessages).containsEntry("$.appealDetail", "empty")
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = ["  ", "\t", "\n"])
    fun `Returns FieldValidationError if the decision detail is blank`(decisionDetail: String) {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val result = appealService.createAppeal(
        LocalDate.now(),
        "Some information about why the appeal is being made",
        AppealDecision.accepted,
        decisionDetail,
        application,
        assessment,
        createdByUser,
      )

      assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      val resultEntity = result.entity as ValidatableActionResult.FieldValidationError
      assertThat(resultEntity.validationMessages).containsEntry("$.decisionDetail", "empty")
    }

    @Test
    fun `Stores appeal in repository and returns the stored appeal`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val now = LocalDate.now()

      every { appealRepository.save(any()) } returnsArgument 0
      every { assessmentService.createApprovedPremisesAssessment(any(), any()) } returns mockk<ApprovedPremisesAssessmentEntity>()
      every { domainEventService.saveAssessmentAppealedEvent(any()) } just Runs
      every { communityApiClient.getStaffUserDetails(createdByUser.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
      every { applicationUrlTemplate.resolve(any(), any()) } returns "http://frontend/applications/${application.id}"
      every { applicationAppealUrlTemplate.resolve(any()) } returns "http://frontend/applications/${application.id}/appeals/$appealId"
      every { cas1AppealEmailService.appealSuccess(any(), any()) } returns Unit

      mockkStatic(UUID::class) {
        every { UUID.randomUUID() } returns appealId

        val result = appealService.createAppeal(
          now,
          "Some information about why the appeal is being made",
          AppealDecision.accepted,
          "Some information about the decision made",
          application,
          assessment,
          createdByUser,
        )

        assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
        result as AuthorisableActionResult.Success
        assertThat(result.entity).isInstanceOf(ValidatableActionResult.Success::class.java)
        val resultEntity = result.entity as ValidatableActionResult.Success
        assertThat(resultEntity.entity).matches {
          it.matches(now)
        }
        verify(exactly = 1) {
          appealRepository.save(
            match {
              it.matches(now)
            },
          )
        }
      }
    }

    @Test
    fun `Creates a domain event`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val now = LocalDate.now()

      every { appealRepository.save(any()) } returnsArgument 0
      every { assessmentService.createApprovedPremisesAssessment(any(), any()) } returns mockk<ApprovedPremisesAssessmentEntity>()
      every { domainEventService.saveAssessmentAppealedEvent(any()) } just Runs
      every { communityApiClient.getStaffUserDetails(createdByUser.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
      every { applicationUrlTemplate.resolve(any(), any()) } returns "http://frontend/applications/${application.id}"
      every { applicationAppealUrlTemplate.resolve(any()) } returns "http://frontend/applications/${application.id}/appeals/$appealId"
      every { cas1AppealEmailService.appealSuccess(any(), any()) } returns Unit

      mockkStatic(UUID::class) {
        every { UUID.randomUUID() } returns appealId

        val result = appealService.createAppeal(
          now,
          "Some information about why the appeal is being made",
          AppealDecision.accepted,
          "Some information about the decision made",
          application,
          assessment,
          createdByUser,
        )

        assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
        result as AuthorisableActionResult.Success
        assertThat(result.entity).isInstanceOf(ValidatableActionResult.Success::class.java)

        verify(exactly = 1) {
          domainEventService.saveAssessmentAppealedEvent(
            match {
              it.matches()
            },
          )
        }
      }
    }

    @Test
    fun `Does not create a new assessment if the appeal was rejected`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val now = LocalDate.now()

      every { appealRepository.save(any()) } returnsArgument 0
      every { assessmentService.createApprovedPremisesAssessment(any()) } returns mockk<ApprovedPremisesAssessmentEntity>()
      every { domainEventService.saveAssessmentAppealedEvent(any()) } just Runs
      every { communityApiClient.getStaffUserDetails(createdByUser.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
      every { applicationUrlTemplate.resolve(any(), any()) } returns "http://frontend/applications/${application.id}"
      every { applicationAppealUrlTemplate.resolve(any()) } returns "http://frontend/applications/${application.id}/appeals/$appealId"
      every { cas1AppealEmailService.appealFailed(any()) } returns Unit

      mockkStatic(UUID::class) {
        every { UUID.randomUUID() } returns appealId

        val result = appealService.createAppeal(
          now,
          "Some information about why the appeal is being made",
          AppealDecision.rejected,
          "Some information about the decision made",
          application,
          assessment,
          createdByUser,
        )

        assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
        result as AuthorisableActionResult.Success
        assertThat(result.entity).isInstanceOf(ValidatableActionResult.Success::class.java)

        verify(exactly = 0) {
          assessmentService.createApprovedPremisesAssessment(any())
        }
      }
    }

    @Test
    fun `Sends a failure email if the appeal was rejected`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val now = LocalDate.now()

      every { appealRepository.save(any()) } returnsArgument 0
      every { assessmentService.createApprovedPremisesAssessment(any()) } returns mockk<ApprovedPremisesAssessmentEntity>()
      every { domainEventService.saveAssessmentAppealedEvent(any()) } just Runs
      every { communityApiClient.getStaffUserDetails(createdByUser.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
      every { applicationUrlTemplate.resolve(any(), any()) } returns "http://frontend/applications/${application.id}"
      every { applicationAppealUrlTemplate.resolve(any()) } returns "http://frontend/applications/${application.id}/appeals/$appealId"
      every { cas1AppealEmailService.appealFailed(any()) } returns Unit

      mockkStatic(UUID::class) {
        every { UUID.randomUUID() } returns appealId

        val result = appealService.createAppeal(
          now,
          "Some information about why the appeal is being made",
          AppealDecision.rejected,
          "Some information about the decision made",
          application,
          assessment,
          createdByUser,
        )

        assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
        result as AuthorisableActionResult.Success
        assertThat(result.entity).isInstanceOf(ValidatableActionResult.Success::class.java)

        verify(exactly = 1) {
          cas1AppealEmailService.appealFailed(application)
        }
      }
    }

    @Test
    fun `Creates a new assessment if the appeal was accepted`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val now = LocalDate.now()

      every { appealRepository.save(any()) } returnsArgument 0
      every { assessmentService.createApprovedPremisesAssessment(any(), any()) } returns mockk<ApprovedPremisesAssessmentEntity>()
      every { domainEventService.saveAssessmentAppealedEvent(any()) } just Runs
      every { communityApiClient.getStaffUserDetails(createdByUser.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
      every { applicationUrlTemplate.resolve(any(), any()) } returns "http://frontend/applications/${application.id}"
      every { applicationAppealUrlTemplate.resolve(any()) } returns "http://frontend/applications/${application.id}/appeals/$appealId"
      every { cas1AppealEmailService.appealSuccess(any(), any()) } returns Unit

      mockkStatic(UUID::class) {
        every { UUID.randomUUID() } returns appealId

        val result = appealService.createAppeal(
          now,
          "Some information about why the appeal is being made",
          AppealDecision.accepted,
          "Some information about the decision made",
          application,
          assessment,
          createdByUser,
        )

        assertThat(result).isInstanceOf(AuthorisableActionResult.Success::class.java)
        result as AuthorisableActionResult.Success
        assertThat(result.entity).isInstanceOf(ValidatableActionResult.Success::class.java)

        verify(exactly = 1) {
          assessmentService.createApprovedPremisesAssessment(application, createdFromAppeal = true)
        }
      }
    }

    @Test
    fun `Sends emails when the appeal was accepted`() {
      createdByUser.addRoleForUnitTest(UserRole.CAS1_APPEALS_MANAGER)

      val now = LocalDate.now()

      every { appealRepository.save(any()) } returnsArgument 0
      every { assessmentService.createApprovedPremisesAssessment(any(), any()) } returns mockk<ApprovedPremisesAssessmentEntity>()
      every { domainEventService.saveAssessmentAppealedEvent(any()) } just Runs
      every { communityApiClient.getStaffUserDetails(createdByUser.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffUserDetails)
      every { applicationUrlTemplate.resolve(any(), any()) } returns "http://frontend/applications/${application.id}"
      every { applicationAppealUrlTemplate.resolve(any()) } returns "http://frontend/applications/${application.id}/appeals/$appealId"
      every { cas1AppealEmailService.appealSuccess(any(), any()) } returns Unit

      mockkStatic(UUID::class) {
        every { UUID.randomUUID() } returns appealId

        val result = appealService.createAppeal(
          now,
          "Some information about why the appeal is being made",
          AppealDecision.accepted,
          "Some information about the decision made",
          application,
          assessment,
          createdByUser,
        )

        verify(exactly = 1) {
          cas1AppealEmailService.appealSuccess(
            application,
            match { it.application == application && it.createdBy == createdByUser },
          )
        }
      }
    }

    private fun AppealEntity.matches(now: LocalDate) =
      this.id == appealId &&
        this.appealDate == now &&
        this.appealDetail == "Some information about why the appeal is being made" &&
        this.decision == AppealDecision.accepted.value &&
        this.decisionDetail == "Some information about the decision made" &&
        this.application == application &&
        this.assessment == assessment &&
        this.createdBy == createdByUser

    private fun DomainEvent<AssessmentAppealedEnvelope>.matches() =
      this.applicationId == application.id &&
        this.assessmentId == null &&
        this.bookingId == null &&
        this.crn == application.crn &&
        withinSeconds(10).matches(this.occurredAt.toString()) &&
        this.data.matches()

    private fun AssessmentAppealedEnvelope.matches() =
      this.eventDetails.applicationId == application.id &&
        this.eventDetails.applicationUrl == "http://frontend/applications/${application.id}" &&
        this.eventDetails.appealId == appealId &&
        this.eventDetails.appealUrl == "http://frontend/applications/${application.id}/appeals/$appealId" &&
        this.eventDetails.personReference.crn == application.crn &&
        this.eventDetails.personReference.noms == application.nomsNumber &&
        this.eventDetails.deliusEventNumber == application.eventNumber &&
        withinSeconds(10).matches(this.eventDetails.createdAt.toString()) &&
        this.eventDetails.createdBy.matches() &&
        this.eventDetails.appealDetail == "Some information about why the appeal is being made" &&
        this.eventDetails.decision == DomainEventApiAppealDecision.accepted &&
        this.eventDetails.decisionDetail == "Some information about the decision made"

    private fun StaffMember.matches() =
      this.staffCode == staffUserDetails.staffCode &&
        this.staffIdentifier == staffUserDetails.staffIdentifier &&
        this.forenames == staffUserDetails.staff.forenames &&
        this.surname == staffUserDetails.staff.surname &&
        this.username == staffUserDetails.username
  }
}

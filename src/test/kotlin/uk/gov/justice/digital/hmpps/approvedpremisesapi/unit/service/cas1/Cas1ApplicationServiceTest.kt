package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.util.UUID

@SuppressWarnings("UnusedPrivateProperty")
@ExtendWith(MockKExtension::class)
class Cas1ApplicationServiceTest {

  @MockK
  private lateinit var approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository

  @MockK
  private lateinit var applicationRepository: ApplicationRepository

  @MockK
  private lateinit var offlineApplicationRepository: OfflineApplicationRepository

  @MockK
  private lateinit var cas1ApplicationStatusService: Cas1ApplicationStatusService

  @MockK
  private lateinit var cas1ApplicationDomainEventService: Cas1ApplicationDomainEventService

  @MockK
  private lateinit var cas1ApplicationEmailService: Cas1ApplicationEmailService

  @MockK
  private lateinit var cas1AssessmentService: Cas1AssessmentService

  @MockK
  private lateinit var cas1UserAccessService: Cas1UserAccessService

  @MockK
  private lateinit var userAccessService: UserAccessService

  @MockK
  private lateinit var userRepository: UserRepository

  @InjectMockKs
  private lateinit var service: Cas1ApplicationService

  @Nested
  inner class GetApplicationForUsername {

    @Test
    fun `getApplicationForUsername where user cannot access the application returns Unauthorised result`() {
      val distinguishedName = "SOMEPERSON"
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { userRepository.findByDeliusUsername(any()) } returns UserEntityFactory()
        .withDeliusUsername(distinguishedName)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(any()) } returns ApprovedPremisesApplicationEntityFactory()
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

      every { userAccessService.userCanViewApplication(any(), any()) } returns false

      assertThat(
        service.getApplicationForUsername(
          applicationId,
          distinguishedName,
        ) is CasResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `getApplicationForUsername where user can access the application returns Success result with entity from db`() {
      val distinguishedName = "SOMEPERSON"
      val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      val userEntity = UserEntityFactory()
        .withId(userId)
        .withDeliusUsername(distinguishedName)
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .produce()

      val applicationEntity = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(userEntity)
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(any()) } returns applicationEntity
      every { userRepository.findByDeliusUsername(any()) } returns userEntity
      every { userAccessService.userCanViewApplication(any(), any()) } returns true

      val result = service.getApplicationForUsername(applicationId, distinguishedName)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(applicationEntity)
      }
    }
  }

  @Nested
  inner class WithdrawApprovedPremisesApplication {

    @Test
    fun `withdrawApprovedPremisesApplication returns NotFound if Application does not exist`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val applicationId = UUID.fromString("bb13d346-f278-43d7-9c23-5c4077c031ca")

      every { approvedPremisesApplicationRepository.findByIdOrNull(applicationId) } returns null

      val result = service.withdrawApprovedPremisesApplication(
        applicationId,
        user,
        "alternative_identified_placement_no_longer_required",
        null,
      )

      assertThat(result is CasResult.NotFound).isTrue
    }

    @Test
    fun `withdrawApprovedPremisesApplication is idempotent and returns success if already withdrawn`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(true)
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(application.id) } returns application
      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = service.withdrawApprovedPremisesApplication(application.id, user, "other", null)

      assertThat(result is CasResult.Success).isTrue
    }

    @Test
    fun `withdrawApprovedPremisesApplication returns Success and saves Application with isWithdrawn set to true, triggers domain event and email`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      application.assessments.add(assessment)

      every { approvedPremisesApplicationRepository.findByIdOrNull(application.id) } returns application
      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { approvedPremisesApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesApplicationEntity }
      every { cas1ApplicationStatusService.applicationWithdrawn(any()) } just Runs
      every { cas1ApplicationDomainEventService.applicationWithdrawn(any(), any()) } just Runs
      every { cas1ApplicationEmailService.applicationWithdrawn(any(), any()) } just Runs
      every { cas1AssessmentService.updateAssessmentWithdrawn(any(), any()) } just Runs

      val result = service.withdrawApprovedPremisesApplication(
        application.id,
        user,
        "alternative_identified_placement_no_longer_required",
        null,
      )

      assertThat(result is CasResult.Success).isTrue

      verify {
        approvedPremisesApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "alternative_identified_placement_no_longer_required"
          },
        )
      }

      verify { cas1ApplicationStatusService.applicationWithdrawn(application) }
      verify { cas1ApplicationDomainEventService.applicationWithdrawn(application, user) }
      verify { cas1ApplicationEmailService.applicationWithdrawn(application, user) }
      verify { cas1AssessmentService.updateAssessmentWithdrawn(assessment.id, user) }
    }

    @Test
    fun `withdrawApprovedPremisesApplication returns Success and saves Application with isWithdrawn set to true, triggers domain event when other reason is set`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(application.id) } returns application
      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { approvedPremisesApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesApplicationEntity }
      every { cas1ApplicationStatusService.applicationWithdrawn(any()) } just Runs
      every { cas1ApplicationDomainEventService.applicationWithdrawn(any(), any()) } just Runs
      every { cas1ApplicationEmailService.applicationWithdrawn(any(), any()) } just Runs

      val result = service.withdrawApprovedPremisesApplication(application.id, user, "other", "Some other reason")

      assertThat(result is CasResult.Success).isTrue

      verify {
        approvedPremisesApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "other" &&
              it.otherWithdrawalReason == "Some other reason"
          },
        )
      }

      verify { cas1ApplicationStatusService.applicationWithdrawn(application) }
      verify { cas1ApplicationDomainEventService.applicationWithdrawn(application, user) }
      verify { cas1ApplicationEmailService.applicationWithdrawn(application, user) }
    }

    @Test
    fun `withdrawApprovedPremisesApplication does not persist otherWithdrawalReason if withdrawlReason is not other`() {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      every { approvedPremisesApplicationRepository.findByIdOrNull(application.id) } returns application
      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true
      every { approvedPremisesApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesApplicationEntity }
      every { cas1ApplicationStatusService.applicationWithdrawn(any()) } just Runs
      every { cas1ApplicationEmailService.applicationWithdrawn(any(), any()) } returns Unit
      every { cas1ApplicationDomainEventService.applicationWithdrawn(any(), any()) } just Runs

      service.withdrawApprovedPremisesApplication(
        application.id,
        user,
        "alternative_identified_placement_no_longer_required",
        "Some other reason",
      )

      verify {
        approvedPremisesApplicationRepository.save(
          match {
            it.id == application.id &&
              it is ApprovedPremisesApplicationEntity &&
              it.isWithdrawn &&
              it.withdrawalReason == "alternative_identified_placement_no_longer_required" &&
              it.otherWithdrawalReason == null
          },
        )
      }

      verify { cas1ApplicationStatusService.applicationWithdrawn(application) }
      verify { cas1ApplicationDomainEventService.applicationWithdrawn(application, user) }
      verify { cas1ApplicationEmailService.applicationWithdrawn(application, user) }
    }
  }

  @Nested
  inner class GetWithdrawableState {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    @Test
    fun `getWithdrawableState withdrawable if application not withdrawn`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(false)
        .produce()

      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = service.getWithdrawableState(application, user)

      assertThat(result.withdrawn).isFalse()
      assertThat(result.withdrawable).isTrue()
    }

    @Test
    fun `getWithdrawableState not withdrawable if application already withdrawn `() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(true)
        .produce()

      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns true

      val result = service.getWithdrawableState(application, user)

      assertThat(result.withdrawn).isTrue()
      assertThat(result.withdrawable).isFalse()
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `getWithdrawableState userMayDirectlyWithdraw delegates to user access service`(canWithdraw: Boolean) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withIsWithdrawn(false)
        .produce()

      every { cas1UserAccessService.userMayWithdrawApplication(user, application) } returns canWithdraw

      val result = service.getWithdrawableState(application, user)

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(canWithdraw)
    }
  }
}

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.ApplicationListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationService
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
  private lateinit var applicationListener: ApplicationListener

  @MockK
  private lateinit var cas1ApplicationDomainEventService: Cas1ApplicationDomainEventService

  @MockK
  private lateinit var cas1ApplicationEmailService: Cas1ApplicationEmailService

  @MockK
  private lateinit var assessmentService: AssessmentService

  @MockK
  private lateinit var userAccessService: UserAccessService

  @InjectMockKs
  private lateinit var service: Cas1ApplicationService

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
      every { userAccessService.userMayWithdrawApplication(user, application) } returns true

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

      every { approvedPremisesApplicationRepository.findByIdOrNull(application.id) } returns application
      every { userAccessService.userMayWithdrawApplication(user, application) } returns true
      every { applicationListener.preUpdate(any()) } returns Unit
      every { approvedPremisesApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesApplicationEntity }
      every { cas1ApplicationDomainEventService.applicationWithdrawn(any(), any()) } just Runs
      every { cas1ApplicationEmailService.applicationWithdrawn(any(), any()) } just Runs

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

      verify { cas1ApplicationDomainEventService.applicationWithdrawn(application, user) }
      verify { cas1ApplicationEmailService.applicationWithdrawn(application, user) }
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
      every { userAccessService.userMayWithdrawApplication(user, application) } returns true
      every { applicationListener.preUpdate(any()) } returns Unit
      every { approvedPremisesApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesApplicationEntity }
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
      every { userAccessService.userMayWithdrawApplication(user, application) } returns true
      every { applicationListener.preUpdate(any()) } returns Unit
      every { approvedPremisesApplicationRepository.save(any()) } answers { it.invocation.args[0] as ApprovedPremisesApplicationEntity }
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

      every { userAccessService.userMayWithdrawApplication(user, application) } returns true

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

      every { userAccessService.userMayWithdrawApplication(user, application) } returns true

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

      every { userAccessService.userMayWithdrawApplication(user, application) } returns canWithdraw

      val result = service.getWithdrawableState(application, user)

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(canWithdraw)
    }
  }
}

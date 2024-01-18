package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.allocations.rules

import io.mockk.EqMatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocatorRuleOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.rules.LegacyAllocatorRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.AllocationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UserAllocationsEngine

class LegacyAllocatorRuleTest {
  private val mockUserRepository = mockk<UserRepository>()
  private val mockOffenderService = mockk<OffenderService>()

  private val legacyAllocatorRule = LegacyAllocatorRule(0, mockUserRepository, mockOffenderService)

  val probationRegion = ProbationRegionEntityFactory()
    .withYieldedApArea {
      ApAreaEntityFactory()
        .produce()
    }
    .produce()

  private val createdByUser = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  val user = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  private val application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(createdByUser)
    .produce()

  private val assessment = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(application)
    .produce()

  private val placementRequirements = PlacementRequirementsEntityFactory()
    .withApplication(application)
    .withAssessment(assessment)
    .produce()

  private val placementRequest = PlacementRequestEntityFactory()
    .withPlacementRequirements(placementRequirements)
    .withApplication(application)
    .withAssessment(assessment)
    .produce()

  private val placementApplication = PlacementApplicationEntityFactory()
    .withApplication(application)
    .withCreatedByUser(createdByUser)
    .produce()

  @BeforeEach
  fun mockUserAllocationsEngine() {
    mockkConstructor(UserAllocationsEngine::class)
  }

  @AfterEach
  fun unmockUserAllocationsEngine() {
    unmockkConstructor(UserAllocationsEngine::class)
  }

  @Nested
  inner class EvaluateAssessment {
    @Test
    fun `Returns DoNotAllocate when UserAllocationsEngine returns null`() {
      every {
        constructedWith<UserAllocationsEngine>(
          EqMatcher(mockUserRepository),
          EqMatcher(AllocationType.Assessment),
          EqMatcher(assessment.application.getRequiredQualifications().toMutableList()),
          EqMatcher(true),
          EqMatcher(true),
        ).getAllocatedUser()
      } returns null

      every { mockOffenderService.isLao(assessment.application.crn) } returns true

      val result = legacyAllocatorRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.DoNotAllocate)
    }

    @Test
    fun `Returns AllocateToUser with username when UserAllocationsEngine returns a user`() {
      every {
        constructedWith<UserAllocationsEngine>(
          EqMatcher(mockUserRepository),
          EqMatcher(AllocationType.Assessment),
          EqMatcher(assessment.application.getRequiredQualifications().toMutableList()),
          EqMatcher(true),
          EqMatcher(true),
        ).getAllocatedUser()
      } returns user

      every { mockOffenderService.isLao(assessment.application.crn) } returns true

      val result = legacyAllocatorRule.evaluateAssessment(assessment)

      assertThat(result).isInstanceOf(UserAllocatorRuleOutcome.AllocateToUser::class.java)
      result as UserAllocatorRuleOutcome.AllocateToUser
      assertThat(result.userName).isEqualTo(user.deliusUsername)
    }
  }

  @Nested
  inner class EvaluatePlacementRequest {
    @Test
    fun `Returns DoNotAllocate when UserAllocationsEngine returns null`() {
      every {
        constructedWith<UserAllocationsEngine>(
          EqMatcher(mockUserRepository),
          EqMatcher(AllocationType.PlacementRequest),
          EqMatcher(emptyList<UserQualification>()),
          EqMatcher(true),
          EqMatcher(true),
        ).getAllocatedUser()
      } returns null

      every { mockOffenderService.isLao(placementRequest.application.crn) } returns true

      val result = legacyAllocatorRule.evaluatePlacementRequest(placementRequest)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.DoNotAllocate)
    }

    @Test
    fun `Returns AllocateToUser with username when UserAllocationsEngine returns a user`() {
      every {
        constructedWith<UserAllocationsEngine>(
          EqMatcher(mockUserRepository),
          EqMatcher(AllocationType.PlacementRequest),
          EqMatcher(emptyList<UserQualification>()),
          EqMatcher(true),
          EqMatcher(true),
        ).getAllocatedUser()
      } returns user

      every { mockOffenderService.isLao(placementRequest.application.crn) } returns true

      val result = legacyAllocatorRule.evaluatePlacementRequest(placementRequest)

      assertThat(result).isInstanceOf(UserAllocatorRuleOutcome.AllocateToUser::class.java)
      result as UserAllocatorRuleOutcome.AllocateToUser
      assertThat(result.userName).isEqualTo(user.deliusUsername)
    }
  }

  @Nested
  inner class EvaluatePlacementApplication {
    @Test
    fun `Returns DoNotAllocate when UserAllocationsEngine returns null`() {
      every {
        constructedWith<UserAllocationsEngine>(
          EqMatcher(mockUserRepository),
          EqMatcher(AllocationType.PlacementApplication),
          EqMatcher(emptyList<UserQualification>()),
          EqMatcher(true),
          EqMatcher(true),
        ).getAllocatedUser()
      } returns null

      every { mockOffenderService.isLao(placementApplication.application.crn) } returns true

      val result = legacyAllocatorRule.evaluatePlacementApplication(placementApplication)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.DoNotAllocate)
    }

    @Test
    fun `Returns AllocateToUser with username when UserAllocationsEngine returns a user`() {
      every {
        constructedWith<UserAllocationsEngine>(
          EqMatcher(mockUserRepository),
          EqMatcher(AllocationType.PlacementApplication),
          EqMatcher(emptyList<UserQualification>()),
          EqMatcher(true),
          EqMatcher(true),
        ).getAllocatedUser()
      } returns user

      every { mockOffenderService.isLao(placementApplication.application.crn) } returns true

      val result = legacyAllocatorRule.evaluatePlacementApplication(placementApplication)

      assertThat(result).isInstanceOf(UserAllocatorRuleOutcome.AllocateToUser::class.java)
      result as UserAllocatorRuleOutcome.AllocateToUser
      assertThat(result.userName).isEqualTo(user.deliusUsername)
    }
  }
}

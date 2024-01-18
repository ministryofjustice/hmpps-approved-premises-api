package uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.rules

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocatorRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocatorRuleOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.AllocationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UserAllocationsEngine

@Component
@ConditionalOnProperty(name = ["user-allocations.rules.legacy-allocator.enabled"])
class LegacyAllocatorRule(
  @Value("\${user-allocations.rules.legacy-allocator.priority:0}")
  override val priority: Int,
  private val userRepository: UserRepository,
  private val offenderService: OffenderService,
) : UserAllocatorRule {
  override val name: String
    get() = "Legacy allocations (adaptor for the legacy UserAllocationsEngine)"

  override fun evaluateAssessment(assessmentEntity: AssessmentEntity): UserAllocatorRuleOutcome {
    val qualifications = assessmentEntity.application.getRequiredQualifications().toMutableList()
    val isLao = offenderService.isLao(assessmentEntity.application.crn)

    val allocationsEngine = UserAllocationsEngine(this.userRepository, AllocationType.Assessment, qualifications, isLao)

    return when (val user = allocationsEngine.getAllocatedUser()) {
      null -> UserAllocatorRuleOutcome.DoNotAllocate
      else -> UserAllocatorRuleOutcome.AllocateToUser(user.deliusUsername)
    }
  }

  override fun evaluatePlacementRequest(placementRequestEntity: PlacementRequestEntity): UserAllocatorRuleOutcome {
    val isLao = offenderService.isLao(placementRequestEntity.application.crn)

    val allocationsEngine = UserAllocationsEngine(this.userRepository, AllocationType.PlacementRequest, emptyList(), isLao)

    return when (val user = allocationsEngine.getAllocatedUser()) {
      null -> UserAllocatorRuleOutcome.DoNotAllocate
      else -> UserAllocatorRuleOutcome.AllocateToUser(user.deliusUsername)
    }
  }

  override fun evaluatePlacementApplication(placementApplicationEntity: PlacementApplicationEntity): UserAllocatorRuleOutcome {
    val isLao = offenderService.isLao(placementApplicationEntity.application.crn)

    val allocationsEngine = UserAllocationsEngine(this.userRepository, AllocationType.PlacementApplication, emptyList(), isLao)

    return when (val user = allocationsEngine.getAllocatedUser()) {
      null -> UserAllocatorRuleOutcome.DoNotAllocate
      else -> UserAllocatorRuleOutcome.AllocateToUser(user.deliusUsername)
    }
  }
}

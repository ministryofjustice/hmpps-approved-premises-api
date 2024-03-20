package uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.rules

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocatorRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.allocations.UserAllocatorRuleOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity

@Component
@ConditionalOnProperty(name = ["user-allocations.rules.emergency-and-short-notice-assessments.enabled"])
class EmergencyAndShortNoticeAssessmentRule(
  @Value("\${user-allocations.rules.emergency-and-short-notice-assessments.priority:0}")
  override val priority: Int,
  private val config: EmergencyAndShortNoticeAssessmentRuleConfig,
) : UserAllocatorRule {
  override val name: String
    get() = "Emergency assessments"

  private val allocateToUsersNormalised: Map<String, String> by lazy {
    config.allocateToUsers.mapKeys { it.key.normalise() }
  }

  override fun evaluateAssessment(assessmentEntity: AssessmentEntity): UserAllocatorRuleOutcome {
    val application = assessmentEntity.application

    if (application !is ApprovedPremisesApplicationEntity) return UserAllocatorRuleOutcome.Skip
    if (application.submittedAt == null) return UserAllocatorRuleOutcome.Skip

    return when (application.isEmergencyApplication) {
      true -> allocateByRegion(application.apArea)
      else -> UserAllocatorRuleOutcome.Skip
    }
  }

  private fun allocateByRegion(apArea: ApAreaEntity?): UserAllocatorRuleOutcome {
    if (apArea == null) return UserAllocatorRuleOutcome.Skip

    val regionName = apArea.name.normalise()

    return when (val userName = allocateToUsersNormalised[regionName]) {
      null -> UserAllocatorRuleOutcome.Skip
      else -> UserAllocatorRuleOutcome.AllocateToUser(userName)
    }
  }

  private fun String.normalise(): String = this.lowercase()
    .replace(regex = "[^a-zA-Z -]".toRegex(), "")
    .replace("[\\s-]+".toRegex(), "-")
}

@Component
@ConfigurationProperties(prefix = "user-allocations.rules.emergency-and-short-notice-assessments")
data class EmergencyAndShortNoticeAssessmentRuleConfig(
  val allocateToUsers: Map<String, String>,
)

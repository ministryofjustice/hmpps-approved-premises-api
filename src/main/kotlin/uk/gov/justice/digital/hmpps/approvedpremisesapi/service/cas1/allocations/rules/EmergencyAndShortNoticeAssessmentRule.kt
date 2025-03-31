package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.rules

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AutoAllocationDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRule
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRuleOutcome
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

@SuppressWarnings("MagicNumber")
@Component
class EmergencyAndShortNoticeAssessmentRule(
  private val clock: Clock,
) : UserAllocatorRule {
  override val name: String
    get() = "Emergency assessments"

  override val priority: Int
    get() = 2

  override fun evaluateAssessment(assessmentEntity: AssessmentEntity): UserAllocatorRuleOutcome {
    val application = assessmentEntity.application

    if (application !is ApprovedPremisesApplicationEntity) return UserAllocatorRuleOutcome.Skip
    if (application.submittedAt == null) return UserAllocatorRuleOutcome.Skip

    return when (
      application.noticeType == Cas1ApplicationTimelinessCategory.emergency ||
        application.noticeType == Cas1ApplicationTimelinessCategory.shortNotice
    ) {
      true -> allocateByCruManagementArea(application.cruManagementArea!!)
      else -> UserAllocatorRuleOutcome.Skip
    }
  }

  private fun allocateByCruManagementArea(cruManagementArea: Cas1CruManagementAreaEntity): UserAllocatorRuleOutcome {
    // Javadoc states that dayOfWeek will never be null
    val autoAllocationDay = when (LocalDate.now(clock).dayOfWeek!!) {
      DayOfWeek.MONDAY -> AutoAllocationDay.MONDAY
      DayOfWeek.TUESDAY -> AutoAllocationDay.TUESDAY
      DayOfWeek.WEDNESDAY -> AutoAllocationDay.WEDNESDAY
      DayOfWeek.THURSDAY -> AutoAllocationDay.THURSDAY
      DayOfWeek.FRIDAY -> AutoAllocationDay.FRIDAY
      DayOfWeek.SATURDAY -> AutoAllocationDay.SATURDAY
      DayOfWeek.SUNDAY -> AutoAllocationDay.SUNDAY
    }

    return when (val userName = cruManagementArea.assessmentAutoAllocations[autoAllocationDay]) {
      null -> UserAllocatorRuleOutcome.Skip
      else -> UserAllocatorRuleOutcome.AllocateToUser(userName)
    }
  }
}

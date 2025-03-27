package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.allocations.rules

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks.MutableClockConfiguration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AutoAllocationDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.UserAllocatorRuleOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.allocations.rules.EmergencyAndShortNoticeAssessmentRule
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class EmergencyAndShortNoticeAssessmentRuleTest {

  @Nested
  inner class EvaluateAssessment {

    @ParameterizedTest
    // 1/7/2024 is a Monday
    @CsvSource(
      "emergency,2024-07-01,mr monday",
      "emergency,2024-07-02,mrs tuesday",
      "emergency,2024-07-03,ms wed",
      "emergency,2024-07-04,dr thurs",
      "emergency,2024-07-05,prof fri",
      "emergency,2024-07-06,sat snr",
      "emergency,2024-07-07,miss sunday",
      "shortNotice,2024-07-01,mr monday",
      "shortNotice,2024-07-02,mrs tuesday",
      "shortNotice,2024-07-03,ms wed",
      "shortNotice,2024-07-04,dr thurs",
      "shortNotice,2024-07-05,prof fri",
      "shortNotice,2024-07-06,sat snr",
      "shortNotice,2024-07-07,miss sunday",
    )
    fun `Returns AllocateToUser with configured username for the application's cru management area when the application is for CAS1, submitted, and the application is emergency or short notice`(
      noticeType: Cas1ApplicationTimelinessCategory,
      date: LocalDate,
      expectedAllocation: String,
    ) {
      val assessmentRule = EmergencyAndShortNoticeAssessmentRule(
        MutableClockConfiguration.MutableClock(
          date.atTime(12, 0, 0).toInstant(ZoneOffset.UTC),
        ),
      )

      val createdByUser = UserEntityFactory()
        .withDefaults()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withNoticeType(noticeType)
        .withCruManagementArea(
          Cas1CruManagementAreaEntityFactory()
            .withAssessmentAutoAllocationUsername(null)
            .withAssessmentAutoAllocations(
              mutableMapOf(
                AutoAllocationDay.MONDAY to "mr monday",
                AutoAllocationDay.TUESDAY to "mrs tuesday",
                AutoAllocationDay.WEDNESDAY to "ms wed",
                AutoAllocationDay.THURSDAY to "dr thurs",
                AutoAllocationDay.FRIDAY to "prof fri",
                AutoAllocationDay.SATURDAY to "sat snr",
                AutoAllocationDay.SUNDAY to "miss sunday",
              ),
            )
            .produce(),
        )
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = assessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.AllocateToUser(expectedAllocation))
    }

    @Test
    fun `Returns Skip if the application is not for CAS1`() {
      val assessmentRule = EmergencyAndShortNoticeAssessmentRule(Clock.systemUTC())

      val apArea = ApAreaEntityFactory()
        .produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(apArea)
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withProbationRegion(probationRegion)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = assessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @ParameterizedTest
    @EnumSource(Cas1ApplicationTimelinessCategory::class, names = ["emergency", "shortNotice"])
    fun `Returns Skip if the application is not submitted`(noticeType: Cas1ApplicationTimelinessCategory) {
      val assessmentRule = EmergencyAndShortNoticeAssessmentRule(Clock.systemUTC())

      val apArea = ApAreaEntityFactory()
        .produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea {
          ApAreaEntityFactory()
            .produce()
        }
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(null)
        .withCruManagementArea(null)
        .withNoticeType(noticeType)
        .produce()
        .apply {
          this.apArea = apArea
        }

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = assessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @Test
    fun `Returns Skip if the application has a standard noticeType`() {
      val assessmentRule = EmergencyAndShortNoticeAssessmentRule(Clock.systemUTC())

      val apArea = ApAreaEntityFactory()
        .produce()

      val probationRegion = ProbationRegionEntityFactory()
        .withApArea(apArea)
        .produce()

      val createdByUser = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withNoticeType(Cas1ApplicationTimelinessCategory.standard)
        .withCruManagementArea(null)
        .produce()
        .apply {
          this.apArea = apArea
        }

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = assessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }

    @ParameterizedTest
    @EnumSource(Cas1ApplicationTimelinessCategory::class, names = ["emergency", "shortNotice"])
    fun `Returns Skip with when no user is configured for the cru management area on the given day`(noticeType: Cas1ApplicationTimelinessCategory) {
      val assessmentRule = EmergencyAndShortNoticeAssessmentRule(
        MutableClockConfiguration.MutableClock(
          // Wednesday
          LocalDate.parse("2024-07-03").atTime(12, 0, 0).toInstant(ZoneOffset.UTC),
        ),
      )

      val createdByUser = UserEntityFactory()
        .withDefaults()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(createdByUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withNoticeType(noticeType)
        .withCruManagementArea(
          Cas1CruManagementAreaEntityFactory()
            .withAssessmentAutoAllocationUsername(null)
            .withAssessmentAutoAllocations(
              mutableMapOf(
                AutoAllocationDay.MONDAY to "mr monday",
                AutoAllocationDay.TUESDAY to "mrs tuesday",
                AutoAllocationDay.THURSDAY to "dr thurs",
                AutoAllocationDay.FRIDAY to "prof fri",
                AutoAllocationDay.SATURDAY to "sat snr",
                AutoAllocationDay.SUNDAY to "miss sunday",
              ),
            )
            .produce(),
        )
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val result = assessmentRule.evaluateAssessment(assessment)

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }
  }

  @Nested
  inner class EvaluatePlacementApplication {
    @Test
    fun `Always returns Skip`() {
      val assessmentRule = EmergencyAndShortNoticeAssessmentRule(Clock.systemUTC())

      val result = assessmentRule.evaluatePlacementApplication(mockk<PlacementApplicationEntity>())

      assertThat(result).isEqualTo(UserAllocatorRuleOutcome.Skip)
    }
  }
}

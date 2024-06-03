package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.generator

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.ReferralsMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApTypeCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ReferralsDataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ReferralsMetricsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TierCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ReferralsMetricsProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.LocalDate
import java.time.Period

enum class SomeOtherEnum {
  Foo,
  Bar,
}

class ReferralsMetricsReportGeneratorTest {
  private val newestJsonSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory()
    .withSchema("{}")
    .produce()

  private val user = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val mockWorkingDayService = mockk<WorkingDayService>()

  @Test
  fun `It groups referrals by Tier correctly`() {
    val tierA0Assessments = (1..3).toList().map { createDto("A0") }
    val tierA1Assessments = (1..4).toList().map { createDto("A1") }
    val tierD2Assessments = (1..5).toList().map { createDto("D2") }
    val noTierAssessments = (1..6).toList().map { createDto(null) }

    val assessments = listOf(
      tierA0Assessments,
      tierA1Assessments,
      tierD2Assessments,
      noTierAssessments,
    ).flatten()

    every { mockWorkingDayService.getWorkingDaysCount(any(), any()) } returns 1

    val results = ReferralsMetricsReportGenerator<TierCategory>(assessments, mockWorkingDayService).createReport(
      listOf(TierCategory.ALL, TierCategory.A0, TierCategory.A1, TierCategory.D2, TierCategory.NONE),
      ReferralsMetricsProperties(2023, 1),
    )

    assertThat(results[0][ReferralsMetricsReportRow::category]).isEqualTo(TierCategory.ALL.toString())
    assertThat(results[0][ReferralsMetricsReportRow::numberOfUniqueReferrals]).isEqualTo(assessments.size)

    assertThat(results[1][ReferralsMetricsReportRow::category]).isEqualTo(TierCategory.A0.toString())
    assertThat(results[1][ReferralsMetricsReportRow::numberOfUniqueReferrals]).isEqualTo(tierA0Assessments.size)

    assertThat(results[2][ReferralsMetricsReportRow::category]).isEqualTo(TierCategory.A1.toString())
    assertThat(results[2][ReferralsMetricsReportRow::numberOfUniqueReferrals]).isEqualTo(tierA1Assessments.size)

    assertThat(results[3][ReferralsMetricsReportRow::category]).isEqualTo(TierCategory.D2.toString())
    assertThat(results[3][ReferralsMetricsReportRow::numberOfUniqueReferrals]).isEqualTo(tierD2Assessments.size)

    assertThat(results[4][ReferralsMetricsReportRow::category]).isEqualTo(TierCategory.NONE.toString())
    assertThat(results[4][ReferralsMetricsReportRow::numberOfUniqueReferrals]).isEqualTo(noTierAssessments.size)
  }

  @Test
  fun `It groups referrals by AP Type correctly`() {
    val normalAssessments = (1..3).toList().map { createDto("A0") }
    val esapAssessments = (1..4).toList().map { createDto("A1", isEsap = true) }
    val pipeAssessments = (1..5).toList().map { createDto("D2", isPipe = true) }

    val assessments = listOf(
      normalAssessments,
      esapAssessments,
      pipeAssessments,
    ).flatten()

    every { mockWorkingDayService.getWorkingDaysCount(any(), any()) } returns 1

    val results = ReferralsMetricsReportGenerator<ApTypeCategory>(assessments, mockWorkingDayService).createReport(
      listOf(ApTypeCategory.ALL, ApTypeCategory.ESAP, ApTypeCategory.NORMAL, ApTypeCategory.PIPE),
      ReferralsMetricsProperties(2023, 1),
    )

    assertThat(results[0][ReferralsMetricsReportRow::category]).isEqualTo(ApTypeCategory.ALL.toString())
    assertThat(results[0][ReferralsMetricsReportRow::numberOfUniqueReferrals]).isEqualTo(assessments.size)

    assertThat(results[1][ReferralsMetricsReportRow::category]).isEqualTo(ApTypeCategory.ESAP.toString())
    assertThat(results[1][ReferralsMetricsReportRow::numberOfUniqueReferrals]).isEqualTo(esapAssessments.size)

    assertThat(results[2][ReferralsMetricsReportRow::category]).isEqualTo(ApTypeCategory.NORMAL.toString())
    assertThat(results[2][ReferralsMetricsReportRow::numberOfUniqueReferrals]).isEqualTo(normalAssessments.size)

    assertThat(results[3][ReferralsMetricsReportRow::category]).isEqualTo(ApTypeCategory.PIPE.toString())
    assertThat(results[3][ReferralsMetricsReportRow::numberOfUniqueReferrals]).isEqualTo(pipeAssessments.size)
  }

  @Test
  fun `it throws an error if an unrecognised type is given`() {
    val assessments = listOf(
      createDto("A0"),
    )

    assertThatThrownBy {
      ReferralsMetricsReportGenerator<SomeOtherEnum>(assessments, mockWorkingDayService).createReport(
        listOf(SomeOtherEnum.Foo, SomeOtherEnum.Bar),
        ReferralsMetricsProperties(2023, 1),
      )
    }.hasMessageContaining("Unknown Metric type - ${SomeOtherEnum::class.java}")
  }

  @Test
  fun `it returns assessment completion timeliness data correctly`() {
    val assessments = listOf(
      createDto(tier = "A0", applicationSubmittedAt = LocalDate.of(2023, 1, 2), assessmentSubmittedAt = LocalDate.of(2023, 1, 5)),
      createDto(tier = "A0", applicationSubmittedAt = LocalDate.of(2023, 1, 3), assessmentSubmittedAt = LocalDate.of(2023, 1, 7)),
      createDto(tier = "A0", applicationSubmittedAt = LocalDate.of(2023, 1, 4), assessmentSubmittedAt = LocalDate.of(2023, 1, 7)),
      createDto(tier = "A0", applicationSubmittedAt = LocalDate.of(2023, 1, 3), assessmentSubmittedAt = LocalDate.of(2023, 1, 15)),
    )

    every { mockWorkingDayService.getWorkingDaysCount(any<LocalDate>(), any<LocalDate>()) } answers {
      Period.between(firstArg(), secondArg()).days
    }

    val results = ReferralsMetricsReportGenerator<TierCategory>(assessments, mockWorkingDayService).createReport(
      listOf(TierCategory.A0, TierCategory.A1),
      ReferralsMetricsProperties(2023, 1),
    )

    assertThat(results[0][ReferralsMetricsReportRow::assessmentCompletionTimeliness]).isEqualTo("75.0%")
    assertThat(results[1][ReferralsMetricsReportRow::assessmentCompletionTimeliness]).isEqualTo("N/A")
  }

  @Test
  fun `it returns the correct number of accepted assessments`() {
    val acceptedAssessments = (1..3).toList().map { createDto("A0", decision = AssessmentDecision.ACCEPTED) }
    val rejectedAssessments = (1..6).toList().map { createDto("A0", decision = AssessmentDecision.REJECTED) }

    val assessments = listOf(
      acceptedAssessments,
      rejectedAssessments,
    ).flatten()

    every { mockWorkingDayService.getWorkingDaysCount(any(), any()) } returns 1

    val results = ReferralsMetricsReportGenerator<TierCategory>(assessments, mockWorkingDayService).createReport(
      listOf(TierCategory.A0),
      ReferralsMetricsProperties(2023, 1),
    )

    assertThat(results[0][ReferralsMetricsReportRow::referralsAccepted]).isEqualTo(acceptedAssessments.size)
  }

  @Test
  fun `it counts the number of rejected assessments by type`() {
    val assessmentsRejectedAccommodationNeedOnly = (1..1).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Reject, not suitable for an AP: Accommodation need only") }
    val assessmentsRejectedNeedsCannotBeMet = (1..2).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Reject, not suitable for an AP: Health / social care / disability needs cannot be met") }
    val assessmentsRejectedSupervisionPeriodTooShort = (1..3).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Reject, not suitable for an AP: Remaining supervision period too short") }
    val assessmentsRejectedRiskTooLow = (1..4).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Reject, not suitable for an AP: Risk too low") }
    val assessmentsRejectedOtherReasons = (1..5).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Reject, not suitable for an AP: Not suitable for other reasons") }
    val assessmentsRejectedRequestedInformationNotProvided = (1..6).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Reject, insufficient information: Requested information not provided by probation practitioner") }
    val assessmentsRejectedInsufficientContingencyPlan = (1..7).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Reject, insufficient information: Insufficient contingency plan") }
    val assessmentsRejectedInsufficientMoveOnPlan = (1..8).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Reject, insufficient information: Insufficient move on plan") }
    val assessmentsRejectedRiskTooHighToCommunity = (1..9).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Reject, risk too high (must be approved by an AP Area Manager (APAM): Risk to community") }
    val assessmentsRejectedRiskTooHighToOtherPeopleInAP = (1..10).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Reject, risk too high (must be approved by an AP Area Manager (APAM): Risk to other people in AP") }
    val assessmentsRejectedRiskToHighToStaff = (1..11).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Reject, risk too high (must be approved by an AP Area Manager (APAM): Risk to staff") }
    val assessmentsWithdrawn = (1..12).map { createDto("A0", decision = AssessmentDecision.REJECTED, rejectionReason = "Application withdrawn: Application withdrawn by the probation practitioner") }

    val assessments = listOf(
      assessmentsRejectedAccommodationNeedOnly,
      assessmentsRejectedNeedsCannotBeMet,
      assessmentsRejectedSupervisionPeriodTooShort,
      assessmentsRejectedRiskTooLow,
      assessmentsRejectedOtherReasons,
      assessmentsRejectedRequestedInformationNotProvided,
      assessmentsRejectedInsufficientContingencyPlan,
      assessmentsRejectedInsufficientMoveOnPlan,
      assessmentsRejectedRiskTooHighToCommunity,
      assessmentsRejectedRiskTooHighToOtherPeopleInAP,
      assessmentsRejectedRiskToHighToStaff,
      assessmentsWithdrawn,
    ).flatten()

    every { mockWorkingDayService.getWorkingDaysCount(any(), any()) } returns 1

    val results = ReferralsMetricsReportGenerator<TierCategory>(assessments, mockWorkingDayService).createReport(
      listOf(TierCategory.A0),
      ReferralsMetricsProperties(2023, 1),
    )

    assertThat(results[0][ReferralsMetricsReportRow::referralsRejectedAccommodationNeedOnly]).isEqualTo(assessmentsRejectedAccommodationNeedOnly.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsRejectedNeedsCannotBeMet]).isEqualTo(assessmentsRejectedNeedsCannotBeMet.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsRejectedSupervisionPeriodTooShort]).isEqualTo(assessmentsRejectedSupervisionPeriodTooShort.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsRejectedRiskTooLow]).isEqualTo(assessmentsRejectedRiskTooLow.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsRejectedOtherReasons]).isEqualTo(assessmentsRejectedOtherReasons.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsRejectedRequestedInformationNotProvided]).isEqualTo(assessmentsRejectedRequestedInformationNotProvided.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsRejectedInsufficientContingencyPlan]).isEqualTo(assessmentsRejectedInsufficientContingencyPlan.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsRejectedInsufficientMoveOnPlan]).isEqualTo(assessmentsRejectedInsufficientMoveOnPlan.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsRejectedRiskTooHighToCommunity]).isEqualTo(assessmentsRejectedRiskTooHighToCommunity.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsRejectedRiskTooHighToOtherPeopleInAP]).isEqualTo(assessmentsRejectedRiskTooHighToOtherPeopleInAP.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsRejectedRiskToHighToStaff]).isEqualTo(assessmentsRejectedRiskToHighToStaff.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsWithdrawn]).isEqualTo(assessmentsWithdrawn.size)
  }

  @Test
  fun `it counts the amount of referrals by release type`() {
    val assessmentsWithLicenceReleaseType = (1..2).map { createDto("A0", releaseType = "licence") }
    val assessmentsWithHdcReleaseType = (1..3).map { createDto("A0", releaseType = "hdc") }
    val assessmentsWithPssReleaseType = (1..4).map { createDto("A0", releaseType = "pss") }
    val assessmentsWithRotlReleaseType = (1..5).map { createDto("A0", releaseType = "rotl") }

    val assessments = listOf(
      assessmentsWithLicenceReleaseType,
      assessmentsWithHdcReleaseType,
      assessmentsWithPssReleaseType,
      assessmentsWithRotlReleaseType,
    ).flatten()

    every { mockWorkingDayService.getWorkingDaysCount(any(), any()) } returns 1

    val results = ReferralsMetricsReportGenerator<TierCategory>(assessments, mockWorkingDayService).createReport(
      listOf(TierCategory.A0),
      ReferralsMetricsProperties(2023, 1),
    )

    assertThat(results[0][ReferralsMetricsReportRow::referralsByReleaseTypeLicence]).isEqualTo(assessmentsWithLicenceReleaseType.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsByReleaseTypeHdc]).isEqualTo(assessmentsWithHdcReleaseType.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsByReleaseTypePSS]).isEqualTo(assessmentsWithPssReleaseType.size)
    assertThat(results[0][ReferralsMetricsReportRow::referralsByReleaseTypeParole]).isEqualTo(assessmentsWithRotlReleaseType.size)
  }

  @Test
  fun `it counts the number of referrals with information requests`() {
    val assessmentsWithInformationRequests = (1..7).map { createDto("A0", hasInformationRequests = true) }
    val assessmentsWithoutInformationRequests = (1..10).map { createDto("A0", hasInformationRequests = false) }

    val assessments = listOf(
      assessmentsWithInformationRequests,
      assessmentsWithoutInformationRequests,
    ).flatten()

    every { mockWorkingDayService.getWorkingDaysCount(any(), any()) } returns 1

    val results = ReferralsMetricsReportGenerator<TierCategory>(assessments, mockWorkingDayService).createReport(
      listOf(TierCategory.A0),
      ReferralsMetricsProperties(2023, 1),
    )

    assertThat(results[0][ReferralsMetricsReportRow::referralsWithInformationRequests]).isEqualTo(assessmentsWithInformationRequests.size)
  }

  private fun createDto(
    tier: String?,
    hasInformationRequests: Boolean = false,
    decision: AssessmentDecision = AssessmentDecision.ACCEPTED,
    rejectionReason: String? = null,
    releaseType: String = "license",
    applicationSubmittedAt: LocalDate = LocalDate.now(),
    assessmentSubmittedAt: LocalDate = LocalDate.now(),
    isPipe: Boolean = false,
    isEsap: Boolean = false,
  ) = ReferralsDataDto(
    tier = tier,
    isEsapApplication = isEsap,
    isPipeApplication = isPipe,
    decision = decision.toString(),
    applicationSubmittedAt = applicationSubmittedAt,
    assessmentSubmittedAt = assessmentSubmittedAt,
    rejectionRationale = rejectionReason,
    releaseType = releaseType,
    clarificationNoteCount = if (hasInformationRequests) {
      randomInt(1, 10)
    } else {
      0
    },
  )
}

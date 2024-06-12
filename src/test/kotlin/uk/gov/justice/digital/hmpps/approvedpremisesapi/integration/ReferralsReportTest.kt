package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Approved Premises`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Assessment for Temporary Accommodation`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.ReferralsMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApTypeCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ReferralsDataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ReferralsMetricsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TierCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ReferralsMetricsProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ReferralsReportTest : IntegrationTestBase() {
  @Autowired
  lateinit var realWorkingDayService: WorkingDayService

  @Test
  fun `Get referrals report returns 403 Forbidden if user does not have access`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/referrals-by-tier?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @ParameterizedTest
  @EnumSource(ServiceName::class, names = ["approvedPremises"], mode = EnumSource.Mode.EXCLUDE)
  fun `Get referrals report report returns not allowed if the service is not Approved Premises`(serviceName: ServiceName) {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/referrals-by-tier?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", serviceName.value)
        .exchange()
        .expectStatus()
        .is4xxClientError
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["referrals-by-tier", "referrals-by-ap-type"])
  fun `Get referrals report returns the correct data`(reportType: String) {
    `Given a User`(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { user, jwt ->
      govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

      val month = 4
      val year = 2023

      `Given an Assessment for Approved Premises`(
        allocatedToUser = user,
        createdByUser = user,
        createdAt = OffsetDateTime.of(LocalDate.of(year, 12, 4), LocalTime.MIDNIGHT, ZoneOffset.UTC),
      )

      `Given an Assessment for Approved Premises`(
        allocatedToUser = user,
        createdByUser = user,
        createdAt = OffsetDateTime.of(LocalDate.of(year, month, 4), LocalTime.MIDNIGHT, ZoneOffset.UTC),
        reallocated = true,
      )

      `Given an Assessment for Temporary Accommodation`(
        allocatedToUser = user,
        createdByUser = user,
        createdAt = OffsetDateTime.of(LocalDate.of(year, month, 4), LocalTime.MIDNIGHT, ZoneOffset.UTC),
      )

      val assessments = listOf(
        `Given an Assessment for Approved Premises`(
          allocatedToUser = user,
          createdByUser = user,
          createdAt = OffsetDateTime.of(LocalDate.of(year, month, 4), LocalTime.MIDNIGHT, ZoneOffset.UTC),
        ),
        `Given an Assessment for Approved Premises`(
          allocatedToUser = user,
          createdByUser = user,
          createdAt = OffsetDateTime.of(LocalDate.of(year, month, 15), LocalTime.MIDNIGHT, ZoneOffset.UTC),
        ),
        `Given an Assessment for Approved Premises`(
          allocatedToUser = user,
          createdByUser = user,
          createdAt = OffsetDateTime.of(LocalDate.of(year, month, 12), LocalTime.MIDNIGHT, ZoneOffset.UTC),
        ),
        `Given an Assessment for Approved Premises`(
          allocatedToUser = user,
          createdByUser = user,
          createdAt = OffsetDateTime.of(LocalDate.of(year, month, 3), LocalTime.MIDNIGHT, ZoneOffset.UTC),
        ),
        `Given an Assessment for Approved Premises`(
          allocatedToUser = user,
          createdByUser = user,
          createdAt = OffsetDateTime.of(LocalDate.of(year, month, 6), LocalTime.MIDNIGHT, ZoneOffset.UTC),
        ),
      ).map { it.first as ApprovedPremisesAssessmentEntity }

      var expectedDTOs = assessments.map {
        var applicationEntity = it.application as ApprovedPremisesApplicationEntity
        ReferralsDataDto(
          tier = applicationEntity.riskRatings?.tier?.value?.level,
          applicationSubmittedAt = applicationEntity.submittedAt?.toLocalDate(),
          assessmentSubmittedAt = it.submittedAt?.toLocalDate(),
          clarificationNoteCount = it.clarificationNotes.size,
          decision = it.decision.toString(),
          isEsapApplication = applicationEntity.isEsapApplication,
          isPipeApplication = applicationEntity.isPipeApplication,
          rejectionRationale = it.rejectionRationale,
          releaseType = applicationEntity.releaseType,
        )
      }

      val expectedDataFrame = if (reportType == "referrals-by-tier") {
        ReferralsMetricsReportGenerator<TierCategory>(expectedDTOs, realWorkingDayService)
          .createReport(TierCategory.entries, ReferralsMetricsProperties(year, month))
      } else {
        ReferralsMetricsReportGenerator<ApTypeCategory>(expectedDTOs, realWorkingDayService)
          .createReport(ApTypeCategory.entries, ReferralsMetricsProperties(year, month))
      }

      webTestClient.get()
        .uri("/reports/$reportType?year=$year&month=$month")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readExcel(it.responseBody!!.inputStream())
            .convertTo<ReferralsMetricsReportRow>(ExcessiveColumns.Remove)
          Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
        }
    }
  }
}

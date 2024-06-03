package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelinessEntityRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.PlacementMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.PlacementMetricsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TierCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementMetricsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PlacementMetricsTest : IntegrationTestBase() {
  @Autowired
  lateinit var applicationTimelinessEntityRepository: ApplicationTimelinessEntityRepository

  @Autowired
  lateinit var realWorkingDayService: WorkingDayService

  @Test
  fun `It returns 403 Forbidden if a user does not have the correct role`() {
    givenAUser { user, jwt ->
      webTestClient.get()
        .uri("/reports/placement-metrics?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @ParameterizedTest
  @EnumSource(ServiceName::class, names = ["approvedPremises"], mode = EnumSource.Mode.EXCLUDE)
  fun `Get daily metrics report for returns not allowed if the service is not Approved Premises`(serviceName: ServiceName) {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/placement-metrics?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", serviceName.value)
        .exchange()
        .expectStatus()
        .is4xxClientError
    }
  }

  @Test
  fun `it returns placement metrics`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { user, jwt ->
      val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val month = 4
      val year = 2022

      val submittedDate = OffsetDateTime.of(LocalDate.of(year, month, 22), LocalTime.MIDNIGHT, ZoneOffset.UTC)

      val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
        withApplicationSchema(applicationSchema)
        withSubmittedAt(submittedDate)
        withRiskRatings(
          PersonRisksFactory()
            .withTier(
              RiskWithStatus(
                RiskTier(
                  level = "A1",
                  lastUpdated = LocalDate.now(),
                ),
              ),
            ).produce(),
        )
      }

      domainEventFactory.produceAndPersist {
        withApplicationId(application.id)
        withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
        withOccurredAt(submittedDate)
      }

      domainEventFactory.produceAndPersist {
        withApplicationId(application.id)
        withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
        withOccurredAt(submittedDate.plusDays(3))
      }

      domainEventFactory.produceAndPersist {
        withApplicationId(application.id)
        withType(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)
        withOccurredAt(submittedDate.plusDays(22))
      }

      val timelinessEntities = applicationTimelinessEntityRepository.findAllForMonthAndYear(month, year)

      govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

      val expectedDataFrame = PlacementMetricsReportGenerator(timelinessEntities, realWorkingDayService)
        .createReport(TierCategory.entries, PlacementMetricsReportProperties(month, year))

      webTestClient.get()
        .uri("/reports/placement-metrics?year=$year&month=$month")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readExcel(it.responseBody!!.inputStream())
            .convertTo<PlacementMetricsReportRow>(ExcessiveColumns.Remove)
          assertThat(actual).isEqualTo(expectedDataFrame)
        }
    }
  }
}

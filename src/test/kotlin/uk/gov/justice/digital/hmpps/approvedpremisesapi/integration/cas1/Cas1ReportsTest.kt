package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_CRU_MEMBER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_REPORT_VIEWER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.Cas1OutOfServiceBedsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.Cas1OutOfServiceBedReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1ReportsTest : IntegrationTestBase() {
  val cas1Report: String = Cas1ReportName.lostBeds.value
  val approvedPremisesServiceName: String = ServiceName.approvedPremises.value

  @Test
  fun `Get report returns 400 if query parameters are missing`() {
    givenAUser(roles = listOf(CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/$cas1Report")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", approvedPremisesServiceName)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("Missing required query parameter year")
    }
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 13])
  fun `Get report returns 400 if month provided is invalid`(month: Int) {
    givenAUser(roles = listOf(CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/$cas1Report?year=2024&month=$month")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", approvedPremisesServiceName)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
    }
  }

  @Test
  fun `Get report returns 405 Not Allowed if serviceName header is not approvedPremises`() {
    givenAUser(roles = listOf(CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/$cas1Report?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("$.detail").isEqualTo("This endpoint only supports CAS1")
    }
  }

  @Test
  fun `Get report returns 403 Forbidden if user does not have CAS1 report access`() {
    givenAUser(roles = listOf(CAS1_CRU_MEMBER)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/$cas1Report?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", approvedPremisesServiceName)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get report returns Server Error if report specified is invalid`() {
    givenAUser(roles = listOf(CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/doesnotexist")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", approvedPremisesServiceName)
        .exchange()
        .expectStatus()
        .is5xxServerError
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas1ReportName::class)
  fun `Get report returns OK response if user role is valid and parameters are valid `(reportName: Cas1ReportName) {
    givenAUser(roles = listOf(CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/$reportName?year=2024&month=1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", approvedPremisesServiceName)
        .exchange()
        .expectStatus()
        .isOk
    }
  }

  @Nested
  inner class GetOutOfServiceBedsReport {
    private val outOfServiceBedsEndpoint = "/cas1/reports/${Cas1ReportName.outOfServiceBeds.value}"

    @Autowired
    lateinit var realOutOfServiceBedRepository: Cas1OutOfServiceBedRepository

    @Test
    fun `Get out-of-service beds report returns OK with correct body`() {
      givenAUser(roles = listOf(CAS1_REPORT_VIEWER)) { userEntity, jwt ->
        givenAnOffender { _, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }

          val bed1 = bedEntityFactory.produceAndPersist {
            withRoom(
              roomEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
          }

          val bed2 = bedEntityFactory.produceAndPersist {
            withRoom(
              roomEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
          }

          val bed3 = bedEntityFactory.produceAndPersist {
            withRoom(
              roomEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
          }

          cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withBed(bed1)
          }.apply {
            this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withCreatedBy(userEntity)
              withOutOfServiceBed(this@apply)
              withStartDate(LocalDate.of(2023, 4, 5))
              withEndDate(LocalDate.of(2023, 7, 8))
              withYieldedReason {
                cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()
              }
            }
          }

          cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withBed(bed2)
          }.apply {
            this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withCreatedBy(userEntity)
              withOutOfServiceBed(this@apply)
              withStartDate(LocalDate.of(2023, 4, 12))
              withEndDate(LocalDate.of(2023, 7, 5))
              withYieldedReason {
                cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()
              }
            }
          }

          cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withBed(bed3)
          }.apply {
            this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withCreatedBy(userEntity)
              withOutOfServiceBed(this@apply)
              withStartDate(LocalDate.of(2023, 4, 1))
              withEndDate(LocalDate.of(2023, 7, 5))
              withYieldedReason {
                cas1OutOfServiceBedReasonEntityFactory.produceAndPersist()
              }
            }

            this.cancellation = cas1OutOfServiceBedCancellationEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withOutOfServiceBed(this@apply)
            }
          }

          val expectedDataFrame = Cas1OutOfServiceBedsReportGenerator(realOutOfServiceBedRepository)
            .createReport(
              listOf(bed1, bed2),
              Cas1ReportService.MonthSpecificReportParams(2023, 4),
            )

          webTestClient.get()
            .uri("$outOfServiceBedsEndpoint?year=2023&month=4")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.approvedPremises.value)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith {
              val actual = DataFrame
                .readExcel(it.responseBody!!.inputStream())
                .convertTo<Cas1OutOfServiceBedReportRow>(ExcessiveColumns.Remove)
              Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
            }
        }
      }
    }
  }
}

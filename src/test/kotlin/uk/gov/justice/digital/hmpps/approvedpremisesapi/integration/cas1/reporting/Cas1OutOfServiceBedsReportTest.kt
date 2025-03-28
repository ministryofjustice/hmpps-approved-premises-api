package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.reporting

import org.assertj.core.api.Assertions
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_REPORT_VIEWER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.Cas1OutOfServiceBedsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.Cas1OutOfServiceBedsReportGenerator.Cas1BedIdentifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.Cas1OutOfServiceBedReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1OutOfServiceBedsReportTest : IntegrationTestBase() {
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
          withName("bed1")
          withRoom(
            roomEntityFactory.produceAndPersist {
              withName("room1")
              withPremises(premises)
            },
          )
        }

        val bed2 = bedEntityFactory.produceAndPersist {
          withName("bed2")
          withRoom(
            roomEntityFactory.produceAndPersist {
              withName("room2")
              withPremises(premises)
            },
          )
        }

        val bed3 = bedEntityFactory.produceAndPersist {
          withName("bed3")
          withRoom(
            roomEntityFactory.produceAndPersist {
              withName("room3")
              withPremises(premises)
            },
          )
        }

        val bed4 = bedEntityFactory.produceAndPersist {
          withName("bed4")
          withRoom(
            roomEntityFactory.produceAndPersist {
              withName("room4")
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

          cas1OutOfServiceBedEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withBed(bed4)
          }.apply {
            this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
              withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
              withCreatedBy(userEntity)
              withOutOfServiceBed(this@apply)
              withStartDate(LocalDate.of(2023, 4, 1))
              withEndDate(LocalDate.of(2023, 7, 5))
              withReason(cas1OutOfServiceBedReasonTestRepository.getReferenceById(Cas1OutOfServiceBedReasonRepository.BED_ON_HOLD_REASON_ID))
            }
          }

          this.cancellation = cas1OutOfServiceBedCancellationEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withOutOfServiceBed(this@apply)
          }
        }

        /*
        Note - this test is mostly redundant because it calls the report generator directly to determine
        what the endpoint should be returning. So it's only checking that the controller is calling
        the report generator, not that it returns an expected result

        This is better tested by [Cas1OutOfServiceBedReportGeneratorTest] which could be merged
        with this test
         */
        val expectedDataFrame = Cas1OutOfServiceBedsReportGenerator(realOutOfServiceBedRepository)
          .createReport(
            listOf(Cas1BedIdentifier(bed1.id), Cas1BedIdentifier(bed2.id)),
            Cas1ReportService.MonthSpecificReportParams(2023, 4),
          ).sortBy { row -> row["bedName"] }

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
              .sortBy { row -> row["bedName"] }

            Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }
}

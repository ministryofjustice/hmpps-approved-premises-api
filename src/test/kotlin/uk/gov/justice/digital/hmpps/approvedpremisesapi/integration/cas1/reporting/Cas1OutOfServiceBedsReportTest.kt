package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.reporting

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_REPORT_VIEWER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.Cas1OutOfServiceBedReportRow
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
        val premises = givenAnApprovedPremises(
          name = "ap name",
          region = givenAProbationRegion(name = "the region"),
        )

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

        val oosbRecordBed1 = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed1)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(userEntity)
            withReferenceNumber("ref1")
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.of(2023, 4, 5))
            withEndDate(LocalDate.of(2023, 7, 8))
            withReason(
              cas1OutOfServiceBedReasonEntityFactory.produceAndPersist {
                withName("Reason1")
              },
            )
          }
        }

        val oosbRecordBed2 = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed2)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
            withCreatedBy(userEntity)
            withReferenceNumber("ref2")
            withOutOfServiceBed(this@apply)
            withStartDate(LocalDate.of(2023, 4, 12))
            withEndDate(LocalDate.of(2023, 7, 5))
            withReason(
              cas1OutOfServiceBedReasonEntityFactory.produceAndPersist {
                withName("Reason2")
              },
            )
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
              .convertTo<Cas1OutOfServiceBedReportRow>(ExcessiveColumns.Fail)
              .sortBy { row -> row["bedName"] }

            val actualRows = actual.toList()
            assertThat(actualRows).hasSize(2)
            assertThat(actualRows[0].roomName).isEqualTo("room1")
            assertThat(actualRows[0].bedName).isEqualTo("bed1")
            assertThat(actualRows[0].id).isEqualTo(oosbRecordBed1.id.toString())
            assertThat(actualRows[0].workOrderId).isEqualTo("ref1")
            assertThat(actualRows[0].region).isEqualTo("the region")
            assertThat(actualRows[0].ap).isEqualTo("ap name")
            assertThat(actualRows[0].reason).isEqualTo("Reason1")
            assertThat(actualRows[0].startDate).isEqualTo(LocalDate.of(2023, 4, 5))
            assertThat(actualRows[0].endDate).isEqualTo(LocalDate.of(2023, 7, 8))
            assertThat(actualRows[0].lengthDays).isEqualTo(26)

            assertThat(actualRows[1].roomName).isEqualTo("room2")
            assertThat(actualRows[1].bedName).isEqualTo("bed2")
            assertThat(actualRows[1].id).isEqualTo(oosbRecordBed2.id.toString())
            assertThat(actualRows[1].workOrderId).isEqualTo("ref2")
            assertThat(actualRows[1].ap).isEqualTo("ap name")
            assertThat(actualRows[1].reason).isEqualTo("Reason2")
            assertThat(actualRows[1].startDate).isEqualTo(LocalDate.of(2023, 4, 12))
            assertThat(actualRows[1].endDate).isEqualTo(LocalDate.of(2023, 7, 5))
            assertThat(actualRows[1].lengthDays).isEqualTo(19)
          }
      }
    }
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.reporting

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.sortBy
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_REPORT_VIEWER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_REPORT_VIEWER_WITH_PII
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1OutOfServiceBedsReportTest : InitialiseDatabasePerClassTestBase() {
  lateinit var oosbRecordBed1: Cas1OutOfServiceBedEntity
  lateinit var oosbRecordBed2: Cas1OutOfServiceBedEntity

  @BeforeAll
  fun setup() {
    givenAUser(roles = listOf(CAS1_REPORT_VIEWER)) { userEntity, _ ->
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

        oosbRecordBed1 = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed1)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.parse("2020-01-03T10:15:30+01:00").roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
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
            withNotes("Notes on OOSB1 Revision 1")
          }
        }

        oosbRecordBed2 = cas1OutOfServiceBedEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withBed(bed2)
        }.apply {
          this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
            withCreatedAt(OffsetDateTime.parse("2020-12-03T10:15:30+01:00").roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
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
            withNotes("Notes on OOSB2 Revision 1")
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
      }
    }
  }

  @Test
  fun `Get out-of-service beds report without PII requires report viewer role`() {
    val (_, jwt) = givenAUser(roles = listOf())

    webTestClient.get()
      .uri("/cas1/reports/${Cas1ReportName.outOfServiceBeds.value}?year=2023&month=4")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get out-of-service beds report without PII`() {
    val (_, jwt) = givenAUser(roles = listOf(CAS1_REPORT_VIEWER))
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 4, 30)

    webTestClient.get()
      .uri("/cas1/reports/${Cas1ReportName.outOfServiceBeds.value}?startDate=$startDate&endDate=$endDate")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().valuesMatch(
        "content-disposition",
        "attachment; filename=\"out-of-service-beds-$startDate-to-$endDate-\\d{8}_\\d{4}.csv\"",
      )
      .expectBody()
      .consumeWith {
        val actual = DataFrame
          .readCSV(it.responseBody!!.inputStream())
          .convertTo<Cas1OutOfServiceBedReportRowWithoutPii>(ExcessiveColumns.Fail)
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

  @Test
  fun `Get out-of-service beds report with PII requires report viewer with PII role`() {
    val (_, jwt) = givenAUser(roles = listOf(CAS1_REPORT_VIEWER))

    webTestClient.get()
      .uri("/cas1/reports/${Cas1ReportName.outOfServiceBedsWithPii.value}?year=2023&month=4")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isForbidden()
  }

  @Test
  fun `Get out-of-service beds report with PII`() {
    val (_, jwt) = givenAUser(roles = listOf(CAS1_REPORT_VIEWER_WITH_PII))
    val startDate = LocalDate.of(2023, 4, 1)
    val endDate = LocalDate.of(2023, 4, 30)

    webTestClient.get()
      .uri("/cas1/reports/${Cas1ReportName.outOfServiceBedsWithPii.value}?startDate=$startDate&endDate=$endDate")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader().valuesMatch(
        "content-disposition",
        "attachment; filename=\"out-of-service-beds-with-pii-$startDate-to-$endDate-\\d{8}_\\d{4}.csv\"",
      )
      .expectBody()
      .consumeWith {
        val actual = DataFrame
          .readCSV(it.responseBody!!.inputStream())
          .convertTo<Cas1OutOfServiceBedReportRowWithPii>(ExcessiveColumns.Fail)
          .sortBy { row -> row["bedName"] }

        val actualRows = actual.toList()
        assertThat(actualRows).hasSize(2)
        assertThat(actualRows[0].notes).isEqualTo(
          """Date/Time: Friday 3 January 2020
Reason: Reason1
Notes: Notes on OOSB1 Revision 1""",
        )
        assertThat(actualRows[1].notes).isEqualTo(
          """Date/Time: Thursday 3 December 2020
Reason: Reason2
Notes: Notes on OOSB2 Revision 1""",
        )
      }
  }
}

data class Cas1OutOfServiceBedReportRowWithoutPii(
  val roomName: String,
  val bedName: String,
  val id: String,
  val workOrderId: String?,
  val region: String,
  val ap: String,
  val reason: String,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val lengthDays: Int,
)

data class Cas1OutOfServiceBedReportRowWithPii(
  val roomName: String,
  val bedName: String,
  val id: String,
  val workOrderId: String?,
  val region: String,
  val ap: String,
  val reason: String,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val lengthDays: Int,
  val notes: String,
)

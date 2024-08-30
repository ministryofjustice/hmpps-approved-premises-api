package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

class MigrateCas1TruncateOosbTest : MigrationJobTestBase() {

  @Autowired
  lateinit var outOfServiceBedService: Cas1OutOfServiceBedService

  @Test
  fun `ensure oosb records with end dates after bed end date are updated`() {
    clock.setNow(LocalDateTime.of(2023, 6, 1, 12, 0, 0))

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val bedNoEndDate = createBed(premises, endDate = null)
    val activeOosbRecordBedHasNoEndDate = createOosbRecord(
      bed = bedNoEndDate,
      startDate = LocalDate.parse("2022-07-15"),
      endDate = LocalDate.parse("2099-08-15"),
    )

    val bedWithEndDate = createBed(premises, endDate = LocalDate.parse("2023-03-01"))
    val activeOosbRecordBeforeBedEndDate = createOosbRecord(
      bed = bedWithEndDate,
      startDate = LocalDate.parse("2022-07-15"),
      endDate = LocalDate.parse("2023-02-28"),
    )

    val activeOosbRecordAfterBedEndDate = createOosbRecord(
      bed = bedWithEndDate,
      startDate = LocalDate.parse("2022-07-15"),
      endDate = LocalDate.parse("2023-03-02"),
    )

    val cancelledOosbRecordAfterBedEndDate = createOosbRecord(
      bed = bedWithEndDate,
      startDate = LocalDate.parse("2022-07-15"),
      endDate = LocalDate.parse("2024-03-02"),
    )
    outOfServiceBedService.cancelOutOfServiceBed(cancelledOosbRecordAfterBedEndDate, notes = "test")

    migrationJobService.runMigrationJob(MigrationJobType.cas1TruncateOosbForBedsWithEndDate)

    assertThat(cas1OutOfServiceBedTestRepository.findByIdOrNull(activeOosbRecordBedHasNoEndDate.id)!!.revisionHistory).hasSize(1)
    assertThat(cas1OutOfServiceBedTestRepository.findByIdOrNull(activeOosbRecordBeforeBedEndDate.id)!!.revisionHistory).hasSize(1)

    val updatedOosbRecordAfterBedEndDate = cas1OutOfServiceBedTestRepository.findByIdOrNull(activeOosbRecordAfterBedEndDate.id)!!
    assertThat(updatedOosbRecordAfterBedEndDate.revisionHistory).hasSize(2)
    assertThat(updatedOosbRecordAfterBedEndDate.startDate).isEqualTo(LocalDate.parse("2022-07-15"))
    assertThat(updatedOosbRecordAfterBedEndDate.endDate).isEqualTo(LocalDate.parse("2023-02-28"))
    assertThat(updatedOosbRecordAfterBedEndDate.reason).isEqualTo(activeOosbRecordAfterBedEndDate.reason)
    assertThat(updatedOosbRecordAfterBedEndDate.referenceNumber).isNull()
    assertThat(updatedOosbRecordAfterBedEndDate.notes).isEqualTo("End date has been automatically updated by application support as the bed has been removed as of 2023-03-01")

    assertThat(cas1OutOfServiceBedTestRepository.findByIdOrNull(cancelledOosbRecordAfterBedEndDate.id)!!.revisionHistory).hasSize(1)
  }

  fun createOosbRecord(bed: BedEntity, startDate: LocalDate, endDate: LocalDate) =
    cas1OutOfServiceBedEntityFactory.produceAndPersist {
      withBed(bed)
    }.apply {
      this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
        withCreatedAt(OffsetDateTime.now())
        withCreatedBy(null)
        withOutOfServiceBed(this@apply)
        withStartDate(startDate)
        withEndDate(endDate)
        withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
      }
    }

  fun createBed(premises: PremisesEntity, endDate: LocalDate?) = bedEntityFactory.produceAndPersist {
    withYieldedRoom {
      roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises }
      }
    }
    withEndDate { endDate }
  }
}

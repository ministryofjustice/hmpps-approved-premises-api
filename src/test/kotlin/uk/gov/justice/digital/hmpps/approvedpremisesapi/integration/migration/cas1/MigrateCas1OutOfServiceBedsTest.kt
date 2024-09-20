package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionChangeType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

class MigrateCas1OutOfServiceBedsTest : MigrationJobTestBase() {
  private lateinit var approvedPremises: ApprovedPremisesEntity
  private lateinit var approvedPremisesBed: BedEntity

  private lateinit var temporaryAccommodationPremises: TemporaryAccommodationPremisesEntity
  private lateinit var temporaryAccommodationBed: BedEntity

  @BeforeEach
  fun setup() {
    val probationRegion = `Given a Probation Region`()

    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

    approvedPremises = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
    }

    approvedPremisesBed = bedEntityFactory.produceAndPersist {
      withYieldedRoom {
        roomEntityFactory.produceAndPersist {
          withPremises(approvedPremises)
        }
      }
    }

    temporaryAccommodationPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
      withLocalAuthorityArea(localAuthorityArea)
    }

    temporaryAccommodationBed = bedEntityFactory.produceAndPersist {
      withYieldedRoom {
        roomEntityFactory.produceAndPersist {
          withPremises(temporaryAccommodationPremises)
        }
      }
    }
  }

  @Test
  fun `Job migrates expected set of lost beds`() {
    val lostBedReason = lostBedReasonEntityFactory.produceAndPersist {
      withServiceScope(ServiceName.approvedPremises.value)
    }

    // outOfServiceBedReason
    cas1OutOfServiceBedReasonEntityFactory.produceAndPersist {
      withId(lostBedReason.id)
      withName(lostBedReason.name)
      withIsActive(lostBedReason.isActive)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
    }

    val approvedPremisesLostBeds = lostBedsEntityFactory.produceAndPersistMultiple(10) {
      withYieldedReason { lostBedReason }
      withPremises(approvedPremises)
      withBed(approvedPremisesBed)
    }

    // temporaryAccommodationLostBeds
    lostBedsEntityFactory.produceAndPersistMultiple(10) {
      withYieldedReason { lostBedReason }
      withPremises(temporaryAccommodationPremises)
      withBed(temporaryAccommodationBed)
    }

    assertThat(cas1OutOfServiceBedTestRepository.findAll()).isEmpty()

    migrationJobService.runMigrationJob(MigrationJobType.cas1LostBedsToOutOfServiceBeds)

    assertThat(cas1OutOfServiceBedTestRepository.findAll().map { it.id })
      .containsExactlyInAnyOrder(*approvedPremisesLostBeds.map { it.id }.toTypedArray())
  }

  @Test
  fun `Job migrates the expected data for an active lost bed`() {
    val lostBedReason = lostBedReasonEntityFactory.produceAndPersist {
      withServiceScope(ServiceName.approvedPremises.value)
    }

    val outOfServiceBedReason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist {
      withId(lostBedReason.id)
      withName(lostBedReason.name)
      withIsActive(lostBedReason.isActive)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
    }

    val lostBed = lostBedsEntityFactory.produceAndPersist {
      withYieldedReason { lostBedReason }
      withPremises(approvedPremises)
      withBed(approvedPremisesBed)
    }

    assertThat(cas1OutOfServiceBedTestRepository.findAll()).isEmpty()

    migrationJobService.runMigrationJob(MigrationJobType.cas1LostBedsToOutOfServiceBeds)

    val result = cas1OutOfServiceBedTestRepository.findAll().first()

    assertThat(result.id).isEqualTo(lostBed.id)
    assertThat(result.premises.id).isEqualTo(lostBed.premises.id)
    assertThat(result.bed.id).isEqualTo(lostBed.bed.id)
    assertThat(result.cancellation).isNull()
    assertThat(result.revisionHistory).hasSize(1)
    assertThat(result.latestRevision.revisionType).isEqualTo(Cas1OutOfServiceBedRevisionType.INITIAL)
    assertThat(result.latestRevision.startDate).isEqualTo(lostBed.startDate)
    assertThat(result.latestRevision.endDate).isEqualTo(lostBed.endDate)
    assertThat(result.latestRevision.notes).isEqualTo(lostBed.notes)
    assertThat(result.latestRevision.reason).isEqualTo(outOfServiceBedReason)
    assertThat(result.latestRevision.createdBy).isNull()
    assertThat(result.latestRevision.changeTypePacked).isEqualTo(Cas1OutOfServiceBedRevisionChangeType.NO_CHANGE)
  }

  @Test
  fun `Job migrates the expected data for a cancelled lost bed`() {
    val lostBedReason = lostBedReasonEntityFactory.produceAndPersist {
      withServiceScope(ServiceName.approvedPremises.value)
    }

    val outOfServiceBedReason = cas1OutOfServiceBedReasonEntityFactory.produceAndPersist {
      withId(lostBedReason.id)
      withName(lostBedReason.name)
      withIsActive(lostBedReason.isActive)
      withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
    }

    val lostBed = lostBedsEntityFactory.produceAndPersist {
      withYieldedReason { lostBedReason }
      withPremises(approvedPremises)
      withBed(approvedPremisesBed)
    }

    lostBed.cancellation = lostBedCancellationEntityFactory.produceAndPersist {
      withLostBed(lostBed)
    }

    assertThat(cas1OutOfServiceBedTestRepository.findAll()).isEmpty()

    migrationJobService.runMigrationJob(MigrationJobType.cas1LostBedsToOutOfServiceBeds)

    val result = cas1OutOfServiceBedTestRepository.findAll().first()

    assertThat(result.id).isEqualTo(lostBed.id)
    assertThat(result.premises.id).isEqualTo(lostBed.premises.id)
    assertThat(result.bed.id).isEqualTo(lostBed.bed.id)
    assertThat(result.cancellation).isNotNull
    assertThat(result.cancellation!!.id).isEqualTo(lostBed.cancellation!!.id)
    assertThat(result.cancellation!!.createdAt).isEqualTo(lostBed.cancellation!!.createdAt)
    assertThat(result.cancellation!!.notes).isEqualTo(lostBed.cancellation!!.notes)
    assertThat(result.revisionHistory).hasSize(1)
    assertThat(result.latestRevision.revisionType).isEqualTo(Cas1OutOfServiceBedRevisionType.INITIAL)
    assertThat(result.latestRevision.startDate).isEqualTo(lostBed.startDate)
    assertThat(result.latestRevision.endDate).isEqualTo(lostBed.endDate)
    assertThat(result.latestRevision.notes).isEqualTo(lostBed.notes)
    assertThat(result.latestRevision.reason).isEqualTo(outOfServiceBedReason)
    assertThat(result.latestRevision.createdBy).isNull()
    assertThat(result.latestRevision.changeTypePacked).isEqualTo(Cas1OutOfServiceBedRevisionChangeType.NO_CHANGE)
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionChangeType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("detekt:LongParameterList")
class Cas1LostBedsToOutOfServiceBedsMigrationJob(
  private val lostBedRepository: LostBedMigrationRepository,
  private val outOfServiceBedRepository: Cas1OutOfServiceBedRepository,
  private val outOfServiceBedCancellationRepository: Cas1OutOfServiceBedCancellationRepository,
  private val outOfServiceBedReasonRepository: Cas1OutOfServiceBedReasonRepository,
  private val outOfServiceBedRevisionRepository: Cas1OutOfServiceBedRevisionRepository,
  private val transactionTemplate: TransactionTemplate,
  private val logger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  override fun process() {
    val cas1LostBeds = lostBedRepository.findAllCas1ForMigration()
    logger.info("Starting migrating ${cas1LostBeds.count()} CAS1 lost beds")

    cas1LostBeds.forEach { lostBed ->
      transactionTemplate.executeWithoutResult {
        createOutOfServiceBed(lostBed)
      }
    }

    logger.info("Finished migrating ${cas1LostBeds.count()} CAS1 lost beds")
  }

  private fun createOutOfServiceBed(lostBed: LostBedsEntity) {
    val reason = outOfServiceBedReasonRepository.findByIdOrNull(lostBed.reason.id)
      ?: throw MigrationException(
        "Could not find out-of-service bed reason with ID ${lostBed.reason.id}. " +
          "Make sure that the 'update_cas1_out_of_service_bed_reasons' migration job has been run first.",
      )

    logger.info(
      "Migrating CAS1 lost bed {id: ${lostBed.id}, premises: ${lostBed.premises.id}, bed: ${lostBed.bed.id}}",
    )

    val outOfServiceBed = outOfServiceBedRepository.saveAndFlush(
      Cas1OutOfServiceBedEntity(
        id = lostBed.id,
        premises = lostBed.premises as ApprovedPremisesEntity,
        bed = lostBed.bed,
        createdAt = OffsetDateTime.now(),
        cancellation = null,
        revisionHistory = mutableListOf(),
      ),
    )

    logger.info(
      "Created CAS1 out-of-service bed {" +
        "id: ${outOfServiceBed.id}, " +
        "premises: ${outOfServiceBed.premises.id}, " +
        "bed: ${outOfServiceBed.bed.id}" +
        "}",
    )

    outOfServiceBed.apply {
      this.cancellation = lostBed.cancellation?.let {
        logger.info(
          "Migrating lost bed cancellation for ${lostBed.id} {" +
            "id: ${it.id}, " +
            "createdAt: ${it.createdAt}, " +
            "notes: ${it.notes}" +
            "}",
        )

        val cancellation = outOfServiceBedCancellationRepository.saveAndFlush(
          Cas1OutOfServiceBedCancellationEntity(
            id = it.id,
            createdAt = it.createdAt,
            notes = it.notes,
            outOfServiceBed = this,
          ),
        )

        logger.info(
          "Created out-of-service bed cancellation for ${outOfServiceBed.id} {" +
            "id: ${cancellation.id}, " +
            "createdAt: ${cancellation.createdAt}, " +
            "notes: ${cancellation.notes}" +
            "}",
        )

        cancellation
      }

      logger.info(
        "Migrating initial details for ${lostBed.id} {" +
          "startDate: ${lostBed.startDate}, " +
          "endDate: ${lostBed.endDate}, " +
          "referenceNumber: ${lostBed.referenceNumber}, " +
          "notes: ${lostBed.notes}, " +
          "reason: ${lostBed.reason.id}" +
          "}",
      )

      val initialRevision = outOfServiceBedRevisionRepository.saveAndFlush(
        Cas1OutOfServiceBedRevisionEntity(
          id = UUID.randomUUID(),
          createdAt = this.createdAt,
          revisionType = Cas1OutOfServiceBedRevisionType.INITIAL,
          startDate = lostBed.startDate,
          endDate = lostBed.endDate,
          referenceNumber = lostBed.referenceNumber,
          notes = lostBed.notes,
          reason = reason,
          outOfServiceBed = this,
          createdBy = null,
          changeTypePacked = Cas1OutOfServiceBedRevisionChangeType.NO_CHANGE,
        ),
      )

      this.revisionHistory += initialRevision

      logger.info(
        "Created initial revision for ${outOfServiceBed.id} {" +
          "startDate: ${initialRevision.startDate}, " +
          "endDate: ${initialRevision.endDate}, " +
          "referenceNumber: ${initialRevision.referenceNumber}, " +
          "notes: ${initialRevision.notes}, " +
          "reason: ${initialRevision.reason.id}" +
          "}",
      )
    }
  }
}

@Repository
interface LostBedMigrationRepository : JpaRepository<LostBedsEntity, UUID> {
  @Query("SELECT lb FROM ApprovedPremisesEntity ap INNER JOIN LostBedsEntity lb ON lb.premises = ap")
  fun findAllCas1ForMigration(): List<LostBedsEntity>
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationInBatchesJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.util.UUID

@Component
class Cas3VoidBedspaceJob(
  private val cas3BedspacesRepository: Cas3BedspacesRepository,
  private val cas3VoidBedspaceRepository: Cas3VoidBedspaceMigrationRepository,
  private val migrationLogger: MigrationLogger,
  transactionTemplate: TransactionTemplate,
) : MigrationInBatchesJob(migrationLogger, transactionTemplate) {
  override val shouldRunInTransaction = false

  override fun process(pageSize: Int) {
    migrationLogger.info("Beginng void bedspace migration")
    val voidBedspaces = cas3VoidBedspaceRepository.findNonMigratedBedspaces()
    super.processInBatches(items = voidBedspaces, batchSize = pageSize) { voidBedspaces ->

      val bedspaceMap = cas3BedspacesRepository.findAllById(voidBedspaces.map { it.bed!!.id }).associateBy { it.id }
      voidBedspaces.forEach { voidBedspace ->
        voidBedspace.bedspace = bedspaceMap[voidBedspace.bed!!.id]
        voidBedspace.cancellationDate = voidBedspace.cancellation?.createdAt
        voidBedspace.cancellationNotes = voidBedspace.cancellation?.notes
      }
      cas3VoidBedspaceRepository.saveAllAndFlush(voidBedspaces)
    }
    migrationLogger.info("Finished void bedspace migration")
  }
}

@Repository
interface Cas3VoidBedspaceMigrationRepository : JpaRepository<Cas3VoidBedspaceEntity, UUID> {
  @Query("select bs from Cas3VoidBedspaceEntity bs where bs.bedspace is null")
  fun findNonMigratedBedspaces(): List<Cas3VoidBedspaceEntity>
}

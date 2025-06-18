package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger

@Component
class Cas3VoidBedspaceCancellationJob(
  private val cas3BedspacesRepository: Cas3BedspacesRepository,
  private val cas3VoidBedspaceRepository: Cas3VoidBedspacesRepository,
  private val cas3VoidBedspaceCancellationRepository: Cas3VoidBedspaceCancellationRepository,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    var pageNumber = 0
    var hasNext = true

    while (hasNext) {
      val page = cas3VoidBedspaceCancellationRepository.findAll(PageRequest.of(pageNumber, pageSize))
      val cancellations = page.content
      hasNext = page.hasNext()

      if (cancellations.isEmpty()) break

      val voidBedspaces = cancellations.map { it.voidBedspace }
      val bedIds = voidBedspaces.map { it.bed.id }
      val bedspacesMap = cas3BedspacesRepository.findAllById(bedIds).associateBy { it.id }

      migrationLogger.info("Processing page $pageNumber with ${cancellations.size} BedspaceVoidCancellation entities")

      voidBedspaces.forEach {
        it.bedspace = bedspacesMap[it.bed.id]
        it.cancellationDate = it.cancellation!!.createdAt
        it.cancellationNotes = it.cancellation?.notes
      }

      migrationLogger.info("Adding cancellation data for void bedspaces: ${voidBedspaces.map { it.id }}")
      cas3VoidBedspaceRepository.saveAllAndFlush(voidBedspaces)

      pageNumber++
    }

    migrationLogger.info("Finished adding cancellation data")
  }
}

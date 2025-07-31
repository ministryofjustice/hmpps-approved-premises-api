package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration

import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService

@Component
class Cas3UpdateApplicationOffenderNameJob(
  private val applicationRepository: ApplicationRepository,
  private val offenderService: OffenderService,
  private val entityManager: EntityManager,
  private val migrationLogger: MigrationLogger,
) : MigrationJob() {
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
  override fun process(pageSize: Int) {
    var page = 1
    var hasNext = true
    var slice: Slice<TemporaryAccommodationApplicationEntity>
    var offendersCrn = setOf<String>()

    try {
      while (hasNext) {
        migrationLogger.info("Getting page $page for max page size $pageSize")
        slice = applicationRepository.findAllTemporaryAccommodationApplicationsAndNameNull(TemporaryAccommodationApplicationEntity::class.java, PageRequest.of(0, pageSize))

        offendersCrn = slice.map { it.crn }.toSet()

        migrationLogger.info("Updating offenders name with crn ${offendersCrn.map { it }}")

        val personInfos = splitAndRetrievePersonInfo(pageSize, offendersCrn)

        slice.content.forEach {
          val personInfo = personInfos[it.crn] ?: PersonSummaryInfoResult.Unknown(it.crn)
          updateApplication(personInfo, it)
        }

        entityManager.clear()
        hasNext = slice.hasNext()
        page += 1
      }
    } catch (exception: Exception) {
      migrationLogger.error("Unable to update offenders name with crn ${offendersCrn.joinToString { "," }}", exception)
    }
  }

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught", "TooGenericExceptionThrown")
  private fun updateApplication(personInfo: PersonSummaryInfoResult, it: TemporaryAccommodationApplicationEntity) {
    try {
      val offenderName = when (personInfo) {
        is PersonSummaryInfoResult.Success.Full -> "${personInfo.summary.name.forename} ${personInfo.summary.name.surname}"
        is PersonSummaryInfoResult.NotFound, is PersonSummaryInfoResult.Unknown -> throw Exception("Offender not found")
        is PersonSummaryInfoResult.Success.Restricted -> throw Exception("You are not authorized")
      }

      it.name = offenderName
      applicationRepository.save(it)
    } catch (exception: Exception) {
      migrationLogger.error("Unable to update offender name with crn ${it.crn} for the application ${it.id}", exception)
    }
  }

  private fun splitAndRetrievePersonInfo(pageSize: Int, crns: Set<String>): Map<String, PersonSummaryInfoResult> = offenderService.getPersonSummaryInfoResultsInBatches(
    crns = crns,
    laoStrategy = LaoStrategy.NeverRestricted,
    batchSize = pageSize,
  ).associateBy { it.crn }
}

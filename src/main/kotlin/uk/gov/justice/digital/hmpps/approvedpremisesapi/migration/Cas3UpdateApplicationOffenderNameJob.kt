package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration

import org.apache.commons.collections4.ListUtils
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.util.stream.Collectors
import javax.persistence.EntityManager

class Cas3UpdateApplicationOffenderNameJob(
  private val applicationRepository: ApplicationRepository,
  private val offenderService: OffenderService,
  private val entityManager: EntityManager,
  private val pageSize: Int,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = false

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
  override fun process() {
    var page = 1
    var hasNext = true
    var slice: Slice<TemporaryAccommodationApplicationEntity>
    var offendersCrn = setOf<String>()

    try {
      while (hasNext) {
        log.info("Getting page $page for max page size $pageSize")
        slice = applicationRepository.findAllTemporaryAccommodationApplicationsAndNameNull(TemporaryAccommodationApplicationEntity::class.java, PageRequest.of(0, pageSize))

        offendersCrn = slice.map { it.crn }.toSet()

        log.info("Updating offenders name with crn ${offendersCrn.joinToString { "," }}")

        val personInfos = splitAndRetrievePersonInfo(offendersCrn, "")

        slice.content.forEach {
          val personInfo = personInfos[it.crn] ?: PersonSummaryInfoResult.Unknown(it.crn)
          updateApplication(personInfo, it)
        }

        entityManager.clear()
        hasNext = slice.hasNext()
        page += 1
      }
    } catch (exception: Exception) {
      log.error("Unable to update offenders name with crn ${offendersCrn.joinToString { "," }}", exception)
    }
  }

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
  private fun updateApplication(personInfo: PersonSummaryInfoResult, it: TemporaryAccommodationApplicationEntity) {
    try {
      val offenderName = when (personInfo) {
        is PersonSummaryInfoResult.Success.Full -> "${personInfo.summary.name.forename} ${personInfo.summary.name.surname}"
        is PersonSummaryInfoResult.NotFound, is PersonSummaryInfoResult.Unknown -> throw NotFoundProblem(
          personInfo.crn,
          "Offender",
        )
        is PersonSummaryInfoResult.Success.Restricted -> throw ForbiddenProblem()
      }

      it.name = offenderName
      applicationRepository.save(it)
    } catch (exception: Exception) {
      log.error("Unable to update offender name with crn ${it.crn} for the application ${it.id}", exception)
    }
  }

  private fun splitAndRetrievePersonInfo(crns: Set<String>, deliusUsername: String): Map<String, PersonSummaryInfoResult> {
    val crnMap = ListUtils.partition(crns.toList(), pageSize)
      .stream().map { crns ->
        offenderService.getOffenderSummariesByCrns(crns.toSet(), deliusUsername, ignoreLao = true).associateBy { it.crn }
      }.collect(Collectors.toList())

    return crnMap.flatMap { it.toList() }.toMap()
  }
}

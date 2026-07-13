package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.http.HttpEntity
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import java.util.UUID

@Component
class Cas1BackfillApplicationDraftDocumentJob(
  private val repository: Cas1BackfillApplicationDraftDocumentJobRepository,
  private val transactionTemplate: TransactionTemplate,
  @Value($$"${services.cas1-ui.base-url}") private val cas1UiBaseUrl: String,
  private val applicationsTransformer: ApplicationsTransformer,
  private val offenderDetailService: OffenderDetailService,
) : MigrationJob() {
  override val shouldRunInTransaction = false
  private val restTemplate = RestTemplate()

  private val log = LoggerFactory.getLogger(this::class.java)

  @SuppressWarnings("MagicNumber")
  override fun process(pageSize: Int) {
    var hasNext = true
    var runningCount = 0
    while (hasNext) {
      if (runningCount % 100 == 0) {
        log.info("Have processed $runningCount applications")
      }

      val applications = repository.applicationsWithNoDocument(PageRequest.of(0, pageSize))
      val personInfoResults = offenderDetailService.getPersonInfoResults(
        applications.map { it.crn }.toSet(),
        LaoStrategy.NeverRestricted,
      )

      applications.forEach { application ->
        val personInfoResult = personInfoResults.first { it.crn == application.crn }

        transactionTemplate.executeWithoutResult {
          processApplication(
            application,
            personInfoResult,
          )
        }
      }

      runningCount += applications.size

      hasNext = applications.hasNext()
    }

    log.info("Have backfilled a total $runningCount applications")
  }

  @SuppressWarnings("TooGenericExceptionCaught", "TooGenericExceptionThrown")
  private fun processApplication(
    application: ApprovedPremisesApplicationEntity,
    personInfoResult: PersonInfoResult,
  ) {
    val applicationId = application.id

    if (application.submittedAt != null) {
      error("Application $applicationId has no document but has been submitted")
    }

    val cas1Application = applicationsTransformer.transformJpaToCas1Application(
      application,
      personInfoResult,
    )

    try {
      val result = restTemplate.postForEntity<String>("$cas1UiBaseUrl/render-application", HttpEntity(cas1Application))
      repository.updateDocument(applicationId, result.body!!)
    } catch (e: Exception) {
      throw Exception("Error getting document for $applicationId", e)
    }
  }
}

@Repository
interface Cas1BackfillApplicationDraftDocumentJobRepository : JpaRepository<ApprovedPremisesApplicationEntity, UUID> {

  @Query(
    value = """
      FROM ApprovedPremisesApplicationEntity a
      WHERE a.document IS NULL AND a.data IS NOT NULL AND a.submittedAt IS NULL
      ORDER BY a.createdAt ASC""",
  )
  fun applicationsWithNoDocument(pageable: Pageable): Slice<ApprovedPremisesApplicationEntity>

  @Modifying
  @Query(
    value = """
        UPDATE applications
        SET document = CAST(:document AS json)
        WHERE id = :applicationId
    """,
    nativeQuery = true,
  )
  fun updateDocument(applicationId: UUID, document: String)
}

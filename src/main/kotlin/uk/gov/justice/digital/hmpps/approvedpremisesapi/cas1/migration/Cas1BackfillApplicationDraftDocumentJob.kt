package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpEntity
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import java.util.UUID

@Component
class Cas1BackfillApplicationDraftDocumentJob(
  private val repository: Cas1BackfillApplicationDraftDocumentJobRepository,
  private val transactionTemplate: TransactionTemplate,
  @Value($$"${services.cas1-ui.base-url}") private val cas1UiBaseUrl: String,
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

      val applicationIds = repository.applicationIdsWithNoDocument(PageRequest.of(0, 50))

      applicationIds.forEach { applicationId ->
        transactionTemplate.executeWithoutResult {
          processApplication(applicationId)
        }
      }

      runningCount += applicationIds.size

      hasNext = applicationIds.hasNext()
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught", "TooGenericExceptionThrown")
  private fun processApplication(applicationId: UUID) {
    val application = repository.findByIdOrNull(applicationId)!!

    if (application.submittedAt != null) {
      error("Application $applicationId has no document but has been submitted")
    }

    val data = application.data

    if (data != null) {
      try {
        val result = restTemplate.postForEntity<String>("$cas1UiBaseUrl/render-application", HttpEntity(data))
        repository.updateDocument(applicationId, result.body!!)
      } catch (e: Exception) {
        throw Exception("Error getting document for $applicationId", e)
      }
    }
  }
}

@Repository
interface Cas1BackfillApplicationDraftDocumentJobRepository : JpaRepository<ApprovedPremisesApplicationEntity, UUID> {

  @Query(
    value = """
      SELECT a.id 
      FROM applications a inner join approved_premises_applications apa on a.id = apa.id 
      WHERE a.document IS NULL AND a.data IS NOT NULL AND a.submitted_at IS NULL""",
    nativeQuery = true,
  )
  fun applicationIdsWithNoDocument(pageable: Pageable): Slice<UUID>

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

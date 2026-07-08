package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.migration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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

  override fun process(pageSize: Int) {
    val applicationIds = repository.applicationIdsWithNoDocument()

    log.info("There are ${applicationIds.size} applications with data but no document")

    applicationIds.forEach { applicationId ->
      transactionTemplate.executeWithoutResult {
        processApplication(applicationId)
      }
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun processApplication(applicationId: UUID) {
    val application = repository.findByIdOrNull(applicationId)!!

    if (application.submittedAt != null) {
      log.error("Application $applicationId has no document but has been submitted")
      return
    }

    val data = application.data

    if (data != null) {
      try {
        val result = restTemplate.postForEntity<String>("$cas1UiBaseUrl/backfill/application/$applicationId", HttpEntity(data))
        repository.updateDocument(applicationId, result.body!!)
      } catch (e: Exception) {
        log.error("Error getting document for $applicationId", e)
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
  fun applicationIdsWithNoDocument(): List<UUID>

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

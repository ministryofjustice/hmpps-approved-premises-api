package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.cas1UiMockPostForBackfillApplicationDocument
import java.time.OffsetDateTime

class Cas1BackfillApplicationDraftDocumentJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun `backfill applications ignoring null data`() {
    val applicationNoDataNoDocument = givenACas1Application(
      data = null,
      document = null,
    )
    val applicationHasDataNoDocument = givenACas1Application(
      data = """{ "name" : "populated data" }""",
      document = null,
    )
    val applicationHasDocument = givenACas1Application(
      data = """{ "name" : "populated data" }""",
      document = """{ "name": "already populated document" }""",
    )
    val applicationSubmitted = givenACas1Application(
      submittedAt = OffsetDateTime.now(),
      data = """{ "name" : "populated data" }""",
      document = null,
    )

    cas1UiMockPostForBackfillApplicationDocument(
      applicationId = applicationHasDataNoDocument.id,
      request = """{ "name" : "populated data" }""",
      response = """{ "name": "backfilled document" }""",
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillAppDraftDoc)

    assertThat(approvedPremisesApplicationRepository.findByIdOrNull(applicationNoDataNoDocument.id)!!.document).isNull()

    assertThat(approvedPremisesApplicationRepository.findByIdOrNull(applicationHasDataNoDocument.id)!!.document)
      .isEqualTo("""{ "name": "backfilled document" }""")

    assertThat(approvedPremisesApplicationRepository.findByIdOrNull(applicationHasDocument.id)!!.document)
      .isEqualTo("""{ "name": "already populated document" }""")

    assertThat(approvedPremisesApplicationRepository.findByIdOrNull(applicationSubmitted.id)!!.document)
      .isNull()
  }
}

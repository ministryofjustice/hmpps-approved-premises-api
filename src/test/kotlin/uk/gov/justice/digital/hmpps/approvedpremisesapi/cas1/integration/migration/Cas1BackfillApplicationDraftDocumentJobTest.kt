package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.cas1UiMockRenderApplicationPost
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import java.time.OffsetDateTime

class Cas1BackfillApplicationDraftDocumentJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Autowired
  lateinit var applicationsTransformer: ApplicationsTransformer

  @Test
  fun `backfill applications ignoring null data`() {
    // ignored
    val applicationNoDataNoDocument = givenACas1Application(
      submittedAt = null,
      data = null,
      document = null,
    )
    val applicationHasDocument = givenACas1Application(
      submittedAt = null,
      data = """{ "name" : "populated data" }""",
      document = """{ "name": "already populated document" }""",
    )
    val applicationSubmitted = givenACas1Application(
      submittedAt = OffsetDateTime.now(),
      data = """{ "name" : "populated data" }""",
      document = null,
    )

    val applicationsHasDataNoDocument = (0..12).map {
      val (offender, inmateDetails) = givenAnOffender()
      TestApplication(
        application = givenACas1Application(
          submittedAt = null,
          data = """{ "name" : "populated data" }""",
          document = null,
          crn = offender.otherIds.crn,
        ),
        offenderDetailSummary = offender,
        inmateDetails = inmateDetails,
      )
    }

    applicationsHasDataNoDocument.forEach {
      cas1UiMockRenderApplicationPost(
        request = it.toCas1ApplicationJson(),
        response = """{ "crn": "${it.application.crn}", "name": "backfilled document" }""",
      )
    }

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillAppDraftDoc)

    assertThat(approvedPremisesApplicationRepository.findByIdOrNull(applicationNoDataNoDocument.id)!!.document).isNull()

    applicationsHasDataNoDocument.forEach {
      assertThat(approvedPremisesApplicationRepository.findByIdOrNull(it.application.id)!!.document)
        .isEqualTo("""{ "crn": "${it.application.crn}", "name": "backfilled document" }""")
    }

    assertThat(approvedPremisesApplicationRepository.findByIdOrNull(applicationHasDocument.id)!!.document)
      .isEqualTo("""{ "name": "already populated document" }""")

    assertThat(approvedPremisesApplicationRepository.findByIdOrNull(applicationSubmitted.id)!!.document)
      .isNull()
  }

  private fun TestApplication.toCas1ApplicationJson() = jackson3JsonMapper.writeValueAsString(
    applicationsTransformer.transformJpaToCas1Application(
      applicationEntity = application,
      personInfo = PersonInfoResult.Success.Full(
        crn = offenderDetailSummary.otherIds.crn,
        offenderDetailSummary = offenderDetailSummary,
        inmateDetail = inmateDetails,
        tier = null,
      ),
    ),
  )

  private data class TestApplication(
    val application: ApprovedPremisesApplicationEntity,
    val offenderDetailSummary: OffenderDetailSummary,
    val inmateDetails: InmateDetail,
  )
}

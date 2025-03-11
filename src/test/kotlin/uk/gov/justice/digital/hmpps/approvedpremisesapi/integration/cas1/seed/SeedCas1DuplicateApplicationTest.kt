package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1DuplicateApplicationSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1DuplicateApplicationTest : SeedTestBase() {

  @Autowired
  lateinit var jsonSchemaService: JsonSchemaService

  @Test
  fun `Duplicate application and adds note`() {
    givenAUser { applicant, _ ->
      givenAnOffender { offenderDetails, _ ->

        val sourceApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(applicant)
          withApplicationSchema(jsonSchemaService.getNewestSchema(ApprovedPremisesApplicationJsonSchemaEntity::class.java))
          withSubmittedAt(OffsetDateTime.now())
          withNomsNumber(offenderDetails.otherIds.nomsNumber)
          withApArea(givenAnApArea())
          withIsEmergencyApplication(true)
          withReleaseType("licence")
        }

        apDeliusContextMockSuccessfulTeamsManagingCaseCall(
          offenderDetails.otherIds.crn,
          ManagingTeamsResponse(
            teamCodes = listOf("TEAM1"),
          ),
        )

        apOASysContextMockSuccessfulNeedsDetailsCall(
          offenderDetails.otherIds.crn,
          NeedsDetailsFactory().produce(),
        )

        seed(
          SeedFileType.approvedPremisesDuplicateApplication,
          rowsToCsv(
            listOf(
              Cas1DuplicateApplicationSeedCsvRow(
                applicationId = sourceApplication.id,
              ),
            ),
          ),
        )

        val newApplication = approvedPremisesApplicationRepository.findAll()
          .filter { it.crn == sourceApplication.crn }
          .first { it.id != sourceApplication.id }

        assertThat(newApplication.crn).isEqualTo(sourceApplication.crn)
        assertThat(newApplication.createdByUser.id).isEqualTo(sourceApplication.createdByUser.id)
        assertThat(newApplication.data).isEqualTo(sourceApplication.data)
        assertThat(newApplication.document).isNull()
        assertThat(newApplication.schemaVersion.id).isEqualTo(sourceApplication.schemaVersion.id)
        assertThat(newApplication.createdAt).isWithinTheLastMinute()
        assertThat(newApplication.submittedAt).isNull()
        assertThat(newApplication.isWomensApplication).isEqualTo(sourceApplication.isWomensApplication)
        assertThat(newApplication.isEmergencyApplication).isEqualTo(sourceApplication.isEmergencyApplication)
        assertThat(newApplication.apType).isEqualTo(sourceApplication.apType)
        assertThat(newApplication.isInapplicable).isNull()
        assertThat(newApplication.isWithdrawn).isEqualTo(false)
        assertThat(newApplication.withdrawalReason).isNull()
        assertThat(newApplication.otherWithdrawalReason).isNull()
        assertThat(newApplication.convictionId).isEqualTo(sourceApplication.convictionId)
        assertThat(newApplication.eventNumber).isEqualTo(sourceApplication.eventNumber)
        assertThat(newApplication.offenceId).isEqualTo(sourceApplication.offenceId)
        assertThat(newApplication.nomsNumber).isEqualTo(sourceApplication.nomsNumber)
        assertThat(newApplication.teamCodes).hasSize(1)
        assertThat(newApplication.teamCodes.map { it.teamCode }).containsOnly("TEAM1")
        assertThat(newApplication.placementRequests).isEmpty()
        assertThat(newApplication.releaseType).isEqualTo(sourceApplication.releaseType)
        assertThat(newApplication.sentenceType).isEqualTo(sourceApplication.sentenceType)
        assertThat(newApplication.situation).isEqualTo(sourceApplication.situation)
        assertThat(newApplication.arrivalDate).isEqualTo(sourceApplication.arrivalDate)
        assertThat(newApplication.name).isEqualTo("${offenderDetails.firstName.uppercase()} ${offenderDetails.surname.uppercase()}")
        assertThat(newApplication.targetLocation).isEqualTo(sourceApplication.targetLocation)
        assertThat(newApplication.status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)
        assertThat(newApplication.inmateInOutStatusOnSubmission).isEqualTo(sourceApplication.inmateInOutStatusOnSubmission)
        assertThat(newApplication.apArea).isNull()
        assertThat(newApplication.applicantUserDetails).isEqualTo(sourceApplication.applicantUserDetails)
        assertThat(newApplication.caseManagerIsNotApplicant).isEqualTo(sourceApplication.caseManagerIsNotApplicant)
        assertThat(newApplication.caseManagerUserDetails).isEqualTo(sourceApplication.caseManagerUserDetails)
        assertThat(newApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.emergency)

        assertThat(newApplication.isEsapApplication).isEqualTo(sourceApplication.isEsapApplication)
        assertThat(newApplication.isPipeApplication).isEqualTo(sourceApplication.isPipeApplication)

        val notes = applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationIdAndDeletedAtIsNull(newApplication.id)
        assertThat(notes).hasSize(1)
        assertThat(notes)
          .extracting("body")
          .contains(
            "Application automatically created by Application Support by duplicating existing application ${sourceApplication.id}",
          )
      }
    }
  }

  private fun rowsToCsv(rows: List<Cas1DuplicateApplicationSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "application_id",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.applicationId.toString())
        .newRow()
    }

    return builder.build()
  }
}

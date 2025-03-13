package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.UpdateAssessmentReportPropertiesRepository
import java.time.OffsetDateTime

class Cas1UpdateAssessmentReportPropertiesJobTest : IntegrationTestBase() {

  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Autowired
  lateinit var updateAssessmentReportPropertiesRepository: UpdateAssessmentReportPropertiesRepository

  @Test
  fun shouldUpdateAssessmentReportPropertiesCorrectlyWhenAvailable() {
    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(givenAProbationRegion())
    }

    val (offenderDetails) = givenAnOffender()

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withApplicationSchema(applicationSchema)
    }
    val assessment1 = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withAgreeWithShortNoticeReason(null)
      withAgreeWithShortNoticeReasonComments(null)
      withReasonForLateApplication(null)
      withSubmittedAt(OffsetDateTime.now())
      withData(
        objectMapper.writeValueAsString(
          mapOf(
            "suitability-assessment" to mapOf(
              "application-timeliness" to mapOf(
                "agreeWithShortNoticeReason" to "yes",
                "agreeWithShortNoticeReasonComments" to "Applicant agrees due to unforeseen circumstances.",
                "reasonForLateApplication" to "Family emergency delayed the submission of the application.",
              ),
            ),
          ),
        ),
      )
    }

    val assessment2 = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withAgreeWithShortNoticeReason(null)
      withAgreeWithShortNoticeReasonComments(null)
      withReasonForLateApplication(null)
      withSubmittedAt(OffsetDateTime.now())
      withData(
        objectMapper.writeValueAsString(
          mapOf(
            "suitability-assessment" to mapOf(
              "application-timeliness" to mapOf(
                "agreeWithShortNoticeReason" to "no",
                "agreeWithShortNoticeReasonComments" to null,
                "reasonForLateApplication" to null,
              ),
            ),
          ),
        ),
      )
    }

    val assessment3 = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withAgreeWithShortNoticeReason(null)
      withAgreeWithShortNoticeReasonComments("")
      withReasonForLateApplication("")
      withSubmittedAt(OffsetDateTime.now())
      withData(
        objectMapper.writeValueAsString(
          mapOf(
            "suitability-assessment" to emptyMap<String, Any>(),
          ),
        ),
      )
    }

    migrationJobService.runMigrationJob(MigrationJobType.cas1ApprovedPremisesAssessmentReportProperties, 1)

    val assessment1AfterUpdate = updateAssessmentReportPropertiesRepository.findByIdOrNull(assessment1.id)!!
    assertThat(assessment1AfterUpdate.agreeWithShortNoticeReason).isTrue
    assertThat(assessment1AfterUpdate.agreeWithShortNoticeReasonComments).isEqualTo("Applicant agrees due to unforeseen circumstances.")
    assertThat(assessment1AfterUpdate.reasonForLateApplication).isEqualTo("Family emergency delayed the submission of the application.")

    val assessment2AfterUpdate = updateAssessmentReportPropertiesRepository.findByIdOrNull(assessment2.id)!!
    assertThat(assessment2AfterUpdate.agreeWithShortNoticeReason).isFalse
    assertThat(assessment2AfterUpdate.agreeWithShortNoticeReasonComments).isNull()
    assertThat(assessment2AfterUpdate.reasonForLateApplication).isNull()

    val assessment3AfterUpdate = updateAssessmentReportPropertiesRepository.findByIdOrNull(assessment3.id)!!
    assertThat(assessment3AfterUpdate.agreeWithShortNoticeReason).isNull()
    assertThat(assessment3AfterUpdate.agreeWithShortNoticeReasonComments).isNull()
    assertThat(assessment3AfterUpdate.reasonForLateApplication).isNull()
  }
}

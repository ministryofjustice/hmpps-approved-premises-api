package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1RemoveAssessmentDetailsSeedCsvRow
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class Cas1RedactAssessmentDetailsTest : SeedTestBase() {

  @Test
  fun `Updates required assessments leaving others unmodified`() {
    val sampleJson = """{
      "review-application":{"review":{"reviewed":"yes"}},
      "sufficient-information":{"sufficient-information":{"sufficientInformation":"yes","query":""}},
      "suitability-assessment":{"suitability-assessment":{"riskFactors":"yes","riskFactorsComments":"asd","riskManagement":"yes","riskManagementComments":"","locationOfPlacement":"yes","locationOfPlacementComments":"","moveOnPlan":"yes","moveOnPlanComments":""}},
      "required-actions":{"required-actions":{"additionalActions":"no","additionalActionsComments":"","curfewsOrSignIns":"no","curfewsOrSignInsComments":"","concernsOfUnmanagableRisk":"no","concernsOfUnmanagableRiskComments":"","additionalRecommendations":"no","additionalRecommendationsComments":"","nameOfAreaManager":"","dateOfDiscussion-year":"","dateOfDiscussion-month":"","dateOfDiscussion-day":"","outlineOfDiscussion":""}},
      "make-a-decision":{"make-a-decision":{"decision":"accept","decisionRationale":""}},"matching-information":{"matching-information":{"apType":"normal","lengthOfStayAgreed":"yes","lengthOfStayWeeks":"","lengthOfStayDays":"","cruInformation":"asd","isWheelchairDesignated":"notRelevant","isArsonDesignated":"notRelevant","isSingle":"notRelevant","isCatered":"essential","isSuitedForSexOffenders":"notRelevant","isStepFreeDesignated":"notRelevant","hasEnSuite":"notRelevant","isSuitableForVulnerable":"notRelevant","acceptsSexOffenders":"notRelevant","acceptsChildSexOffenders":"notRelevant","acceptsNonSexualChildOffenders":"notRelevant","acceptsHateCrimeOffenders":"notRelevant","isArsonSuitable":"notRelevant"}},
      "check-your-answers":{"check-your-answers":{"reviewed":"1"}}
    }"""

    val assessment1NoJson = createAssessment(data = null)
    val assessment2HasJson = createAssessment(data = sampleJson)
    val assessment3Unmodified = createAssessment(data = sampleJson)

    withCsv(
      "valid-csv",
      rowsToCsv(
        listOf(
          Cas1RemoveAssessmentDetailsSeedCsvRow(assessment1NoJson.id.toString()),
          Cas1RemoveAssessmentDetailsSeedCsvRow(assessment2HasJson.id.toString()),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesRedactAssessmentDetails, "valid-csv")

    val expectedJson = """{"sufficient-information":{"sufficient-information":{"sufficientInformation":"yes","query":""}}}"""

    Assertions.assertThat(approvedPremisesAssessmentRepository.findByIdOrNull(assessment1NoJson.id)!!.data).isNull()
    Assertions.assertThat(approvedPremisesAssessmentRepository.findByIdOrNull(assessment1NoJson.id)!!.document).isNull()

    Assertions.assertThat(approvedPremisesAssessmentRepository.findByIdOrNull(assessment2HasJson.id)!!.data).isEqualTo(expectedJson)
    Assertions.assertThat(approvedPremisesAssessmentRepository.findByIdOrNull(assessment2HasJson.id)!!.document).isEqualTo(expectedJson)

    Assertions.assertThat(approvedPremisesAssessmentRepository.findByIdOrNull(assessment3Unmodified.id)!!.data).isEqualTo(sampleJson)
    Assertions.assertThat(approvedPremisesAssessmentRepository.findByIdOrNull(assessment3Unmodified.id)!!.document).isEqualTo(sampleJson)
  }

  fun removeLineBreaks(input: String) = input.replace(Regex("""(\r\n)|\n"""), "")

  private fun createAssessment(data: String?): AssessmentEntity {
    val (user) = `Given a User`()
    val (offenderDetails) = `Given an Offender`()

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

    return approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(user)
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
      withData(data)
      withDocument(data)
    }
  }

  private fun rowsToCsv(rows: List<Cas1RemoveAssessmentDetailsSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "assessment_id",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.assessmentId)
        .newRow()
    }

    return builder.build()
  }
}

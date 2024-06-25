package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1FurtherInfoBugFixSeedCsvRow
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedAssessmentMoreInfoBugFixTest : SeedTestBase() {

  @Test
  fun `Updates required assessments leaving others unmodified`() {
    val sampleJson = """{
      "sufficient-information-confirm": {
        "confirm": "no"
      },
      "information-received": {
        "informationReceived":"no"
      },
      "something": "else"
      }"""

    val assessment1NoJson = createAssessment(data = null)
    val assessment2HasFormattedJson = createAssessment(data = sampleJson)
    val assessment3Unmodified = createAssessment(data = sampleJson)
    val assessment4FlatJson = createAssessment(data = removeLineBreaks(sampleJson))

    withCsv(
      "valid-csv",
      rowsToCsv(
        listOf(
          Cas1FurtherInfoBugFixSeedCsvRow(assessment1NoJson.id.toString()),
          Cas1FurtherInfoBugFixSeedCsvRow(assessment2HasFormattedJson.id.toString()),
          Cas1FurtherInfoBugFixSeedCsvRow(assessment4FlatJson.id.toString()),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesAssessmentMoreInfoBugFix, "valid-csv")

    val expectedJson = """{
      "sufficient-information-confirm":{"confirm":"yes"},
      "information-received": {
        "informationReceived":"no"
      },
      "something": "else"
      }"""

    assertThat(approvedPremisesAssessmentRepository.findByIdOrNull(assessment1NoJson.id)!!.data).isNull()
    assertThat(approvedPremisesAssessmentRepository.findByIdOrNull(assessment2HasFormattedJson.id)!!.data).isEqualTo(expectedJson)
    assertThat(approvedPremisesAssessmentRepository.findByIdOrNull(assessment3Unmodified.id)!!.data).isEqualTo(sampleJson)
    assertThat(approvedPremisesAssessmentRepository.findByIdOrNull(assessment4FlatJson.id)!!.data).isEqualTo(removeLineBreaks(expectedJson))
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
    }
  }

  private fun rowsToCsv(rows: List<Cas1FurtherInfoBugFixSeedCsvRow>): String {
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

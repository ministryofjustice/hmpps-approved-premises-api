package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.seed

import com.fasterxml.jackson.databind.node.ArrayNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.seed.Cas2BackfillManualRoshAnswersSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas2BackfillManualRoshAnswersSeedJobTest : SeedTestBase() {

  @Test
  fun `backfills manual RoSH answers for eligible applications created since the supplied date`() {
    val user = cas2UserEntityFactory.produceAndPersist()

    val applicationToUpdate = givenApplication(
      user = user,
      createdAt = OffsetDateTime.parse("2026-10-01T12:34:56.789Z"),
      data = dataWithManualRosh,
      document = documentWithRoshTask(
        """[{"question": "Who is at risk?", "answer": "Person A"}]""",
      ),
    )

    val applicationCreatedBeforeCutoff = givenApplication(
      user = user,
      createdAt = OffsetDateTime.parse("2026-08-01T12:34:56.789Z"),
      data = dataWithManualRosh,
      document = documentWithRoshTask("[]"),
    )

    val applicationWithDifferentOrigin = cas2ApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withApplicationOrigin(ApplicationOrigin.prisonBail)
      withCreatedAt(OffsetDateTime.parse("2026-09-01T00:00:00+01:00"))
      withData(dataWithManualRosh)
      withDocument(documentWithRoshTask("[]"))
    }

    val applicationWithoutManualRosh = givenApplication(
      user = user,
      createdAt = OffsetDateTime.parse("2026-09-01T00:00:00+01:00"),
      data = """{"risks-of-serious-harm-to-others":{}}""",
      document = documentWithRoshTask("[]"),
    )

    seed(
      SeedFileType.cas2BackfillManualRoshAnswers,
      rowsToCsv(
        listOf(
          Cas2BackfillManualRoshAnswersSeedCsvRow(
            createdAfter = LocalDate.of(2026, 8, 24),
          ),
        ),
      ),
    )

    assertThat(questionsAndAnswers(applicationToUpdate))
      .containsExactly(
        "Created by prison offender manager" to "1 October 2026",
        "Overall risk rating" to "High",
        "Risk to children" to "Low",
        "Risk to known adult" to "High",
        "Risk to public" to "Medium",
        "Risk to staff" to "Very high",
        "Who is at risk?" to "Person A",
      )

    assertThat(questionsAndAnswers(applicationCreatedBeforeCutoff)).isEmpty()
    assertThat(questionsAndAnswers(applicationWithDifferentOrigin)).isEmpty()
    assertThat(questionsAndAnswers(applicationWithoutManualRosh)).isEmpty()
  }

  private fun givenApplication(
    user: Cas2UserEntity,
    createdAt: OffsetDateTime,
    data: String,
    document: String,
  ): Cas2ApplicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
    withCreatedByUser(user)
    withApplicationOrigin(ApplicationOrigin.other)
    withCreatedAt(createdAt)
    withData(data)
    withDocument(document)
  }

  private fun questionsAndAnswers(application: Cas2ApplicationEntity): List<Pair<String?, String?>> {
    val persistedApplication = cas2ApplicationRepository.findById(application.id).get()
    val roshTask = jsonMapper.readTree(persistedApplication.document)
      .path("sections")
      .first { it.path("title").textValue() == "Risk and health needs" }
      .path("tasks")
      .first { it.path("title").textValue() == "Add risk of serious harm to others" }

    return (roshTask.path("questionsAndAnswers") as ArrayNode)
      .map { it.path("question").textValue() to it.path("answer").textValue() }
  }

  private fun rowsToCsv(rows: List<Cas2BackfillManualRoshAnswersSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields("created_after")
      .newRow()

    rows.forEach { row ->
      builder
        .withQuotedField(row.createdAfter.toString())
        .newRow()
    }

    return builder.build()
  }

  private fun documentWithRoshTask(questionsAndAnswers: String) = """
    {
      "sections": [{
        "title": "Risk and health needs",
        "tasks": [{
          "title": "Add risk of serious harm to others",
          "questionsAndAnswers": $questionsAndAnswers
        }]
      }]
    }
  """.trimIndent()

  companion object {
    private val dataWithManualRosh = """
      {
        "risks-of-serious-harm-to-others": {
          "manual-rosh-information": {
            "createdAt": "2026-10-01T12:34:56.789Z",
            "overallRisk": "High",
            "riskToChildren": "Low",
            "riskToPublic": "Medium",
            "riskToKnownAdult": "High",
            "riskToStaff": "Very high"
          }
        }
      }
    """.trimIndent()
  }
}

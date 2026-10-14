package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.seed

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.ArrayNode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.seed.Cas2BackfillManualRoshAnswersSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.seed.Cas2BackfillManualRoshAnswersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.JsonMapperFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2BackfillManualRoshAnswersSeedJobTest {
  private val repository = mockk<Cas2ApplicationRepository>()
  private val jsonMapper = JsonMapperFactory.createJackson3JsonMapper()
  private val job = Cas2BackfillManualRoshAnswersSeedJob(repository, jsonMapper)

  private val dataWithManualRosh = """
    {
      "risks-of-serious-harm-to-others": {
        "manual-rosh-information": {
          "riskToChildren": "Low",
          "riskToPublic": "Medium",
          "riskToKnownAdult": "High",
          "riskToStaff": "Very high",
          "overallRisk": "High",
          "createdAt": "2026-10-08T12:20:56.523Z"
        }
      }
    }
  """.trimIndent()

  private val expectedAnswers = listOf(
    "Created by prison offender manager" to "8 October 2026",
    "Overall risk rating" to "High",
    "Risk to children" to "Low",
    "Risk to known adult" to "High",
    "Risk to public" to "Medium",
    "Risk to staff" to "Very high",
  )

  private val expectedAnswersJson = expectedAnswers.joinToString(",", "[", "]") { (q, a) ->
    """{"question":"$q","answer":"$a"}"""
  }

  private fun document(roshResponses: String) = """
    {
      "sections": [{
        "title": "Risk and health needs",
        "tasks": [
          {
            "title": "Add risk of serious harm to others",
            "questionsAndAnswers": $roshResponses
          }
        ]
      }]
    }
  """.trimIndent()

  private fun roshQuestionsAndAnswers(document: String) = (
    jsonMapper.readTree(document)
      .path("sections").first { it.path("title").stringValue() == "Risk and health needs" }
      .path("tasks").first { it.path("title").stringValue() == "Add risk of serious harm to others" }
      .path("questionsAndAnswers") as ArrayNode
    )
    .elements()
    .map { it.path("question").stringValue() to it.path("answer").stringValue() }

  @Nested
  inner class AddManualRoshAnswers {

    @Test
    fun `adds all manual RoSH answers when they are missing, preserving existing answers`() {
      val result = job.addManualRoshAnswers(
        UUID.randomUUID(),
        dataWithManualRosh,
        document("""[{"question": "Who is at risk?", "answer": "Person A"}]"""),
      )

      assertThat(roshQuestionsAndAnswers(result!!))
        .isEqualTo(expectedAnswers + listOf("Who is at risk?" to "Person A"))
    }

    @Test
    fun `returns null when manual RoSH answers are already present in the document`() {
      val roshDocument = """
        [
          {"question": "Created by prison offender manager", "answer": "8 October 2026"},
          {"question": "Overall risk rating", "answer": "High"},
          {"question": "Risk to children", "answer": "Low"},
          {"question": "Risk to known adult", "answer": "High"},
          {"question": "Risk to public", "answer": "Medium"},
          {"question": "Risk to staff", "answer": "High"},
          {"question": "Who is at risk?", "answer": "Person B"}
        ]
      """.trimIndent()

      val result = job.addManualRoshAnswers(
        UUID.randomUUID(),
        dataWithManualRosh,
        document(roshDocument),
      )

      assertThat(result).isNull()
    }

    @Test
    fun `adds only missing answers when some are already present`() {
      val roshDocument = """
        [
          {"question": "Created by prison offender manager", "answer": "8 October 2026"},
          {"question": "Overall risk rating", "answer": "High"},
          {"question": "Risk to children", "answer": "Low"},
          {"question": "Risk to known adult", "answer": "High"},
          {"question": "Who is at risk?", "answer": "Person C"}
        ]
      """.trimIndent()

      val result = job.addManualRoshAnswers(
        UUID.randomUUID(),
        dataWithManualRosh,
        document(roshDocument),
      )

      assertThat(roshQuestionsAndAnswers(result!!))
        .isEqualTo(expectedAnswers + listOf("Who is at risk?" to "Person C"))
    }

    @Test
    fun `returns null when there is no manual RoSH information`() {
      val data = """{"risks-of-serious-harm-to-others":{"summary":{"oasysImportedDate":"2026-10-01"}}}"""

      assertThat(job.addManualRoshAnswers(UUID.randomUUID(), data, document("[]"))).isNull()
    }

    @Test
    fun `returns null when the document has no RoSH task`() {
      val documentWithoutTask = """{"sections":[{"title":"Risk and health needs","tasks":[]}]}"""

      assertThat(job.addManualRoshAnswers(UUID.randomUUID(), dataWithManualRosh, documentWithoutTask)).isNull()
    }

    @Test
    fun `returns null when data or document is null`() {
      assertThat(job.addManualRoshAnswers(UUID.randomUUID(), null, document("[]"))).isNull()
      assertThat(job.addManualRoshAnswers(UUID.randomUUID(), dataWithManualRosh, null)).isNull()
    }
  }

  @Nested
  inner class ProcessRow {

    @Test
    fun `queries from the start of the given UK day and only saves applications that changed`() {
      val user = Cas2UserEntityFactory().produce()
      val needsUpdate = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withData(dataWithManualRosh)
        .withDocument(document("""[{"question": "Who is at risk?", "answer": "Person D"}]"""))
        .produce()
      val alreadyCorrect = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withData(dataWithManualRosh)
        .withDocument(document(expectedAnswersJson))
        .produce()

      every { repository.findAllSubmittedWithManualRoshInformation(any()) } returns listOf(needsUpdate, alreadyCorrect)
      every { repository.save(any()) } answers { firstArg() }

      job.processRow(Cas2BackfillManualRoshAnswersSeedCsvRow(createdAfter = LocalDate.of(2026, 9, 1)))

      verify { repository.findAllSubmittedWithManualRoshInformation(OffsetDateTime.parse("2026-09-01T00:00+01:00")) }
      verify(exactly = 1) { repository.save(needsUpdate) }
      verify(exactly = 0) { repository.save(alreadyCorrect) }
      assertThat(roshQuestionsAndAnswers(needsUpdate.document!!)).isEqualTo(expectedAnswers + listOf("Who is at risk?" to "Person D"))
    }
  }
}

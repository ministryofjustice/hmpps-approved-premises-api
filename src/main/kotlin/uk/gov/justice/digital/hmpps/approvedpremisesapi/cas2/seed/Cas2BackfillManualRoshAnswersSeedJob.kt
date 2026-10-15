package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.seed

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jobs.seed.SeedJob
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Component
class Cas2BackfillManualRoshAnswersSeedJob(
  private val cas2ApplicationRepository: Cas2ApplicationRepository,
  private val jsonMapper: JsonMapper,
) : SeedJob<Cas2BackfillManualRoshAnswersSeedCsvRow>(
  requiredHeaders = setOf("created_after"),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = Cas2BackfillManualRoshAnswersSeedCsvRow(
    createdAfter = LocalDate.parse(columns["created_after"]!!.trim()),
  )

  override fun processRow(row: Cas2BackfillManualRoshAnswersSeedCsvRow) {
    val createdAfter = row.createdAfter.atStartOfDay(UK_TIMEZOME).toOffsetDateTime()
    val applications = cas2ApplicationRepository.findAllSubmittedWithManualRoshInformation(createdAfter)
    log.info("Found ${applications.size} submitted applications with manual RoSH information created since $createdAfter")

    var updatedCount = 0
    applications.forEach { application ->
      val updatedDocument = addManualRoshAnswers(application.id, application.data, application.document)
      if (updatedDocument != null) {
        application.document = updatedDocument
        cas2ApplicationRepository.save(application)
        updatedCount++
        log.info("Updated manual RoSH answers in document for application ${application.id}")
      }
    }

    log.info("Updated $updatedCount of ${applications.size} applications")
  }

  fun addManualRoshAnswers(applicationId: UUID, data: String?, document: String?): String? {
    if (data == null || document == null) error("Expected application $applicationId to have data and document")

    val manualRosh = jsonMapper.readTree(data)
      .path("risks-of-serious-harm-to-others")
      .path("manual-rosh-information")
    if (!manualRosh.isObject) return null

    val documentNode = jsonMapper.readTree(document)
    val roshTask = documentNode.path("sections")
      .firstOrNull { it.path("title").stringValue() == RISK_SECTION_TITLE }
      ?.path("tasks")
      ?.firstOrNull { it.path("title").stringValue() == ROSH_TASK_TITLE } as? ObjectNode

    if (roshTask == null) {
      error("Application $applicationId has manual RoSH information but no '$ROSH_TASK_TITLE' task in its document")
    }

    val questionsAndAnswers = roshTask.get("questionsAndAnswers") as? ArrayNode
      ?: roshTask.putArray("questionsAndAnswers")

    var insertionIndex = questionsAndAnswers.indexOfFirst { it.path("question").stringValue() == "Who is at risk?" }
    insertionIndex = maxOf(insertionIndex, 0)

    var changed = false
    expectedAnswers(manualRosh).forEach { (question, answer) ->
      val existing = questionsAndAnswers.firstOrNull { it.path("question").stringValue() == question } as? ObjectNode
      if (existing == null) {
        // insert before "Who is at risk" question
        questionsAndAnswers.insert(insertionIndex, jsonMapper.createObjectNode().put("question", question).put("answer", answer))
        changed = true
        insertionIndex += 1
      }
    }

    return if (changed) jsonMapper.writeValueAsString(documentNode) else null
  }

  private fun expectedAnswers(manualRosh: JsonNode): List<Pair<String, String>> {
    val createdAt = manualRosh.path("createdAt").stringValue()
      ?.let { Instant.parse(it).atZone(UK_TIMEZOME).toLocalDate().format(DATE_FORMAT) }

    return listOf(
      "Created by prison offender manager" to createdAt,
      "Overall risk rating" to manualRosh.path("overallRisk").stringValue(),
      "Risk to children" to manualRosh.path("riskToChildren").stringValue(),
      "Risk to known adult" to manualRosh.path("riskToKnownAdult").stringValue(),
      "Risk to public" to manualRosh.path("riskToPublic").stringValue(),
      "Risk to staff" to manualRosh.path("riskToStaff").stringValue(),
    ).mapNotNull { (question, answer) -> answer?.takeIf { it.isNotBlank() }?.let { question to it } }
  }

  companion object {
    private const val RISK_SECTION_TITLE = "Risk and health needs"
    private const val ROSH_TASK_TITLE = "Add risk of serious harm to others"
    private val UK_TIMEZOME = ZoneId.of("Europe/London")
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
  }
}

data class Cas2BackfillManualRoshAnswersSeedCsvRow(
  val createdAfter: LocalDate,
)

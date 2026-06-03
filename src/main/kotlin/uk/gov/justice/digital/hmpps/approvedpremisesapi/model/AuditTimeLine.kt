package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import java.time.Instant

data class AuditTimeLine(
  val revisionNumber: Int,
  val occurredAt: Instant,
  val userName: String,
  val title: String,
  val sections: MutableList<AuditTimeLineSection> = mutableListOf(),
)

data class AuditTimeLineSection(
  val title: String,
  val content: MutableList<String> = mutableListOf(),
)

fun AuditTimeLine.render(): String = buildString {
  appendLine("$title By $userName")
  appendLine("on $occurredAt")

  sections.forEach {
    appendLine()
    appendLine(it.title)

    it.content.forEach { line ->
      appendLine("- $line")
    }
  }
}

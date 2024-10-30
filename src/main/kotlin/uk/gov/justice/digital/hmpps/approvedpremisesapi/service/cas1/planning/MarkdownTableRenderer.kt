package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

object MarkdownTableRenderer {

  fun render(
    headers: List<String>,
    body: List<List<String>>,
    colWidths: List<Int>,
  ): String {
    check(headers.size == colWidths.size) { "Col widths should have ${headers.size} entries" }
    body.forEachIndexed { i, row ->
      check(headers.size == row.size) { "Row $i should have ${headers.size} entries" }
    }

    val output = StringBuilder()

    output.append("|")
    headers.forEachIndexed { i, header ->
      output.append(" ")
      output.append(header.padEnd(colWidths[i], ' '))
      output.append(" |")
    }
    output.appendLine()

    output.append("|")
    colWidths.forEach { width ->
      output.append(" ")
      output.append("".padEnd(width, '-'))
      output.append(" |")
    }
    output.appendLine()

    body.forEach { row ->
      output.append("|")
      row.forEachIndexed { i, col ->
        output.append(" ")
        output.append(col.padEnd(colWidths[i], ' '))
        output.append(" |")
      }
      output.appendLine()
    }

    return output.toString()
  }
}

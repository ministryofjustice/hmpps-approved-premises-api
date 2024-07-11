package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

class CsvBuilder {
  private var csv = StringBuilder()

  fun withUnquotedField(field: Any) = apply {
    csv.append("$field,")
  }

  fun withHeader(vararg fields: Any) = withUnquotedFields(*fields)
  fun withUnquotedFields(vararg fields: Any) = apply {
    fields.forEach {
      csv.append("$it,")
    }
  }

  fun withQuotedField(field: Any) = apply {
    csv.append("\"$field\",")
  }

  fun withQuotedFields(vararg fields: Any) = apply {
    fields.forEach {
      csv.append("\"$it\",")
    }
  }

  fun newRow() = apply {
    csv.deleteCharAt(csv.length - 1)
    csv.append("\n")
  }

  fun build() = csv.toString()
}

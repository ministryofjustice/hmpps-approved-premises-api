package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import java.util.Locale

fun String.kebabCaseToPascalCase(): String {
  val pattern = "-[a-z]".toRegex()
  return replace(pattern) { it.value.last().uppercase() }.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

fun String.javaConstantNameToSentence(): String = replace("_", " ")
  .lowercase()
  .replaceFirstChar { it.uppercase() }

package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CruService {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val cruMappings = mutableMapOf(
    "Midlands" to listOf("N31", "N53"),
    "North West" to listOf("N28", "N50", "MCG", "GMP", "N29", "N51", "N24", "N01", "N33", "N55"),
    "South East & Eastern incl. Women" to listOf("N35", "N57", "N34", "N56"),
    "London" to listOf("N07", "N21", "C17", "LDN"),
    "North East" to listOf("N32", "N54", "N02", "N23"),
    "Wales" to listOf("N03", "C10", "N27", "WPT"),
    "Midlands" to listOf("MLW", "N30", "N52"),
    "South West & South Central" to listOf("N36", "N58", "N26", "N37", "N59", "N05"),
  )

  fun cruNameFromProbationAreaCode(code: String): String {
    cruMappings.forEach {
      if (it.value.contains(code)) return it.key
    }

    log.warn("No CRU mapping for Probation Area code: $code")
    return "Unknown CRU"
  }
}

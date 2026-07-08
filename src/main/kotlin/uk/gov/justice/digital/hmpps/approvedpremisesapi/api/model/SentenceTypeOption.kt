package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

@Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
enum class SentenceTypeOption(@get:JsonValue val value: String) {

  standardDeterminate("standardDeterminate"),
  life("life"),
  ipp("ipp"),
  extendedDeterminate("extendedDeterminate"),
  communityOrder("communityOrder"),
  bailPlacement("bailPlacement"),
  nonStatutory("nonStatutory"),
  ;

  companion object {
    private val ENTRIES_BY_LOWERCASE_VALUE: Map<String, SentenceTypeOption> =
      entries.associateBy { it.value.lowercase() }

    @JvmStatic
    @JsonCreator
    fun forValue(value: String): SentenceTypeOption = values().first { it.value == value }

    fun fromValueOrNull(value: String?): SentenceTypeOption? = value?.let { ENTRIES_BY_LOWERCASE_VALUE[it.lowercase()] }
  }
}

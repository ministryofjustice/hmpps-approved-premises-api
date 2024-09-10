package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringListConverter : AttributeConverter<List<String?>?, String?> {
  companion object {
    private const val SPLIT_CHAR = ";"
  }

  override fun convertToDatabaseColumn(stringList: List<String?>?): String? {
    return stringList?.joinToString(separator = SPLIT_CHAR)
  }

  override fun convertToEntityAttribute(string: String?): List<String?>? {
    if (string == null) {
      return null
    }

    if (string.isBlank()) {
      return emptyList()
    }

    return string.split(SPLIT_CHAR)
  }
}

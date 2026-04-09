package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class JsonStringDbConverter : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(attribute: String?): String? {
        return attribute  // keep as-is
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return dbData
    }
}
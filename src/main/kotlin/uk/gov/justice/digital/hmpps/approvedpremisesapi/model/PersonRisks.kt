package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.time.LocalDate

data class RiskWithStatus<T>(val status: RiskStatus, val value: T? = null) {
  constructor(value: T?) : this(RiskStatus.Retrieved, value)
}

enum class RiskStatus {
  Retrieved,
  NotFound,
  Error,
}

data class PersonRisks(
  val roshRisks: RiskWithStatus<RoshRisks>,
  val mappa: RiskWithStatus<Mappa>,
  val tier: RiskWithStatus<RiskTier>,
  val flags: RiskWithStatus<List<String>>,
)

data class RoshRisks(
  val overallRisk: String,
  val riskToChildren: String,
  val riskToPublic: String,
  val riskToKnownAdult: String,
  val riskToStaff: String,
  val lastUpdated: LocalDate?,
)

data class Mappa(
  val level: String,
  val lastUpdated: LocalDate,
)

data class RiskTier(
  val level: String,
  val lastUpdated: LocalDate,
)

@Converter(autoApply = true)
class PersonRisksConverter : AttributeConverter<PersonRisks, String?> {
  private val objectMapper = jacksonObjectMapper()

  override fun convertToDatabaseColumn(domainForm: PersonRisks?): String? {
    return try {
      objectMapper.writeValueAsString(domainForm)
    } catch (exception: Exception) {
      throw RuntimeException("Unable to serialize PersonRisks to JSON string for database", exception)
    }
  }

  override fun convertToEntityAttribute(dbData: String?): PersonRisks? {
    return try {
      objectMapper.readValue(dbData, PersonRisks::class.java)
    } catch (exception: Exception) {
      throw RuntimeException("Unable to deserialize PersonRisks from JSON string", exception)
    }
  }
}

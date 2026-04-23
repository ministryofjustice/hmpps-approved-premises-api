package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.io.Serializable
import java.time.LocalDate

@Suppress("SerialVersionUIDInSerializableClass")
data class RiskWithStatus<T>(val status: RiskStatus, val value: T? = null) : Serializable {
  constructor(value: T?) : this(RiskStatus.Retrieved, value)
}

@Suppress("SerialVersionUIDInSerializableClass")
enum class RiskStatus : Serializable {
  Retrieved,
  NotFound,
  Error,
}

@Suppress("SerialVersionUIDInSerializableClass")
@JsonPropertyOrder("roshRisks", "mappa", "tier", "flags")
data class PersonRisks(
  val roshRisks: RiskWithStatus<RoshRisks>,
  val mappa: RiskWithStatus<Mappa>,
  val tier: RiskWithStatus<RiskTier>,
  val flags: RiskWithStatus<List<String>>,
) : Serializable

@Suppress("SerialVersionUIDInSerializableClass")
@JsonPropertyOrder("overallRisk", "riskToChildren", "riskToPublic", "riskToKnownAdult", "riskToStaff", "lastUpdated")
data class RoshRisks(
  val overallRisk: String,
  val riskToChildren: String,
  val riskToPublic: String,
  val riskToKnownAdult: String,
  val riskToStaff: String,
  val lastUpdated: LocalDate?,
) : Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class Mappa(
  val level: String,
  val lastUpdated: LocalDate,
) : Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class RiskTier(
  val level: String,
  val lastUpdated: LocalDate,
) : Serializable

@Converter(autoApply = true)
class PersonRisksConverter : AttributeConverter<PersonRisks, String?> {
  private val jsonMapper: JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .defaultDateFormat(java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"))
    .build()

  override fun convertToDatabaseColumn(domainForm: PersonRisks?): String? = try {
    jsonMapper.writeValueAsString(domainForm)
  } catch (exception: Exception) {
    throw RuntimeException("Unable to serialize PersonRisks to JSON string for database", exception)
  }

  override fun convertToEntityAttribute(dbData: String?): PersonRisks? = try {
    jsonMapper.readValue(dbData, PersonRisks::class.java)
  } catch (exception: Exception) {
    throw RuntimeException("Unable to deserialize PersonRisks from JSON string", exception)
  }
}

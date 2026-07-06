package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.io.Serializable
import java.time.LocalDate

data class RiskWithStatus<T>(val status: RiskStatus, val value: T? = null) : Serializable {
  companion object {
    private const val serialVersionUID: Long = 1L
  }

  constructor(value: T?) : this(RiskStatus.Retrieved, value)
}

enum class RiskStatus : Serializable {
  Retrieved,
  NotFound,
  Error,
  ;

  companion object {
    private const val serialVersionUID: Long = 1L
  }
}

data class PersonRisks(
  val roshRisks: RiskWithStatus<RoshRisks>,
  val mappa: RiskWithStatus<Mappa>,
  val tier: RiskWithStatus<RiskTier>,
  val flags: RiskWithStatus<List<String>>,
) : Serializable {
  companion object {
    private const val serialVersionUID: Long = 1L
  }
}

data class RoshRisks(
  val overallRisk: String,
  val riskToChildren: String,
  val riskToPublic: String,
  val riskToKnownAdult: String,
  val riskToStaff: String,
  val lastUpdated: LocalDate?,
) : Serializable {
  companion object {
    private const val serialVersionUID: Long = 1L
  }
}

data class Mappa(
  val level: String,
  val lastUpdated: LocalDate,
) : Serializable {
  companion object {
    private const val serialVersionUID: Long = 1L
  }
}

data class RiskTier(
  val level: String,
  val lastUpdated: LocalDate,
  // all risk tiers persisted before this property existed were V2, hence the default value set here
  val version: RiskTierVersion = RiskTierVersion.V2,
) : Serializable {
  companion object {
    private const val serialVersionUID: Long = 1L
  }
}

enum class RiskTierVersion {
  V2,
  V3,
}

@Converter(autoApply = true)
class PersonRisksConverter : AttributeConverter<PersonRisks, String?> {
  private val jsonMapper = jsonMapper { addModule(kotlinModule()) }

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

package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import java.time.LocalDate

data class RiskWithStatus<T>(val status: RiskStatus, val value: T? = null) {
  constructor(value: T?) : this(RiskStatus.Retrieved, value)
}

enum class RiskStatus {
  Retrieved,
  NotFound,
  Error
}

data class PersonRisks(
  val crn: String,
  val roshRisks: RiskWithStatus<RoshRisks>,
  val mappa: RiskWithStatus<Mappa>,
  val tier: RiskWithStatus<RiskTier>
)

data class RoshRisks(
  val overallRisk: String,
  val riskToChildren: String,
  val riskToPublic: String,
  val riskToKnownAdult: String,
  val riskToStaff: String,
  val lastUpdated: LocalDate?
)

data class Mappa(
  val level: String,
  val lastUpdated: LocalDate
)

data class RiskTier(
  val level: String,
  val lastUpdated: LocalDate
)

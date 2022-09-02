package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import java.time.LocalDate

data class PersonRisks(
  val crn: String,
  val roshRisks: RoshRisks,
  val mappa: Mappa,
  val tier: RiskTier
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
  val isNominal: Boolean,
  val lastUpdated: LocalDate
)

data class RiskTier(
  val level: String,
  val lastUpdated: LocalDate
)

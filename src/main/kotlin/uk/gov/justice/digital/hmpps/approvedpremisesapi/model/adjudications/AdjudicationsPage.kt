package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications

import java.time.LocalDateTime

data class AdjudicationsPage(
  val results: Results,
  val agencies: List<Agency>,
)

data class Results(
  val content: List<Adjudication>,
)

data class Adjudication(
  val adjudicationNumber: Long,
  val reportTime: LocalDateTime,
  val agencyIncidentId: Long,
  val agencyId: String,
  val partySeq: Long,
  val adjudicationCharges: List<AdjudicationCharge>,
)

data class AdjudicationCharge(
  val oicChargeId: String?,
  val offenceCode: String?,
  val offenceDescription: String,
  val findingCode: String?,
)

data class Agency(
  val agencyId: String,
  val description: String,
)

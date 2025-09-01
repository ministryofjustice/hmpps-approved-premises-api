package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi

import java.time.LocalDateTime

//data class AdjudicationsPage(
//  val results: List<Adjudication>,
//  val agencies: List<Agency>,
//)
//
//data class Adjudication(
//  val adjudicationNumber: Long,
//  val reportTime: LocalDateTime,
//  val agencyIncidentId: Long,
//  val agencyId: String,
//  val partySeq: Long,
//  val adjudicationCharges: List<AdjudicationCharge>,
//)

data class AdjudicationCharge(
  val oicChargeId: String?,
  val offenceCode: String?,
  val offenceDescription: String,
  val findingCode: String?,
)

data class Agency(
  val agencyId: String,
  val description: String,
  val agencyType: String,
)

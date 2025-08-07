package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AdjudicationCharge
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.LocalDateTime

class AdjudicationFactory : Factory<Adjudication> {
  private var adjudicationNumber: Yielded<Long> = { randomInt(0, 5000).toLong() }
  private var reportTime: Yielded<LocalDateTime> = { LocalDateTime.now().randomDateTimeBefore(30) }
  private var agencyIncidentId: Yielded<Long> = { randomInt(0, 5000).toLong() }
  private var agencyId: Yielded<String> = { "AGNCY" }
  private var partySeq: Yielded<Long> = { randomInt(1, 3).toLong() }
  private var charges: Yielded<List<AdjudicationCharge>> = { listOf() }

  fun withAdjudicationNumber(adjudicationNumber: Long) = apply {
    this.adjudicationNumber = { adjudicationNumber }
  }

  fun withReportTime(reportTime: LocalDateTime) = apply {
    this.reportTime = { reportTime }
  }

  fun withAgencyIncidentId(agencyIncidentId: Long) = apply {
    this.agencyIncidentId = { agencyIncidentId }
  }

  fun withAgencyId(agencyId: String) = apply {
    this.agencyId = { agencyId }
  }

  fun withPartySeq(partySeq: Long) = apply {
    this.partySeq = { partySeq }
  }

  fun withCharges(charges: List<AdjudicationCharge>) = apply {
    this.charges = { charges }
  }

  override fun produce(): Adjudication = Adjudication(
    adjudicationNumber = this.adjudicationNumber(),
    reportTime = this.reportTime(),
    agencyIncidentId = this.agencyIncidentId(),
    agencyId = this.agencyId(),
    partySeq = this.partySeq(),
    adjudicationCharges = this.charges(),
  )
}

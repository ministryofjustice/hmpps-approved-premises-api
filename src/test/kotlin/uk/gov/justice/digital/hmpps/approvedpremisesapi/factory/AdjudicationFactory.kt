package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.manageadjudicationsapi.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.manageadjudicationsapi.HearingDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.manageadjudicationsapi.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.manageadjudicationsapi.OffenceDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.LocalDateTime

class AdjudicationFactory : Factory<Adjudication> {
  private var incidentDetails: Yielded<IncidentDetailsDto> = { randomInt(0, 5000).toLong() }
  private var offenceDetails: Yielded<OffenceDto> = { LocalDateTime.now().randomDateTimeBefore(30) }
  private var status: Yielded<String> = { "AGNCY" }
  private var hearings: Yielded<List<HearingDto>> = { "AGNCY" }

  fun withIncidentDetails(incidentDetails: IncidentDetailsDto) = apply {
    this.incidentDetails = { incidentDetails }
  }

  fun withOffenceDetails(offenceDetails: OffenceDto) = apply {
    this.offenceDetails = { offenceDetails }
  }

  fun withStatus(status: String) = apply {
    this.status = { status }
  }

  fun withHearings(hearings: List<HearingDto>) = apply {
    this.hearings = { hearings }
  }

  override fun produce(): Adjudication = Adjudication(
    incidentDetails = this.incidentDetails(),
    offenceDetails = this.offenceDetails(),
    status = this.status(),
    hearings = this.hearings(),
  )
}

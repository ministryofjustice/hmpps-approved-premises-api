package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.Cas1AuthorisedPlacementPeriod
import java.time.LocalDate

class Cas1AuthorisedPlacementPeriodFactory : Factory<Cas1AuthorisedPlacementPeriod> {
  private var arrival = { LocalDate.now() }
  private var arrivalFlexible: () -> Boolean? = { true }
  private var duration = { 1 }

  fun withArrival(arrival: LocalDate) = apply {
    this.arrival = { arrival }
  }

  fun withArrivalFlexible(arrivalFlexible: Boolean?) = apply {
    this.arrivalFlexible = { arrivalFlexible }
  }

  fun withDuration(duration: Int) = apply {
    this.duration = { duration }
  }

  override fun produce() = Cas1AuthorisedPlacementPeriod(
    arrival = arrival(),
    arrivalFlexible = arrivalFlexible(),
    duration = duration(),
  )
}

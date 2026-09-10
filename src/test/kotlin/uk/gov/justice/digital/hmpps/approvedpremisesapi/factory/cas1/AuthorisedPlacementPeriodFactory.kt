package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.internal.AuthorisedPlacementPeriod
import java.time.LocalDate

class AuthorisedPlacementPeriodFactory : Factory<AuthorisedPlacementPeriod> {
  private var arrival = { LocalDate.now() }
  private var duration = { 1 }

  fun withArrival(arrival: LocalDate) = apply {
    this.arrival = { arrival }
  }
  fun withDuration(duration: Int) = apply {
    this.duration = { duration }
  }

  override fun produce() = AuthorisedPlacementPeriod(
    arrival = arrival(),
    arrivalFlexible = false,
    duration = duration(),
  )
}

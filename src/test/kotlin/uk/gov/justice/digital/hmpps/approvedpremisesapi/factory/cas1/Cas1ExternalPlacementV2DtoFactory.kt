package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPremisesV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1SpaceBookingStatus
import java.time.LocalDate

class Cas1ExternalPlacementV2DtoFactory : Factory<Cas1ExternalPlacementV2Dto> {
  private var status: Yielded<Cas1SpaceBookingStatus> = { Cas1SpaceBookingStatus.UPCOMING }
  private var canonicalArrivalDate: Yielded<LocalDate> = { LocalDate.now() }
  private var canonicalDepartureDate: Yielded<LocalDate> = { LocalDate.now().plusDays(1) }
  private var actualArrivalDate: Yielded<LocalDate?> = { null }
  private var actualDepartureDate: Yielded<LocalDate?> = { null }
  private var expectedArrivalDate: Yielded<LocalDate> = { LocalDate.now() }
  private var expectedDepartureDate: Yielded<LocalDate> = { LocalDate.now().plusDays(1) }
  private var cancellationReason: Yielded<String?> = { null }
  private var premises: Yielded<Cas1ExternalPremisesV2Dto> = {
    Cas1ExternalPremisesV2Dto(
      addressLine1 = "1 Test Street",
      addressLine2 = null,
      town = null,
      postcode = "TE1 1ST",
    )
  }

  fun withStatus(status: Cas1SpaceBookingStatus) = apply {
    this.status = { status }
  }

  fun withActualArrivalDate(actualArrivalDate: LocalDate?) = apply {
    this.actualArrivalDate = { actualArrivalDate }
  }

  fun withActualDepartureDate(actualDepartureDate: LocalDate?) = apply {
    this.actualDepartureDate = { actualDepartureDate }
  }

  fun withExpectedArrivalDate(expectedArrivalDate: LocalDate) = apply {
    this.expectedArrivalDate = { expectedArrivalDate }
  }

  fun withExpectedDepartureDate(expectedDepartureDate: LocalDate) = apply {
    this.expectedDepartureDate = { expectedDepartureDate }
  }

  fun withCancellationReason(cancellationReason: String?) = apply {
    this.cancellationReason = { cancellationReason }
  }

  fun withPremises(premises: Cas1ExternalPremisesV2Dto) = apply {
    this.premises = { premises }
  }

  override fun produce() = Cas1ExternalPlacementV2Dto(
    status = this.status(),
    canonicalArrivalDate = this.canonicalArrivalDate(),
    canonicalDepartureDate = this.canonicalDepartureDate(),
    actualArrivalDate = this.actualArrivalDate(),
    actualDepartureDate = this.actualDepartureDate(),
    expectedArrivalDate = this.expectedArrivalDate(),
    expectedDepartureDate = this.expectedDepartureDate(),
    cancellationReason = this.cancellationReason(),
    premises = this.premises(),
  )
}

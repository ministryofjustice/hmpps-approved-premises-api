package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1KeyWorkerAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingNonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingShortSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TransferReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Cas1SpaceBookingShortSummaryFactory : Factory<Cas1SpaceBookingShortSummary> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<NamedId> = {
    NamedId(
      id = UUID.randomUUID(),
      name = "Test Premises",
      code = null,
    )
  }
  private var createdAt: Yielded<LocalDateTime?> = { null }
  private var expectedArrivalDate: Yielded<LocalDate> = { LocalDate.now() }
  private var expectedDepartureDate: Yielded<LocalDate> = { LocalDate.now() }
  private var actualArrivalDate: Yielded<LocalDate?> = { null }
  private var actualDepartureDate: Yielded<LocalDate?> = { null }
  private var deliusEventNumber: Yielded<String?> = { null }
  private var additionalInformation: Yielded<String?> = { null }
  private var transferReason: Yielded<TransferReason?> = { null }
  private var apArea: Yielded<NamedId> = {
    NamedId(
      id = UUID.randomUUID(),
      name = "Test AP Area",
      code = null,
    )
  }
  private var isNonArrival: Yielded<Boolean?> = { null }
  private var cancellation: Yielded<Cas1SpaceBookingCancellation?> = { null }
  private var characteristics: Yielded<List<Cas1SpaceCharacteristic>> = { emptyList() }
  private var bookedBy: Yielded<User?> = { null }
  private var departure: Yielded<Cas1SpaceBookingDeparture?> = { null }
  private var keyWorkerAllocation: Yielded<Cas1KeyWorkerAllocation?> = { null }
  private var nonArrival: Yielded<Cas1SpaceBookingNonArrival?> = { null }
  private var status: Yielded<Cas1SpaceBookingStatus?> = { null }
  private var statusSetDate: Yielded<LocalDate?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withExpectedArrivalDate(expectedArrivalDate: LocalDate) = apply {
    this.expectedArrivalDate = { expectedArrivalDate }
  }

  fun withExpectedDepartureDate(expectedDepartureDate: LocalDate) = apply {
    this.expectedDepartureDate = { expectedDepartureDate }
  }

  fun withActualArrivalDate(actualArrivalDate: LocalDate?) = apply {
    this.actualArrivalDate = { actualArrivalDate }
  }

  fun withActualDepartureDate(actualDepartureDate: LocalDate?) = apply {
    this.actualDepartureDate = { actualDepartureDate }
  }

  fun withAdditionalInformation(additionalInformation: String?) = apply {
    this.additionalInformation = { additionalInformation }
  }

  fun withTransferReason(transferReason: TransferReason?) = apply {
    this.transferReason = { transferReason }
  }

  fun withStatus(status: Cas1SpaceBookingStatus) = apply {
    this.status = { status }
  }

  fun withStatusSetDate(statusSetDate: LocalDate) = apply {
    this.statusSetDate = { statusSetDate }
  }

  override fun produce() = Cas1SpaceBookingShortSummary(
    id = this.id(),
    premises = this.premises(),
    createdAt = this.createdAt(),
    expectedArrivalDate = this.expectedArrivalDate(),
    expectedDepartureDate = this.expectedDepartureDate(),
    actualArrivalDate = this.actualArrivalDate(),
    actualDepartureDate = this.actualDepartureDate(),
    deliusEventNumber = this.deliusEventNumber(),
    additionalInformation = this.additionalInformation(),
    apArea = this.apArea(),
    isNonArrival = this.isNonArrival(),
    cancellation = this.cancellation(),
    characteristics = this.characteristics(),
    bookedBy = this.bookedBy(),
    departure = this.departure(),
    keyWorkerAllocation = this.keyWorkerAllocation(),
    nonArrival = this.nonArrival(),
    transferReason = this.transferReason(),
    status = this.status(),
    statusSetDate = this.statusSetDate(),
  )
}

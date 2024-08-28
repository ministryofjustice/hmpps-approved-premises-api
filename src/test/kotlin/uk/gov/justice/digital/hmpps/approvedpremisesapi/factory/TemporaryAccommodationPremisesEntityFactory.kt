package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class TemporaryAccommodationPremisesEntityFactory : Factory<TemporaryAccommodationPremisesEntity> {
  private var probationRegion: Yielded<ProbationRegionEntity>? = null
  private var localAuthorityArea: Yielded<LocalAuthorityAreaEntity>? = null
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var postcode: Yielded<String> = { randomPostCode() }
  private var addressLine1: Yielded<String> = { randomStringUpperCase(10) }
  private var addressLine2: Yielded<String> = { randomStringUpperCase(10) }
  private var town: Yielded<String> = { randomStringUpperCase(10) }
  private var notes: Yielded<String> = { randomStringUpperCase(15) }
  private var emailAddress: Yielded<String> = { randomStringUpperCase(10) }
  private var service: Yielded<String> = { ServiceName.temporaryAccommodation.value }
  private var status: Yielded<PropertyStatus> = { randomOf(PropertyStatus.values().asList()) }
  private var probationDeliveryUnit: Yielded<ProbationDeliveryUnitEntity>? = null
  private var turnaroundWorkingDayCount: Yielded<Int>? = null
  private var characteristics: Yielded<MutableList<CharacteristicEntity>> = { mutableListOf() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withAddressLine1(addressLine1: String) = apply {
    this.addressLine1 = { addressLine1 }
  }

  fun withAddressLine2(addressLine2: String) = apply {
    this.addressLine2 = { addressLine2 }
  }

  fun withTown(town: String) = apply {
    this.town = { town }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withService(service: String) = apply {
    this.service = { service }
  }

  fun withPostcode(postcode: String) = apply {
    this.postcode = { postcode }
  }

  fun withProbationRegion(probationRegion: ProbationRegionEntity) = apply {
    this.probationRegion = { probationRegion }
  }

  fun withYieldedProbationRegion(probationRegion: Yielded<ProbationRegionEntity>) = apply {
    this.probationRegion = probationRegion
  }

  fun withLocalAuthorityArea(localAuthorityAreaEntity: LocalAuthorityAreaEntity) = apply {
    this.localAuthorityArea = { localAuthorityAreaEntity }
  }

  fun withYieldedLocalAuthorityArea(localAuthorityAreaEntity: Yielded<LocalAuthorityAreaEntity>) = apply {
    this.localAuthorityArea = localAuthorityAreaEntity
  }

  fun withStatus(status: PropertyStatus) = apply {
    this.status = { status }
  }

  fun withProbationDeliveryUnit(probationDeliveryUnit: ProbationDeliveryUnitEntity) = apply {
    this.probationDeliveryUnit = { probationDeliveryUnit }
  }

  fun withYieldedProbationDeliveryUnit(probationDeliveryUnit: Yielded<ProbationDeliveryUnitEntity>) = apply {
    this.probationDeliveryUnit = probationDeliveryUnit
  }

  fun withTurnaroundWorkingDayCount(turnaroundWorkingDayCount: Int) = apply {
    this.turnaroundWorkingDayCount = { turnaroundWorkingDayCount }
  }

  fun withCharacteristics(characteristics: MutableList<CharacteristicEntity>) = apply {
    this.characteristics = { characteristics }
  }

  fun withUnitTestControlTestProbationAreaAndLocalAuthority() = apply {
    this.withLocalAuthorityArea(
      LocalAuthorityEntityFactory()
        .withIdentifier("LOCALAUTHORITY")
        .produce(),
    )

    this.withProbationRegion(
      ProbationRegionEntityFactory()
        .withDeliusCode("REGION")
        .withApArea(
          ApAreaEntityFactory()
            .withIdentifier("APAREA")
            .produce(),
        )
        .produce(),
    )
  }

  override fun produce(): TemporaryAccommodationPremisesEntity = TemporaryAccommodationPremisesEntity(
    id = this.id(),
    name = this.name(),
    postcode = this.postcode(),
    latitude = null,
    longitude = null,
    probationRegion = this.probationRegion?.invoke() ?: throw RuntimeException("Must provide a probation region"),
    localAuthorityArea = this.localAuthorityArea?.invoke() ?: throw RuntimeException("Must provide a local authority area"),
    bookings = mutableListOf(),
    lostBeds = mutableListOf(),
    addressLine1 = this.addressLine1(),
    addressLine2 = this.addressLine2(),
    town = this.town(),
    notes = this.notes(),
    emailAddress = this.emailAddress(),
    rooms = mutableListOf(),
    characteristics = this.characteristics(),
    status = this.status(),
    probationDeliveryUnit = this.probationDeliveryUnit?.invoke(),
    turnaroundWorkingDayCount = this.turnaroundWorkingDayCount?.invoke() ?: 2,
  )
}

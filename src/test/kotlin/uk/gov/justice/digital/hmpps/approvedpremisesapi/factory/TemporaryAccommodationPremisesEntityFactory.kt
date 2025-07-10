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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPremisesAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.util.UUID

class TemporaryAccommodationPremisesEntityFactory : Factory<TemporaryAccommodationPremisesEntity> {
  private var probationRegion: Yielded<ProbationRegionEntity>? = null
  private var localAuthorityArea: Yielded<LocalAuthorityAreaEntity>? = null
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var postcode: Yielded<String> = { randomPostCode() }
  private var addressLine1: Yielded<String> = { randomPremisesAddress() }
  private var addressLine2: Yielded<String>? = null
  private var town: Yielded<String>? = null
  private var notes: Yielded<String> = { randomStringUpperCase(15) }
  private var emailAddress: Yielded<String> = { randomStringUpperCase(10) }
  private var service: Yielded<String> = { ServiceName.temporaryAccommodation.value }
  private var status: Yielded<PropertyStatus> = { randomOf(PropertyStatus.values().asList()) }
  private var probationDeliveryUnit: Yielded<ProbationDeliveryUnitEntity>? = null
  private var startDate: Yielded<LocalDate> = { LocalDate.now().minusDays(30) }
  private var turnaroundWorkingDays: Yielded<Int> = { 2 }
  private var characteristics: Yielded<MutableList<CharacteristicEntity>> = { mutableListOf() }

  fun withDefaults() = apply {
    withProbationRegion(ProbationRegionEntityFactory().withDefaults().produce())
  }

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

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withTurnaroundWorkingDays(turnaroundWorkingDays: Int) = apply {
    this.turnaroundWorkingDays = { turnaroundWorkingDays }
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
    localAuthorityArea = this.localAuthorityArea?.invoke(),
    bookings = mutableListOf(),
    lostBeds = mutableListOf(),
    addressLine1 = this.addressLine1(),
    addressLine2 = this.addressLine2?.invoke(),
    town = this.town?.invoke(),
    notes = this.notes(),
    emailAddress = this.emailAddress(),
    rooms = mutableListOf(),
    characteristics = this.characteristics(),
    status = this.status(),
    probationDeliveryUnit = this.probationDeliveryUnit?.invoke(),
    startDate = this.startDate.invoke(),
    turnaroundWorkingDays = this.turnaroundWorkingDays.invoke(),
  )
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class Cas3PremisesEntityFactory : Factory<Cas3PremisesEntity> {
  private var localAuthorityArea: Yielded<LocalAuthorityAreaEntity>? = null
  private var probationDeliveryUnit: Yielded<ProbationDeliveryUnitEntity>? = null
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var postcode: Yielded<String> = { randomPostCode() }
  private var addressLine1: Yielded<String> = { randomStringUpperCase(10) }
  private var addressLine2: Yielded<String?> = { randomStringUpperCase(10) }
  private var town: Yielded<String?> = { randomStringUpperCase(10) }
  private var notes: Yielded<String> = { randomStringUpperCase(15) }
  private var service: Yielded<String> = { "CAS3" }
  private var characteristics: Yielded<MutableList<Cas3PremisesCharacteristicEntity>> = { mutableListOf() }
  private var bedspaces: Yielded<MutableList<Cas3BedspacesEntity>> = { mutableListOf() }
  private var status: Yielded<PropertyStatus> = { randomOf(PropertyStatus.entries) }

  fun withDefaults() = apply {
    withDefaultProbationDeliveryUnit()
    withDefaultLocalAuthorityArea()
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

  fun withAddressLine2(addressLine2: String?) = apply {
    this.addressLine2 = { addressLine2 }
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

  fun withLocalAuthorityArea(localAuthorityAreaEntity: LocalAuthorityAreaEntity) = apply {
    this.localAuthorityArea = { localAuthorityAreaEntity }
  }

  private fun withDefaultLocalAuthorityArea() = withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())

  fun withYieldedLocalAuthorityArea(localAuthorityAreaEntity: Yielded<LocalAuthorityAreaEntity>) = apply {
    this.localAuthorityArea = localAuthorityAreaEntity
  }

  fun withProbationDeliveryUnit(probationDeliveryUnit: ProbationDeliveryUnitEntity) = apply {
    this.probationDeliveryUnit = { probationDeliveryUnit }
  }

  private fun withDefaultProbationDeliveryUnit() = withProbationDeliveryUnit(
    ProbationDeliveryUnitEntityFactory()
      .withDefaults()
      .produce(),
  )

  fun withYieldedProbationDeliveryUnit(probationDeliveryUnit: Yielded<ProbationDeliveryUnitEntity>) = apply {
    this.probationDeliveryUnit = probationDeliveryUnit
  }

  fun withCharacteristics(characteristics: MutableList<Cas3PremisesCharacteristicEntity>) = apply {
    this.characteristics = { characteristics }
  }

  fun withBedspaces(bedspaces: MutableList<Cas3BedspacesEntity>) = apply {
    this.bedspaces = { bedspaces }
  }

  fun withStatus(status: PropertyStatus) = apply {
    this.status = { status }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun produce(): Cas3PremisesEntity = Cas3PremisesEntity(
    id = this.id(),
    name = this.name(),
    postcode = this.postcode(),
    localAuthorityArea = this.localAuthorityArea?.invoke()
      ?: throw RuntimeException("Must provide a local authority area"),
    bookings = mutableListOf(),
    addressLine1 = this.addressLine1(),
    addressLine2 = this.addressLine2(),
    town = this.town(),
    notes = this.notes(),
    characteristics = this.characteristics(),
    status = this.status(),
    probationDeliveryUnit = this.probationDeliveryUnit?.invoke()
      ?: throw RuntimeException("Must provide a local authority area"),
    bedspaces = this.bedspaces(),
  )
}

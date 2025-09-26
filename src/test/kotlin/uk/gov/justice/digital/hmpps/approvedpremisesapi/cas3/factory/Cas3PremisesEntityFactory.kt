package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3PremisesEntityFactory : Factory<Cas3PremisesEntity> {
  private var localAuthorityArea: Yielded<LocalAuthorityAreaEntity> = { LocalAuthorityEntityFactory().produce() }
  private var probationDeliveryUnit: Yielded<ProbationDeliveryUnitEntity>? =
    { ProbationDeliveryUnitEntityFactory().produce() }
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
  private var status: Yielded<Cas3PremisesStatus> = { randomOf(Cas3PremisesStatus.entries) }
  private var turnaroundWorkingDays: Yielded<Int> = { 3 }
  private var startDate: Yielded<LocalDate> = { LocalDate.now().minusDays(180) }
  private var endDate: Yielded<LocalDate?> = { null }
  private var createdAt: OffsetDateTime = OffsetDateTime.now()
  private var lastUpdatedAt: OffsetDateTime? = null

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

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withEndDate(endDate: LocalDate?) = apply {
    this.endDate = { endDate }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = createdAt
  }

  fun withLastUpdatedAt(lastUpdatedAt: OffsetDateTime?) = apply {
    this.lastUpdatedAt = lastUpdatedAt
  }

  fun withCharacteristics(characteristics: MutableList<Cas3PremisesCharacteristicEntity>) = apply {
    this.characteristics = { characteristics }
  }

  fun withStatus(status: Cas3PremisesStatus) = apply {
    this.status = { status }
  }

  override fun produce(): Cas3PremisesEntity = Cas3PremisesEntity(
    id = this.id(),
    name = this.name(),
    postcode = this.postcode(),
    localAuthorityArea = this.localAuthorityArea.invoke(),
    bookings = mutableListOf(),
    addressLine1 = this.addressLine1(),
    addressLine2 = this.addressLine2(),
    town = this.town(),
    notes = this.notes(),
    characteristics = this.characteristics(),
    status = this.status(),
    probationDeliveryUnit = this.probationDeliveryUnit!!.invoke(),
    bedspaces = this.bedspaces(),
    turnaroundWorkingDays = this.turnaroundWorkingDays(),
    startDate = this.startDate(),
    endDate = this.endDate(),
    createdAt = this.createdAt,
    lastUpdatedAt = this.lastUpdatedAt,
  )
}

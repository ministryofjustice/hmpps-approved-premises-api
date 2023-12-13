package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationEntityFactory : Factory<Cas2ApplicationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var createdByUser: Yielded<NomisUserEntity>? = null
  private var data: Yielded<String?> = { "{}" }
  private var document: Yielded<String?> = { "{}" }
  private var applicationSchema: Yielded<JsonSchemaEntity> = {
    ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()
  }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var submittedAt: Yielded<OffsetDateTime?> = { null }
  private var statusUpdates: Yielded<MutableList<Cas2StatusUpdateEntity>> = { mutableListOf() }
  private var eventNumber: Yielded<String> = { randomInt(1, 9).toString() }
  private var nomsNumber: Yielded<String> = { randomStringUpperCase(6) }

  private var name: Yielded<String> = { "First Surname" }
  private var dateOfBirth: Yielded<LocalDate> = { LocalDate.now().minusYears(60) }
  private var nationality: Yielded<String?> = { null }
  private var sex: Yielded<String?> = { null }
  private var prisonName: Yielded<String?> = { null }
  private var pncNumber: Yielded<String?> = { null }
  private var personStatus: Yielded<String> = { Cas2NewApplication.PersonStatus.inCustody.name }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withNomsNumber(nomsNumber: String) = apply {
    this.nomsNumber = { nomsNumber }
  }

  fun withCreatedByUser(createdByUser: NomisUserEntity) = apply {
    this.createdByUser = { createdByUser }
  }

  fun withYieldedCreatedByUser(createdByUser: Yielded<NomisUserEntity>) = apply {
    this.createdByUser = createdByUser
  }

  fun withData(data: String?) = apply {
    this.data = { data }
  }

  fun withDocument(document: String?) = apply {
    this.document = { document }
  }

  fun withApplicationSchema(applicationSchema: JsonSchemaEntity) = apply {
    this.applicationSchema = { applicationSchema }
  }

  fun withYieldedApplicationSchema(applicationSchema: Yielded<JsonSchemaEntity>) = apply {
    this.applicationSchema = applicationSchema
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withSubmittedAt(submittedAt: OffsetDateTime?) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withStatusUpdates(statusUpdates: MutableList<Cas2StatusUpdateEntity>) = apply {
    this.statusUpdates = { statusUpdates }
  }

  fun withEventNumber(eventNumber: String) = apply {
    this.eventNumber = { eventNumber }
  }

  fun withNationality(nationality: String) = apply {
    this.nationality = { nationality }
  }

  fun withSex(sex: String) = apply {
    this.sex = { sex }
  }

  fun withPrisonName(prisonName: String) = apply {
    this.prisonName = { prisonName }
  }

  fun withPncNumber(pncNumber: String) = apply {
    this.pncNumber = { pncNumber }
  }

  override fun produce(): Cas2ApplicationEntity = Cas2ApplicationEntity(
    id = this.id(),
    crn = this.crn(),
    createdByUser = this.createdByUser?.invoke() ?: throw RuntimeException("Must provide a createdByUser"),
    data = this.data(),
    document = this.document(),
    schemaVersion = this.applicationSchema(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    statusUpdates = this.statusUpdates(),
    schemaUpToDate = false,
    nomsNumber = this.nomsNumber(),
    name = this.name(),
    sex = this.sex(),
    nationality = this.nationality(),
    dateOfBirth = this.dateOfBirth(),
    prisonName = this.prisonName(),
    pncNumber = this.pncNumber(),
    personStatus = this.personStatus(),
  )
}

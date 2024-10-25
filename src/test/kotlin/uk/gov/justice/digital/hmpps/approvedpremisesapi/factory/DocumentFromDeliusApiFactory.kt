package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class DocumentFromDeliusApiFactory : Factory<APDeliusDocument> {
  private var id: Yielded<String?> = { UUID.randomUUID().toString() }
  private var description: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var level: Yielded<String> = { randomInt(1, 5).toString() }
  private var eventNumber: Yielded<String> = { randomInt(1, 5).toString() }
  private var filename: Yielded<String> = { "${randomStringMultiCaseWithNumbers(5)}.pdf" }
  private var typeCode: Yielded<String> = { randomStringMultiCaseWithNumbers(4) }
  private var typeDescription: Yielded<String> = { randomStringMultiCaseWithNumbers(4) }
  private var dateSaved: Yielded<ZonedDateTime> = { LocalDateTime.now().randomDateTimeBefore(5).atZone(ZoneId.systemDefault()) }
  private var dateCreated: Yielded<ZonedDateTime> = { LocalDateTime.now().randomDateTimeBefore(5).atZone(ZoneId.systemDefault()) }

  fun withId(id: String?) = apply {
    this.id = { id }
  }

  fun withDescription(description: String) = apply {
    this.description = { description }
  }

  fun withLevel(level: String) = apply {
    this.level = { level }
  }

  fun withEventNumber(eventNumber: String) = apply {
    this.eventNumber = { eventNumber }
  }

  fun withFilename(filename: String) = apply {
    this.filename = { filename }
  }

  fun withTypeCode(typeCode: String) = apply {
    this.typeCode = { typeCode }
  }

  fun withTypeDescription(typeDescription: String) = apply {
    this.typeDescription = { typeDescription }
  }

  fun withDateSaved(dateSaved: ZonedDateTime) = apply {
    this.dateSaved = { dateSaved }
  }

  fun withDateCreated(dateCreated: ZonedDateTime) = apply {
    this.dateCreated = { dateCreated }
  }

  override fun produce(): APDeliusDocument = APDeliusDocument(
    id = this.id(),
    description = this.description(),
    level = this.level(),
    eventNumber = this.eventNumber(),
    filename = this.filename(),
    typeCode = this.typeCode(),
    typeDescription = this.typeDescription(),
    dateSaved = this.dateSaved(),
    dateCreated = this.dateCreated(),
  )
}

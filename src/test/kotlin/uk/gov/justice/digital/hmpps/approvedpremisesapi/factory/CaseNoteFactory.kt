package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDateTime

class CaseNoteFactory : Factory<CaseNote> {
  private var caseNoteId: Yielded<String> = { randomStringUpperCase(6) }
  private var offenderIdentifier: Yielded<String> = { randomStringUpperCase(8) }
  private var sensitive: Yielded<Boolean> = { false }
  private var type: Yielded<String> = { if (this.sensitive()) "OMIC" else "ACP" }
  private var typeDescription: Yielded<String?> = { if (this.sensitive()) "OMiC" else "Accredited Programme" }
  private var subType: Yielded<String> = { if (this.sensitive()) "COMM" else "ASSESSMENT" }
  private var subTypeDescription: Yielded<String?> = { if (this.sensitive()) "Communication" else "Assessment" }
  private var source: Yielded<String?> = { null }
  private var creationDateTime: Yielded<LocalDateTime> = { LocalDateTime.now().randomDateTimeBefore(50) }
  private var occurrenceDateTime: Yielded<LocalDateTime> = { LocalDateTime.now().randomDateTimeBefore(50) }
  private var authorUserId: Yielded<String> = { randomInt(0, 1000).toLong().toString() }
  private var authorName: Yielded<String> = { randomStringUpperCase(10) }
  private var text: Yielded<String> = { randomStringUpperCase(20) }
  private var eventId: Yielded<Int> = { randomInt(0, 1000) }
  private var locationId: Yielded<String?> = { null }

  fun withCaseNoteId(caseNoteId: String) = apply {
    this.caseNoteId = { caseNoteId }
  }

  fun withOffenderIdentifier(offenderIdentifier: String) = apply {
    this.offenderIdentifier = { offenderIdentifier }
  }

  fun withSensitive(sensitive: Boolean) = apply {
    this.sensitive = { sensitive }
  }

  fun withEventId(eventId: Int) = apply {
    this.eventId = { eventId }
  }

  fun withType(type: String) = apply {
    this.type = { type }
  }

  fun withTypeDescription(typeDescription: String?) = apply {
    this.typeDescription = { typeDescription }
  }

  fun withSubType(subType: String) = apply {
    this.subType = { subType }
  }

  fun withSubTypeDescription(subTypeDescription: String?) = apply {
    this.subTypeDescription = { subTypeDescription }
  }

  fun withSource(source: String?) = apply {
    this.source = { source }
  }

  fun withCreationDateTime(creationDateTime: LocalDateTime) = apply {
    this.creationDateTime = { creationDateTime }
  }

  fun withOccurrenceDateTime(occurrenceDateTime: LocalDateTime) = apply {
    this.occurrenceDateTime = { occurrenceDateTime }
  }

  fun withAuthorUserId(authorUserId: String) = apply {
    this.authorUserId = { authorUserId }
  }

  fun withAuthorName(authorName: String) = apply {
    this.authorName = { authorName }
  }

  fun withText(text: String) = apply {
    this.text = { text }
  }

  fun withLocationId(locationId: String?) = apply {
    this.locationId = { locationId }
  }

  override fun produce(): CaseNote = CaseNote(
    caseNoteId = this.caseNoteId(),
    type = this.type(),
    typeDescription = this.typeDescription(),
    subType = this.subType(),
    subTypeDescription = this.subTypeDescription(),
    source = this.source(),
    creationDateTime = this.creationDateTime(),
    occurrenceDateTime = this.occurrenceDateTime(),
    authorUserId = this.authorUserId(),
    authorName = this.authorName(),
    text = this.text(),
    locationId = this.locationId(),
    amendments = listOf(),
    offenderIdentifier = this.offenderIdentifier(),
    eventId = this.eventId(),
    sensitive = this.sensitive(),
  )
}

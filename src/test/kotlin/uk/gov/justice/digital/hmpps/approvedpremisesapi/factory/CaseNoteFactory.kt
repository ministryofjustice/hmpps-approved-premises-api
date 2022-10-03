package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDateTime

class CaseNoteFactory : Factory<CaseNote> {
  private var caseNoteId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var bookingId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var type: Yielded<String> = { "ACP" }
  private var typeDescription: Yielded<String?> = { "Accredited Programme" }
  private var subType: Yielded<String> = { "ASSESSMENT" }
  private var subTypeDescription: Yielded<String?> = { "Assessment" }
  private var source: Yielded<String?> = { null }
  private var creationDateTime: Yielded<LocalDateTime> = { LocalDateTime.now().randomDateTimeBefore(50) }
  private var occurrenceDateTime: Yielded<LocalDateTime> = { LocalDateTime.now().randomDateTimeBefore(50) }
  private var staffId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var authorName: Yielded<String> = { randomStringUpperCase(10) }
  private var text: Yielded<String> = { randomStringUpperCase(20) }
  private var originalTextNote: Yielded<String> = { randomStringUpperCase(20) }
  private var agencyId: Yielded<String?> = { randomStringUpperCase(5) }

  fun withCaseNoteId(caseNoteId: Long) = apply {
    this.caseNoteId = { caseNoteId }
  }

  fun withBookingId(bookingId: Long) = apply {
    this.bookingId = { bookingId }
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

  fun withStaffId(staffId: Long) = apply {
    this.staffId = { staffId }
  }

  fun withAuthorName(authorName: String) = apply {
    this.authorName = { authorName }
  }

  fun withText(text: String) = apply {
    this.text = { text }
  }

  fun withOriginalNoteText(originalNoteText: String) = apply {
    this.originalTextNote = { originalNoteText }
  }

  fun withAgencyId(agencyId: String?) = apply {
    this.agencyId = { agencyId }
  }

  override fun produce(): CaseNote = CaseNote(
    caseNoteId = this.caseNoteId(),
    bookingId = this.bookingId(),
    type = this.type(),
    typeDescription = this.typeDescription(),
    subType = this.subType(),
    subTypeDescription = this.subTypeDescription(),
    source = this.source(),
    creationDateTime = this.creationDateTime(),
    occurrenceDateTime = this.occurrenceDateTime(),
    staffId = this.staffId(),
    authorName = this.authorName(),
    text = this.text(),
    originalNoteText = this.originalTextNote(),
    agencyId = this.agencyId(),
    amendments = listOf()
  )
}

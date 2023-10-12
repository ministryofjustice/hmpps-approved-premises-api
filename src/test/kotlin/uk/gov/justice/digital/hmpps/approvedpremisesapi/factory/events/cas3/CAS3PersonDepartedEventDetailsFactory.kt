package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class CAS3PersonDepartedEventDetailsFactory : Factory<CAS3PersonDepartedEventDetails> {
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<Premises> = { PremisesFactory().produce() }
  private var departedAt: Yielded<Instant> = { Instant.now() }
  private var reason: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var notes: Yielded<String> = { randomStringLowerCase(20) }
  private var moveOnCategory: Yielded<MoveOnCategory> = { MoveOnCategoryFactory().produce() }
  private var applicationId: Yielded<UUID?> = { null }
  private var reasonDetail: Yielded<String?> = { null }

  fun withPersonReference(configuration: PersonReferenceFactory.() -> Unit) = apply {
    this.personReference = { PersonReferenceFactory().apply(configuration).produce() }
  }

  fun withDeliusEventNumber(deliusEventNumber: String) = apply {
    this.deliusEventNumber = { deliusEventNumber }
  }

  fun withBookingId(bookingId: UUID) = apply {
    this.bookingId = { bookingId }
  }

  fun withPremises(configuration: PremisesFactory.() -> Unit) = apply {
    this.premises = { PremisesFactory().apply(configuration).produce() }
  }

  fun withDepartedAt(departedAt: Instant) = apply {
    this.departedAt = { departedAt }
  }

  fun withReason(reason: String) = apply {
    this.reason = { reason }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withMoveOnCategory(configuration: MoveOnCategoryFactory.() -> Unit) = apply {
    this.moveOnCategory = { MoveOnCategoryFactory().apply(configuration).produce() }
  }

  fun withApplicationId(applicationId: UUID?) = apply {
    this.applicationId = { applicationId }
  }

  fun withReasonDetail(reasonDetail: String?) = apply {
    this.reasonDetail = { reasonDetail }
  }

  override fun produce() = CAS3PersonDepartedEventDetails(
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    bookingId = this.bookingId(),
    premises = this.premises(),
    departedAt = this.departedAt(),
    reason = this.reason(),
    notes = this.notes(),
    moveOnCategory = this.moveOnCategory(),
    applicationId = this.applicationId(),
    reasonDetail = this.reasonDetail(),
  )
}

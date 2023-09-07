package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cas2ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cas2ApplicationSubmittedSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class Cas2ApplicationSubmittedFactory : Factory<Cas2ApplicationSubmitted> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var age: Yielded<Int> = { randomInt(18, 99) }
  private var gender: Yielded<Cas2ApplicationSubmitted.Gender> = { randomOf(listOf(Cas2ApplicationSubmitted.Gender.male, Cas2ApplicationSubmitted.Gender.female)) }
  private var submittedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var submittedByStaffMember: Yielded<StaffMember> = { StaffMemberFactory().produce() }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  fun withPersonReference(personReference: PersonReference) = apply {
    this.personReference = { personReference }
  }

  fun withGender(gender: Cas2ApplicationSubmitted.Gender) = apply {
    this.gender = { gender }
  }

  fun withSubmittedAt(submittedAt: Instant) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withSubmittedByStaffMember(staffMember: StaffMember) = apply {
    this.submittedByStaffMember = { staffMember }
  }

  override fun produce() = Cas2ApplicationSubmitted(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    age = this.age(),
    gender = this.gender(),
    submittedAt = this.submittedAt(),
    submittedBy = Cas2ApplicationSubmittedSubmittedBy(
      staffMember = this.submittedByStaffMember(),
    ),
  )
}

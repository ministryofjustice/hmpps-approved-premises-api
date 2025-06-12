package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEventDetailsSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class Cas2ApplicationSubmittedEventDetailsFactory : Factory<Cas2ApplicationSubmittedEventDetails> {

  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var referringPrisonCode: Yielded<String> = { "BRI" }
  private var preferredAreas: Yielded<String?> = { "Leeds | Bradford" }
  private var hdcEligibilityDate: Yielded<LocalDate?> = { LocalDate.parse("2023-03-30") }
  private var conditionalReleaseDate: Yielded<LocalDate?> = { LocalDate.parse("2023-04-29") }
  private var submittedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var submittedByStaffMember: Yielded<Cas2StaffMember> = { StaffMemberFactory().produce() }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  fun withPersonReference(personReference: PersonReference) = apply {
    this.personReference = { personReference }
  }

  fun withReferringPrisonCode(referringPrisonCode: String) = apply {
    this.referringPrisonCode = { referringPrisonCode }
  }

  fun withPreferredAreas(preferredAreas: String) = apply {
    this.preferredAreas = { preferredAreas }
  }

  fun withHdcEligibilityDate(hdcEligibilityDate: LocalDate) = apply {
    this.hdcEligibilityDate = { hdcEligibilityDate }
  }

  fun withConditionalReleaseDate(conditionalReleaseDate: LocalDate) = apply {
    this.conditionalReleaseDate = { conditionalReleaseDate }
  }

  fun withSubmittedAt(submittedAt: Instant) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withSubmittedByStaffMember(staffMember: Cas2StaffMember) = apply {
    this.submittedByStaffMember = { staffMember }
  }

  override fun produce() = Cas2ApplicationSubmittedEventDetails(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    referringPrisonCode = this.referringPrisonCode(),
    preferredAreas = this.preferredAreas(),
    hdcEligibilityDate = this.hdcEligibilityDate(),
    conditionalReleaseDate = this.conditionalReleaseDate(),
    submittedAt = this.submittedAt(),
    submittedBy = Cas2ApplicationSubmittedEventDetailsSubmittedBy(
      staffMember = this.submittedByStaffMember(),
    ),
  )
}

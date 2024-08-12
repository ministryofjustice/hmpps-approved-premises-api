package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.IDs
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.OffenderProfile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.ProbationOffenderDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate

class ProbationOffenderDetailFactory : Factory<ProbationOffenderDetail> {
  private var offenderId: Yielded<Long> = { randomInt(100000, 900000).toLong() }
  private var firstName: Yielded<String> = { randomStringUpperCase(8) }
  private var surname: Yielded<String> = { randomStringUpperCase(8) }
  private var dateOfBirth: Yielded<LocalDate> = { LocalDate.now().minusYears(20).randomDateBefore(14) }
  private var gender: Yielded<String?> = { randomOf(listOf("Male", "Female", "Other")) }
  private var otherIds: Yielded<IDs> = { IDs(crn = "CRN") }
  private var offenderProfile: Yielded<OffenderProfile> = { OffenderProfile() }
  private var softDeleted: Yielded<Boolean?> = { false }
  private var currentRestriction: Yielded<Boolean?> = { false }
  private var currentExclusion: Yielded<Boolean?> = { false }

  fun withFirstName(firstName: String) = apply {
    this.firstName = { firstName }
  }

  fun withSurname(surname: String) = apply {
    this.surname = { surname }
  }

  fun withOffenderId(offenderId: Long) = apply {
    this.offenderId = { offenderId }
  }

  fun withDateOfBirth(dateOfBirth: LocalDate) = apply {
    this.dateOfBirth = { dateOfBirth }
  }

  fun withGender(gender: String?) = apply {
    this.gender = { gender }
  }

  fun withOtherIds(otherIds: IDs) = apply {
    this.otherIds = { otherIds }
  }

  fun withOffenderProfile(offenderProfile: OffenderProfile) = apply {
    this.offenderProfile = { offenderProfile }
  }

  fun withCurrentRestriction(currentRestriction: Boolean) = apply {
    this.currentRestriction = { currentRestriction }
  }

  fun withCurrentExclusion(currentExclusion: Boolean) = apply {
    this.currentExclusion = { currentExclusion }
  }

  override fun produce(): ProbationOffenderDetail = ProbationOffenderDetail(
    previousSurname = null,
    offenderId = this.offenderId(),
    title = null,
    firstName = this.firstName(),
    middleNames = null,
    surname = this.surname(),
    dateOfBirth = this.dateOfBirth(),
    gender = this.gender(),
    otherIds = this.otherIds(),
    contactDetails = null,
    offenderProfile = this.offenderProfile(),
    softDeleted = this.softDeleted(),
    currentDisposal = null,
    partitionArea = null,
    currentRestriction = this.currentRestriction(),
    restrictionMessage = null,
    currentExclusion = this.currentExclusion(),
    exclusionMessage = null,
  )
}

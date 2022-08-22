package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderProfile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate

class OffenderDetailsSummaryFactory : Factory<OffenderDetailSummary> {
  private var offenderId: Yielded<Long> = { randomInt(1000, 10000).toLong() }
  private var title: Yielded<String> = { randomOf(listOf("Mr", "Mrs", "Miss")) }
  private var firstName: Yielded<String> = { randomStringUpperCase(6) }
  private var lastName: Yielded<String> = { randomStringUpperCase(10) }
  private var crn: Yielded<String> = { randomStringUpperCase(6) }
  private var gender: Yielded<String> = { randomOf(listOf("Male", "Female", "Other")) }
  private var dateOfBirth: Yielded<LocalDate> = { LocalDate.now().minusYears(20).randomDateBefore() }
  private var currentRestriction: Yielded<Boolean> = { false }
  private var currentExclusion: Yielded<Boolean> = { false }

  fun withOffenderId(id: Long) = apply {
    this.offenderId = { id }
  }

  fun withTitle(title: String) = apply {
    this.title = { title }
  }

  fun withFirstName(firstName: String) = apply {
    this.firstName = { firstName }
  }

  fun withLastName(lastName: String) = apply {
    this.lastName = { lastName }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withGender(gender: String) = apply {
    this.gender = { gender }
  }

  fun withDateOfBirth(dateOfBirth: LocalDate) = apply {
    this.dateOfBirth = { dateOfBirth }
  }

  fun withCurrentRestriction(currentRestriction: Boolean) = apply {
    this.currentRestriction = { currentRestriction }
  }

  fun withCurrentExclusion(currentExclusion: Boolean) = apply {
    this.currentExclusion = { currentExclusion }
  }

  override fun produce() = OffenderDetailSummary(
    offenderId = this.offenderId(),
    title = this.title(),
    firstName = this.firstName(),
    middleNames = listOf(),
    surname = this.lastName(),
    previousSurname = null,
    preferredName = null,
    dateOfBirth = this.dateOfBirth(),
    gender = this.gender(),
    otherIds = OffenderIds(
      crn = this.crn(),
      croNumber = null,
      immigrationNumber = null,
      mostRecentPrisonNumber = null,
      niNumber = null,
      nomsNumber = null,
      pncNumber = null
    ),
    offenderProfile = OffenderProfile(
      ethnicity = null,
      nationality = null,
      secondaryNationality = null,
      notes = null,
      immigrationStatus = null,
      offenderLanguages = null,
      religion = null,
      sexualOrientation = null,
      offenderDetails = null,
      remandStatus = null,
      previousConviction = null,
      riskColour = null,
      disabilities = listOf(),
      genderIdentity = null,
      selfDescribedGender = null
    ),
    softDeleted = null,
    currentDisposal = "",
    partitionArea = null,
    currentRestriction = this.currentRestriction(),
    currentExclusion = this.currentExclusion(),
    isActiveProbationManagedSentence = false
  )
}

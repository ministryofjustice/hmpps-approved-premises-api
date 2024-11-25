package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.RegistrationKeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.RegistrationReview
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.RegistrationStaffHuman
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate

class RegistrationClientResponseFactory : Factory<Registration> {
  private var registrationId: Yielded<Long> = { randomInt(1000, 10000).toLong() }
  private var offenderId: Yielded<Long> = { randomInt(1000, 10000).toLong() }
  private var register: Yielded<RegistrationKeyValue> = {
    RegistrationKeyValue("5", "Public Protection")
  }
  private var type: Yielded<RegistrationKeyValue> = {
    RegistrationKeyValue(
      code = "MAPP",
      description = "MAPPA",
    )
  }
  private var riskColour: Yielded<String> = { "Red" }
  private var startDate: Yielded<LocalDate> = { LocalDate.now().minusMonths(5).randomDateBefore(21) }
  private var nextReviewDate: Yielded<LocalDate> = { LocalDate.now().minusMonths(5).randomDateAfter(21) }
  private var reviewPerioidMonths: Yielded<Int> = { randomInt(6, 12) }
  private var notes: Yielded<String?> = { randomStringMultiCaseWithNumbers(8) }
  private var registeringTeam: Yielded<RegistrationKeyValue> = {
    RegistrationKeyValue(
      code = randomStringMultiCaseWithNumbers(5),
      description = randomStringMultiCaseWithNumbers(10),
    )
  }
  private var registeringOfficer: Yielded<RegistrationStaffHuman> = {
    RegistrationStaffHuman(
      code = randomStringMultiCaseWithNumbers(5),
      forenames = randomStringUpperCase(10),
      surname = randomStringUpperCase(10),
    )
  }
  private var registeringProbationArea: Yielded<RegistrationKeyValue> = {
    RegistrationKeyValue(
      code = randomStringUpperCase(10),
      description = randomStringUpperCase(20),
    )
  }
  private var registerLevel: Yielded<RegistrationKeyValue?> = {
    RegistrationKeyValue(
      code = randomStringUpperCase(10),
      description = randomStringUpperCase(20),
    )
  }
  private var registerCategory: Yielded<RegistrationKeyValue?> = {
    RegistrationKeyValue(
      code = "M2",
      description = "MAPPA Cat 2",
    )
  }
  private var warnUser: Yielded<Boolean> = { false }
  private var active: Yielded<Boolean> = { true }
  private var endDate: Yielded<LocalDate?> = { null }
  private var deregisteringOfficer: Yielded<RegistrationStaffHuman?> = {
    RegistrationStaffHuman(
      code = randomStringUpperCase(10),
      forenames = randomStringUpperCase(10),
      surname = randomStringUpperCase(10),
    )
  }
  private var deregisteringProbationArea: Yielded<RegistrationKeyValue?> = {
    RegistrationKeyValue(
      code = randomStringUpperCase(10),
      description = randomStringUpperCase(20),
    )
  }
  private var deregisteringNotes: Yielded<String?> = { randomStringUpperCase(10) }
  private var numberOfPreviousDeregistrations: Yielded<Int> = { 0 }
  private var registrationReviews: Yielded<List<RegistrationReview>> = { listOf() }

  fun withRegistrationId(registrationId: Long) = apply {
    this.registrationId = { registrationId }
  }

  fun withOffenderId(offenderId: Long) = apply {
    this.offenderId = { offenderId }
  }

  fun withRegister(register: RegistrationKeyValue) = apply {
    this.register = { register }
  }

  fun withType(type: RegistrationKeyValue) = apply {
    this.type = { type }
  }

  fun withRiskColour(riskColour: String) = apply {
    this.riskColour = { riskColour }
  }

  fun withStartDate(startDate: LocalDate) = apply {
    this.startDate = { startDate }
  }

  fun withNextReviewDate(nextReviewDate: LocalDate) = apply {
    this.nextReviewDate = { nextReviewDate }
  }

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  fun withRegisteringTeam(registeringTeam: RegistrationKeyValue) = apply {
    this.registeringTeam = { registeringTeam }
  }

  fun withRegisteringOfficer(registeringOfficer: RegistrationStaffHuman) = apply {
    this.registeringOfficer = { registeringOfficer }
  }

  fun withRegisteringProbationArea(registeringProbationArea: RegistrationKeyValue) = apply {
    this.registeringProbationArea = { registeringProbationArea }
  }

  fun withRegisterLevel(registerLevel: RegistrationKeyValue?) = apply {
    this.registerLevel = { registerLevel }
  }

  fun withRegisterCategory(registerCategory: RegistrationKeyValue?) = apply {
    this.registerCategory = { registerCategory }
  }

  fun withWarnUser(warnUser: Boolean) = apply {
    this.warnUser = { warnUser }
  }

  fun withActive(active: Boolean) = apply {
    this.active = { active }
  }

  fun withEndDate(endDate: LocalDate) = apply {
    this.endDate = { endDate }
  }

  fun withDeregisteringOfficer(deregisteringOfficer: RegistrationStaffHuman?) = apply {
    this.deregisteringOfficer = { deregisteringOfficer }
  }

  fun withDeregisteringProbationArea(deregisteringProbationArea: RegistrationKeyValue?) = apply {
    this.deregisteringProbationArea = { deregisteringProbationArea }
  }

  fun withDeregisteringNotes(deregisteringNotes: String?) = apply {
    this.deregisteringNotes = { deregisteringNotes }
  }

  fun withNumberOfPreviousDeregistrations(numberOfPreviousDeregistrations: Int) = apply {
    this.numberOfPreviousDeregistrations = { numberOfPreviousDeregistrations }
  }

  fun withRegistrationReviews(registrationReviews: List<RegistrationReview>) = apply {
    this.registrationReviews = { registrationReviews }
  }

  override fun produce() = Registration(
    registrationId = this.registrationId(),
    offenderId = this.offenderId(),
    register = this.register(),
    type = this.type(),
    riskColour = this.riskColour(),
    startDate = this.startDate(),
    nextReviewDate = this.nextReviewDate(),
    reviewPeriodMonths = this.reviewPerioidMonths(),
    notes = this.notes(),
    registeringTeam = this.registeringTeam(),
    registeringOfficer = this.registeringOfficer(),
    registeringProbationArea = this.registeringProbationArea(),
    registerLevel = this.registerLevel(),
    registerCategory = this.registerCategory(),
    warnUser = this.warnUser(),
    active = this.active(),
    endDate = this.endDate(),
    deregisteringOfficer = this.deregisteringOfficer(),
    deregisteringProbationArea = this.deregisteringProbationArea(),
    deregisteringNotes = this.deregisteringNotes(),
    numberOfPreviousDeregistrations = this.numberOfPreviousDeregistrations(),
    registrationReviews = this.registrationReviews(),
  )
}

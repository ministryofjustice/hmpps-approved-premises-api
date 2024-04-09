package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Manager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.MappaDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Offence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Profile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Registration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.ZonedDateTime

class CaseDetailFactory : Factory<CaseDetail> {
  var case: Yielded<CaseSummary> = { CaseSummaryFactory().produce() }
  var offences: Yielded<List<Offence>> = { listOf(CaseDetailOffenceFactory().produce()) }
  var registrations: Yielded<List<Registration>> = { listOf(RegistrationFactory().produce()) }
  var mappaDetail: Yielded<MappaDetail> = { MappaDetailFactory().produce() }

  fun withCase(case: CaseSummary) = apply {
    this.case = { case }
  }

  fun withRegistrations(registrations: List<Registration>) = apply {
    this.registrations = { registrations }
  }

  fun withMappaDetail(mappaDetail: MappaDetail) = apply {
    this.mappaDetail = { mappaDetail }
  }

  override fun produce(): CaseDetail = CaseDetail(
    case = this.case(),
    offences = this.offences(),
    registrations = this.registrations(),
    mappaDetail = this.mappaDetail(),
  )
}

class CaseDetailOffenceFactory : Factory<Offence> {
  var description: Yielded<String> = { randomStringLowerCase(10) }
  var date: Yielded<LocalDate> = { LocalDate.now() }
  var main: Yielded<Boolean> = { false }
  var eventNumber: Yielded<String> = { randomStringLowerCase(10) }

  override fun produce(): Offence = Offence(
    description = this.description(),
    date = this.date(),
    main = this.main(),
    eventNumber = this.eventNumber(),
  )
}

class RegistrationFactory : Factory<Registration> {
  var code: Yielded<String> = { randomStringLowerCase(6) }
  var description: Yielded<String> = { randomStringLowerCase(10) }
  var startDate: Yielded<LocalDate> = { LocalDate.now() }

  override fun produce(): Registration = Registration(
    code = this.code(),
    description = this.description(),
    startDate = this.startDate(),
  )
}

class MappaDetailFactory : Factory<MappaDetail> {
  var level: Yielded<Int> = { 10 }
  var levelDescription: Yielded<String> = { randomStringUpperCase(3) }
  var category: Yielded<Int> = { 10 }
  var categoryDescription: Yielded<String> = { randomStringUpperCase(3) }
  var startDate: Yielded<LocalDate> = { LocalDate.now() }
  var lastUpdated: Yielded<ZonedDateTime> = { ZonedDateTime.now() }

  override fun produce(): MappaDetail = MappaDetail(
    level = this.level(),
    levelDescription = this.levelDescription(),
    category = this.category(),
    categoryDescription = this.categoryDescription(),
    startDate = this.startDate(),
    lastUpdated = this.lastUpdated(),
  )
}

class CaseSummaryFactory : Factory<CaseSummary> {
  var crn: Yielded<String> = { randomStringUpperCase(10) }
  var nomsId: Yielded<String?> = { randomStringUpperCase(10) }
  var pnc: Yielded<String?> = { randomStringUpperCase(10) }
  var name: Yielded<Name> = { NameFactory().produce() }
  var dateOfBirth: Yielded<LocalDate> = { LocalDate.now() }
  var gender: Yielded<String> = { randomStringUpperCase(10) }
  var profile: Yielded<Profile> = { ProfileFactory().produce() }
  var manager: Yielded<Manager> = { ManagerFactory().produce() }
  var currentExclusion: Yielded<Boolean> = { false }
  var currentRestriction: Yielded<Boolean> = { false }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }
  fun withNomsId(nomsId: String?) = apply {
    this.nomsId = { nomsId }
  }
  fun withPnc(pnc: String?) = apply {
    this.pnc = { pnc }
  }
  fun withName(name: Name) = apply {
    this.name = { name }
  }
  fun withDateOfBirth(dateOfBirth: LocalDate) = apply {
    this.dateOfBirth = { dateOfBirth }
  }
  fun withGender(gender: String) = apply {
    this.gender = { gender }
  }
  fun withProfile(profile: Profile) = apply {
    this.profile = { profile }
  }
  fun withManager(manager: Manager) = apply {
    this.manager = { manager }
  }
  fun withCurrentExclusion(currentExclusion: Boolean) = apply {
    this.currentExclusion = { currentExclusion }
  }
  fun withCurrentRestriction(currentRestriction: Boolean) = apply {
    this.currentRestriction = { currentRestriction }
  }

  fun fromOffenderDetails(offenderDetails: OffenderDetailSummary) = apply {
    withCrn(offenderDetails.otherIds.crn)
    withNomsId(offenderDetails.otherIds.nomsNumber)
    withName(
      NameFactory()
        .withForename(offenderDetails.firstName)
        .withMiddleNames(offenderDetails.middleNames ?: listOf())
        .withSurname(offenderDetails.surname)
        .produce(),
    )
    withDateOfBirth(offenderDetails.dateOfBirth)
    withGender(offenderDetails.gender)
    withProfile(
      ProfileFactory()
        .withEthnicity(offenderDetails.offenderProfile.ethnicity)
        .withGenderIdentity(offenderDetails.offenderProfile.genderIdentity)
        .withSelfDescribedGender(offenderDetails.offenderProfile.selfDescribedGender)
        .withNationality(offenderDetails.offenderProfile.nationality)
        .withReligion(offenderDetails.offenderProfile.religion)
        .produce(),
    )
    withCurrentExclusion(offenderDetails.currentExclusion)
    withCurrentRestriction(offenderDetails.currentRestriction)
  }

  override fun produce(): CaseSummary = CaseSummary(
    crn = this.crn(),
    nomsId = this.nomsId(),
    pnc = this.pnc(),
    name = this.name(),
    dateOfBirth = this.dateOfBirth(),
    gender = this.gender(),
    profile = this.profile(),
    manager = this.manager(),
    currentExclusion = this.currentExclusion(),
    currentRestriction = this.currentRestriction(),
  )
}

class ProfileFactory : Factory<Profile> {
  var ethnicity: Yielded<String?> = { randomStringLowerCase(10) }
  var genderIdentity: Yielded<String?> = { randomStringLowerCase(10) }
  var selfDescribedGender: Yielded<String?> = { randomStringLowerCase(10) }
  var nationality: Yielded<String?> = { randomStringLowerCase(10) }
  var religion: Yielded<String?> = { randomStringLowerCase(10) }

  fun withEthnicity(ethnicity: String?) = apply {
    this.ethnicity = { ethnicity }
  }
  fun withGenderIdentity(genderIdentity: String?) = apply {
    this.genderIdentity = { genderIdentity }
  }
  fun withSelfDescribedGender(selfDescribedGender: String?) = apply {
    this.selfDescribedGender = { selfDescribedGender }
  }
  fun withNationality(nationality: String?) = apply {
    this.nationality = { nationality }
  }
  fun withReligion(religion: String?) = apply {
    this.religion = { religion }
  }

  override fun produce(): Profile = Profile(
    ethnicity = this.ethnicity(),
    genderIdentity = this.genderIdentity(),
    selfDescribedGender = this.selfDescribedGender(),
    nationality = this.nationality(),
    religion = this.religion(),
  )
}

class NameFactory : Factory<Name> {
  var forename: Yielded<String> = { randomStringUpperCase(10) }
  var surname: Yielded<String> = { randomStringUpperCase(10) }
  var middleNames: Yielded<List<String>> = { listOf(randomStringUpperCase(10)) }

  fun withForename(forename: String) = apply {
    this.forename = { forename }
  }
  fun withSurname(surname: String) = apply {
    this.surname = { surname }
  }
  fun withMiddleNames(middleNames: List<String>) = apply {
    this.middleNames = { middleNames }
  }

  override fun produce(): Name = Name(
    forename = this.forename(),
    surname = this.surname(),
    middleNames = this.middleNames(),
  )
}

class ManagerFactory : Factory<Manager> {
  var team: Yielded<Team> = { TeamFactory().produce() }

  override fun produce(): Manager = Manager(
    team = this.team(),
  )
}

class TeamFactory : Factory<Team> {
  var code: Yielded<String> = { randomStringUpperCase(10) }
  var name: Yielded<String> = { randomStringUpperCase(10) }
  var ldu: Yielded<Ldu> = { LduFactory().produce() }

  override fun produce(): Team = Team(
    code = this.code(),
    name = this.name(),
    ldu = this.ldu(),
  )
}

class LduFactory : Factory<Ldu> {
  var code: Yielded<String> = { randomStringUpperCase(10) }
  var name: Yielded<String> = { randomStringUpperCase(10) }

  override fun produce(): Ldu = Ldu(
    code = this.code(),
    name = this.name(),
  )
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderLanguages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderProfile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.time.LocalDate

class PersonTransformerTest {
  private val personTransformer = PersonTransformer()

  @Test
  fun `transformModelToApi transforms correctly without gender identity`() {
    val offenderDetailSummary = OffenderDetailSummary(
      offenderId = 547839,
      title = "Mr",
      firstName = "Greggory",
      middleNames = listOf(),
      surname = "Someone",
      previousSurname = null,
      preferredName = null,
      dateOfBirth = LocalDate.parse("1980-09-12"),
      gender = "Male",
      otherIds = OffenderIds(
        crn = "CRN123",
        croNumber = null,
        immigrationNumber = null,
        mostRecentPrisonNumber = null,
        niNumber = null,
        nomsNumber = "NOMS321",
        pncNumber = null,
      ),
      offenderProfile = OffenderProfile(
        ethnicity = null,
        nationality = "Spanish",
        secondaryNationality = null,
        notes = null,
        immigrationStatus = null,
        offenderLanguages = OffenderLanguages(
          primaryLanguage = null,
          otherLanguages = listOf(),
          languageConcerns = null,
          requiresInterpreter = null,
        ),
        religion = "Sikh",
        sexualOrientation = null,
        offenderDetails = null,
        remandStatus = null,
        riskColour = null,
        disabilities = listOf(),
        genderIdentity = null,
        selfDescribedGender = null,
      ),
      softDeleted = null,
      currentDisposal = "",
      partitionArea = null,
      currentRestriction = false,
      currentExclusion = false,
      isActiveProbationManagedSentence = false,
    )

    val inmateDetail = InmateDetail(
      offenderNo = "NOMS321",
      inOutStatus = InOutStatus.OUT,
      assignedLivingUnit = null,
    )

    val result = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail)

    assertThat(result).isEqualTo(
      Person(
        crn = "CRN123",
        name = "Greggory Someone",
        dateOfBirth = LocalDate.parse("1980-09-12"),
        sex = "Male",
        status = Person.Status.inCommunity,
        nomsNumber = "NOMS321",
        nationality = "Spanish",
        religionOrBelief = "Sikh",
        genderIdentity = null,
        prisonName = null,
      ),
    )
  }

  @Test
  fun `transformModelToApi transforms correctly with gender identity`() {
    val offenderDetailSummary = OffenderDetailSummary(
      offenderId = 547839,
      title = "Mr",
      firstName = "Greggory",
      middleNames = listOf(),
      surname = "Someone",
      previousSurname = null,
      preferredName = null,
      dateOfBirth = LocalDate.parse("1980-09-12"),
      gender = "Male",
      otherIds = OffenderIds(
        crn = "CRN123",
        croNumber = null,
        immigrationNumber = null,
        mostRecentPrisonNumber = null,
        niNumber = null,
        nomsNumber = "NOMS321",
        pncNumber = null,
      ),
      offenderProfile = OffenderProfile(
        ethnicity = null,
        nationality = "Spanish",
        secondaryNationality = null,
        notes = null,
        immigrationStatus = null,
        offenderLanguages = OffenderLanguages(
          primaryLanguage = null,
          otherLanguages = listOf(),
          languageConcerns = null,
          requiresInterpreter = null,
        ),
        religion = "Sikh",
        sexualOrientation = null,
        offenderDetails = null,
        remandStatus = null,
        riskColour = null,
        disabilities = listOf(),
        genderIdentity = "Female",
        selfDescribedGender = null,
      ),
      softDeleted = null,
      currentDisposal = "",
      partitionArea = null,
      currentRestriction = false,
      currentExclusion = false,
      isActiveProbationManagedSentence = false,
    )

    val inmateDetail = InmateDetail(
      offenderNo = "NOMS321",
      inOutStatus = InOutStatus.OUT,
      assignedLivingUnit = null,
    )

    val result = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail)

    assertThat(result).isEqualTo(
      Person(
        crn = "CRN123",
        name = "Greggory Someone",
        dateOfBirth = LocalDate.parse("1980-09-12"),
        sex = "Male",
        status = Person.Status.inCommunity,
        nomsNumber = "NOMS321",
        nationality = "Spanish",
        religionOrBelief = "Sikh",
        genderIdentity = "Female",
        prisonName = null,
      ),
    )
  }

  @Test
  fun `transformModelToApi transforms correctly with self-described gender identity`() {
    val offenderDetailSummary = OffenderDetailSummary(
      offenderId = 547839,
      title = "Mr",
      firstName = "Greggory",
      middleNames = listOf(),
      surname = "Someone",
      previousSurname = null,
      preferredName = null,
      dateOfBirth = LocalDate.parse("1980-09-12"),
      gender = "Male",
      otherIds = OffenderIds(
        crn = "CRN123",
        croNumber = null,
        immigrationNumber = null,
        mostRecentPrisonNumber = null,
        niNumber = null,
        nomsNumber = "NOMS321",
        pncNumber = null,
      ),
      offenderProfile = OffenderProfile(
        ethnicity = null,
        nationality = "Spanish",
        secondaryNationality = null,
        notes = null,
        immigrationStatus = null,
        offenderLanguages = OffenderLanguages(
          primaryLanguage = null,
          otherLanguages = listOf(),
          languageConcerns = null,
          requiresInterpreter = null,
        ),
        religion = "Sikh",
        sexualOrientation = null,
        offenderDetails = null,
        remandStatus = null,
        riskColour = null,
        disabilities = listOf(),
        genderIdentity = "Female",
        selfDescribedGender = null,
      ),
      softDeleted = null,
      currentDisposal = "",
      partitionArea = null,
      currentRestriction = false,
      currentExclusion = false,
      isActiveProbationManagedSentence = false,
    )

    val inmateDetail = InmateDetail(
      offenderNo = "NOMS321",
      inOutStatus = InOutStatus.OUT,
      assignedLivingUnit = null,
    )

    val result = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail)

    assertThat(result).isEqualTo(
      Person(
        crn = "CRN123",
        name = "Greggory Someone",
        dateOfBirth = LocalDate.parse("1980-09-12"),
        sex = "Male",
        status = Person.Status.inCommunity,
        nomsNumber = "NOMS321",
        nationality = "Spanish",
        religionOrBelief = "Sikh",
        genderIdentity = "Female",
        prisonName = null,
      ),
    )
  }

  @Test
  fun `transformModelToApi transforms correctly when in custody`() {
    val offenderDetailSummary = OffenderDetailSummary(
      offenderId = 547839,
      title = "Mr",
      firstName = "Greggory",
      middleNames = listOf(),
      surname = "Someone",
      previousSurname = null,
      preferredName = null,
      dateOfBirth = LocalDate.parse("1980-09-12"),
      gender = "Male",
      otherIds = OffenderIds(
        crn = "CRN123",
        croNumber = null,
        immigrationNumber = null,
        mostRecentPrisonNumber = null,
        niNumber = null,
        nomsNumber = "NOMS321",
        pncNumber = null,
      ),
      offenderProfile = OffenderProfile(
        ethnicity = null,
        nationality = "Spanish",
        secondaryNationality = null,
        notes = null,
        immigrationStatus = null,
        offenderLanguages = OffenderLanguages(
          primaryLanguage = null,
          otherLanguages = listOf(),
          languageConcerns = null,
          requiresInterpreter = null,
        ),
        religion = "Sikh",
        sexualOrientation = null,
        offenderDetails = null,
        remandStatus = null,
        riskColour = null,
        disabilities = listOf(),
        genderIdentity = null,
        selfDescribedGender = null,
      ),
      softDeleted = null,
      currentDisposal = "",
      partitionArea = null,
      currentRestriction = false,
      currentExclusion = false,
      isActiveProbationManagedSentence = false,
    )

    val inmateDetail = InmateDetail(
      offenderNo = "NOMS321",
      inOutStatus = InOutStatus.IN,
      assignedLivingUnit = AssignedLivingUnit(
        agencyId = "BRI",
        locationId = 5,
        description = "B-2F-004",
        agencyName = "HMP Bristol",
      ),
    )

    val result = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail)

    assertThat(result).isEqualTo(
      Person(
        crn = "CRN123",
        name = "Greggory Someone",
        dateOfBirth = LocalDate.parse("1980-09-12"),
        sex = "Male",
        status = Person.Status.inCustody,
        nomsNumber = "NOMS321",
        nationality = "Spanish",
        religionOrBelief = "Sikh",
        genderIdentity = null,
        prisonName = "HMP Bristol",
      ),
    )
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
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
  fun `transformModelToPersonInfoApi transforms correctly for a restricted person info`() {
    val crn = "CRN123"

    val personInfoResult = PersonInfoResult.Success.Restricted(crn, null)

    val result = personTransformer.transformModelToPersonApi(personInfoResult)

    assertThat(result is RestrictedPerson).isTrue
    assertThat(result.crn).isEqualTo(crn)
  }

  @Test
  fun `transformModelToPersonInfoApi transforms correctly for a not found person info`() {
    val crn = "CRN123"

    val personInfoResult = PersonInfoResult.NotFound(crn)

    val result = personTransformer.transformModelToPersonApi(personInfoResult)

    assertThat(result is UnknownPerson).isTrue
    assertThat(result.crn).isEqualTo(crn)
  }

  @Test
  fun `transformModelToPersonInfoApi transforms correctly for an unknown person info`() {
    val crn = "CRN123"

    val personInfoResult = PersonInfoResult.Unknown(crn)

    val result = personTransformer.transformModelToPersonApi(personInfoResult)

    assertThat(result is UnknownPerson).isTrue
    assertThat(result.crn).isEqualTo(crn)
  }

  @Test
  fun `transformModelToPersonInfoApi transforms correctly for a full person info without prison info`() {
    val crn = "CRN123"

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
        crn = crn,
        croNumber = null,
        immigrationNumber = null,
        mostRecentPrisonNumber = null,
        niNumber = null,
        nomsNumber = "NOMS321",
        pncNumber = null,
      ),
      offenderProfile = OffenderProfile(
        ethnicity = "White and Asian",
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

    val personInfoResult = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = null,
    )

    val result = personTransformer.transformModelToPersonApi(personInfoResult)

    assertThat(result.crn).isEqualTo(crn)
    assertThat(result is FullPerson).isTrue
    assertThat(result).isEqualTo(
      FullPerson(
        type = PersonType.fullPerson,
        crn = "CRN123",
        name = "Greggory Someone",
        dateOfBirth = LocalDate.parse("1980-09-12"),
        sex = "Male",
        status = FullPerson.Status.unknown,
        nomsNumber = null,
        ethnicity = "White and Asian",
        nationality = "Spanish",
        religionOrBelief = "Sikh",
        genderIdentity = null,
        prisonName = null,
      ),
    )
  }

  @Test
  fun `transformModelToPersonInfoApi transforms correctly for a full person info with prison info`() {
    val crn = "CRN123"

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
        crn = crn,
        croNumber = null,
        immigrationNumber = null,
        mostRecentPrisonNumber = null,
        niNumber = null,
        nomsNumber = "NOMS321",
        pncNumber = null,
      ),
      offenderProfile = OffenderProfile(
        ethnicity = "White and Asian",
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

    val personInfoResult = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = inmateDetail,
    )

    val result = personTransformer.transformModelToPersonApi(personInfoResult)

    assertThat(result.crn).isEqualTo(crn)
    assertThat(result is FullPerson).isTrue
    assertThat(result).isEqualTo(
      FullPerson(
        type = PersonType.fullPerson,
        crn = "CRN123",
        name = "Greggory Someone",
        dateOfBirth = LocalDate.parse("1980-09-12"),
        sex = "Male",
        status = FullPerson.Status.inCustody,
        nomsNumber = "NOMS321",
        ethnicity = "White and Asian",
        nationality = "Spanish",
        religionOrBelief = "Sikh",
        genderIdentity = null,
        prisonName = "HMP Bristol",
      ),
    )
  }

  @Test
  fun `transformModelToPersonInfoApi transforms correctly without gender identity`() {
    val crn = "CRN123"

    val offenderDetailSummary = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withGenderIdentity(null)
      .produce()

    val personInfoResult = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = null,
    )

    val result = personTransformer.transformModelToPersonApi(personInfoResult)

    assertThat(result.crn).isEqualTo(crn)
    assertThat(result is FullPerson).isTrue
    result as FullPerson
    assertThat(result.genderIdentity).isEqualTo(null)
  }

  @Test
  fun `transformModelToPersonInfoApi transforms correctly with gender identity`() {
    val crn = "CRN123"

    val offenderDetailSummary = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withGenderIdentity("Male")
      .produce()

    val personInfoResult = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = null,
    )

    val result = personTransformer.transformModelToPersonApi(personInfoResult)

    assertThat(result.crn).isEqualTo(crn)
    assertThat(result is FullPerson).isTrue
    result as FullPerson
    assertThat(result.genderIdentity).isEqualTo("Male")
  }

  @Test
  fun `transformModelToPersonInfoApi transforms correctly with self described gender identity`() {
    val crn = "CRN123"

    val offenderDetailSummary = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withGenderIdentity("Prefer to self-describe")
      .withSelfDescribedGenderIdentity("Other")
      .produce()

    val personInfoResult = PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = null,
    )

    val result = personTransformer.transformModelToPersonApi(personInfoResult)

    assertThat(result.crn).isEqualTo(crn)
    assertThat(result is FullPerson).isTrue
    result as FullPerson
    assertThat(result.genderIdentity).isEqualTo("Other")
  }
}

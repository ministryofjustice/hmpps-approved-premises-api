package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UnknownPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOffenderDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderLanguages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderProfile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.IDs
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.OffenderProfile as ProbationOffenderProfile

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
        status = PersonStatus.unknown,
        nomsNumber = null,
        pncNumber = null,
        ethnicity = "White and Asian",
        nationality = "Spanish",
        religionOrBelief = "Sikh",
        genderIdentity = null,
        prisonName = null,
        isRestricted = false,
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
        pncNumber = "PNC456",
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
      assignedLivingUnit = AssignedLivingUnit(
        agencyId = "BRI",
        locationId = 5,
        description = "B-2F-004",
        agencyName = "HMP Bristol",
      ),
      status = InmateStatus.IN,
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
        status = PersonStatus.inCustody,
        nomsNumber = "NOMS321",
        pncNumber = "PNC456",
        ethnicity = "White and Asian",
        nationality = "Spanish",
        religionOrBelief = "Sikh",
        genderIdentity = null,
        prisonName = "HMP Bristol",
        isRestricted = false,
      ),
    )
  }

  @Test
  fun `transformModelToPersonInfoApi transforms correctly without pnc number`() {
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
      assignedLivingUnit = AssignedLivingUnit(
        agencyId = "BRI",
        locationId = 5,
        description = "B-2F-004",
        agencyName = "HMP Bristol",
      ),
      status = InmateStatus.IN,
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
        status = PersonStatus.inCustody,
        nomsNumber = "NOMS321",
        pncNumber = null,
        ethnicity = "White and Asian",
        nationality = "Spanish",
        religionOrBelief = "Sikh",
        genderIdentity = null,
        prisonName = "HMP Bristol",
        isRestricted = false,
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
  fun `transformModelToPersonInfoApi transforms correctly with an exclusion`() {
    val crn = "CRN123"

    val offenderDetailSummary = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withCurrentExclusion(true)
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
    assertThat(result.isRestricted).isTrue()
  }

  @Test
  fun `transformModelToPersonInfoApi transforms correctly with a restriction`() {
    val crn = "CRN123"

    val offenderDetailSummary = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withCurrentRestriction(true)
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
    assertThat(result.isRestricted).isTrue()
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

  @Test
  fun `transformSummaryToPersonApi returns the correct response for a full person info`() {
    val caseSummary = CaseSummaryFactory().produce()

    val personInfoResult = PersonSummaryInfoResult.Success.Full(caseSummary.crn, caseSummary)

    val result = personTransformer.transformSummaryToPersonApi(personInfoResult)

    assertThat(result.crn).isEqualTo(caseSummary.crn)
    assertThat(result is FullPerson).isTrue

    assertThat(result).isEqualTo(
      FullPerson(
        type = PersonType.fullPerson,
        crn = caseSummary.crn,
        name = "${caseSummary.name.forename} ${caseSummary.name.surname}",
        dateOfBirth = caseSummary.dateOfBirth,
        sex = caseSummary.gender!!,
        status = PersonStatus.unknown,
        nomsNumber = caseSummary.nomsId,
        ethnicity = caseSummary.profile!!.ethnicity,
        nationality = caseSummary.profile!!.nationality,
        religionOrBelief = caseSummary.profile!!.religion,
        genderIdentity = caseSummary.profile!!.genderIdentity,
        prisonName = null,
        isRestricted = false,
      ),
    )
  }

  @Test
  fun `transformSummaryToPersonApi returns the correct response for a restricted person info`() {
    val crn = "CRN123"
    val personInfoResult = PersonSummaryInfoResult.Success.Restricted(crn, null)

    val result = personTransformer.transformSummaryToPersonApi(personInfoResult)

    assertThat(result.crn).isEqualTo(crn)
    assertThat(result is RestrictedPerson).isTrue
  }

  @Test
  fun `transformSummaryToPersonApi returns the correct response for a not found person info`() {
    val crn = "CRN123"
    val personInfoResult = PersonSummaryInfoResult.NotFound(crn)

    val result = personTransformer.transformSummaryToPersonApi(personInfoResult)

    assertThat(result.crn).isEqualTo(crn)
    assertThat(result is UnknownPerson).isTrue
  }

  @Nested
  inner class transformProbationOffenderToPersonApi {
    @Test
    fun `transformProbationOffenderToPersonApi returns the correct response when all data is present`() {
      val nomsNumber = "NOMS"
      val crn = "CRN"
      val pncNumber = "PNC"
      val inmateDetail = InmateDetailFactory()
        .withStatus(status = InmateStatus.IN)
        .withAssignedLivingUnit(assignedLivingUnit = AssignedLivingUnit(agencyId = "1", locationId = 1, description = "description", agencyName = "HMPS Sheffield"))
        .produce()
      val probationOffenderDetail = ProbationOffenderDetailFactory()
        .withOtherIds(otherIds = IDs(nomsNumber = nomsNumber, crn = crn, pncNumber = pncNumber))
        .withOffenderProfile(offenderProfile = ProbationOffenderProfile(nationality = "British", religion = "Atheist"))
        .produce()

      val probationOffenderSearchResult = ProbationOffenderSearchResult.Success.Full(nomsNumber, probationOffenderDetail, inmateDetail)

      val result = personTransformer.transformProbationOffenderToPersonApi(probationOffenderSearchResult, nomsNumber)

      assertThat(result.crn).isEqualTo(crn)

      assertThat(result).isEqualTo(
        FullPerson(
          type = PersonType.fullPerson,
          crn = crn,
          name = "${probationOffenderDetail.firstName} ${probationOffenderDetail.surname}",
          dateOfBirth = probationOffenderDetail.dateOfBirth!!,
          sex = probationOffenderDetail.gender!!,
          status = PersonStatus.inCustody,
          nomsNumber = nomsNumber,
          pncNumber = pncNumber,
          nationality = probationOffenderDetail.offenderProfile?.nationality!!,
          prisonName = inmateDetail.assignedLivingUnit?.agencyName,
          isRestricted = false,
        ),
      )
    }

    @Test
    fun `transformProbationOffenderToPersonApi returns the correct response when missing sex, pncNumber, nationality and prison name`() {
      val nomsNumber = "NOMS"
      val crn = "CRN"
      val inmateDetail = InmateDetailFactory()
        .withStatus(status = InmateStatus.IN)
        .produce()
      val probationOffenderDetail = ProbationOffenderDetailFactory()
        .withGender(null)
        .withOtherIds(otherIds = IDs(nomsNumber = nomsNumber, crn = crn))
        .withOffenderProfile(offenderProfile = ProbationOffenderProfile(religion = "Atheist"))
        .produce()

      val probationOffenderSearchResult = ProbationOffenderSearchResult.Success.Full(nomsNumber, probationOffenderDetail, inmateDetail)

      val result = personTransformer.transformProbationOffenderToPersonApi(probationOffenderSearchResult, nomsNumber)

      assertThat(result.crn).isEqualTo(crn)

      assertThat(result).isEqualTo(
        FullPerson(
          type = PersonType.fullPerson,
          crn = crn,
          name = "${probationOffenderDetail.firstName} ${probationOffenderDetail.surname}",
          dateOfBirth = probationOffenderDetail.dateOfBirth!!,
          sex = "Not found",
          status = PersonStatus.inCustody,
          nomsNumber = nomsNumber,
          pncNumber = "Not found",
          nationality = "Not found",
          prisonName = inmateDetail.assignedLivingUnit?.agencyName,
          isRestricted = false,
        ),
      )
    }
  }
}

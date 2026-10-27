package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcOffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesEmptyResponseForCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesErrorResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesSingleCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextVerifyUserAccessCalls
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockNotFoundInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockSuccessfulInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIVerifyInmateDetailsCalls
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

/**
 * These tests validate the LAO behaviour of [Cas2HdcOffenderService.getPersonByNomsNumberAndActiveCaseLoadId]
 *
 * Like CAS2, CAS2 HDC doesn't use a LaoStrategy and never checks the user's case access.
 * Any offender with a current restriction is treated as forbidden for every user, and a
 * current exclusion has no effect. Non-LAO offenders are then only visible to users
 * whose active caseload matches the offender's prison
 */
class Cas2HdcLaoIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var cas2HdcOffenderService: Cas2HdcOffenderService

  enum class ExpectedResult { FULL, FORBIDDEN }

  @Nested
  inner class GetPersonByNomsNumberAndActiveCaseLoadId {

    @ParameterizedTest(
      name = "currentExclusion={0}, currentRestriction={1} -> {2} (prison API called={3})",
    )
    @CsvSource(
      // currentExclusion, currentRestriction, expectedResult, prisonApiCalled
      value = [
        "false,            false,              FULL,           true",
        "false,            true,               FORBIDDEN,      false",
        "true,             false,              FULL,           true",
        "true,             true,               FORBIDDEN,      false",
      ],
    )
    fun `returns the expected result for each combination of offender LAO status when the user is in the same prison`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
      expectedResult: ExpectedResult,
      prisonApiCalled: Boolean,
    ) {
      val caseSummary = CaseSummaryFactory()
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()
      val nomsNumber = caseSummary.nomsId!!
      val prisonId = randomStringUpperCase(3)
      val inmateDetail = givenInmateDetailInPrison(nomsNumber, prisonId)
      apDeliusContextCaseSummariesSingleCase(caseSummary)

      val result = cas2HdcOffenderService.getPersonByNomsNumberAndActiveCaseLoadId(nomsNumber, prisonId)

      when (expectedResult) {
        ExpectedResult.FULL -> assertThat(result).isEqualTo(
          ProbationOffenderSearchResult.Success.Full(nomsNumber, caseSummary, inmateDetail),
        )

        ExpectedResult.FORBIDDEN -> assertThat(result).isEqualTo(ProbationOffenderSearchResult.Forbidden(nomsNumber))
      }

      prisonAPIVerifyInmateDetailsCalls(nomsNumber, times = if (prisonApiCalled) 1 else 0)
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns Forbidden for a non LAO offender when the user is in a different prison`() {
      val caseSummary = CaseSummaryFactory().produce()
      val nomsNumber = caseSummary.nomsId!!
      givenInmateDetailInPrison(nomsNumber, "PRISON1")
      apDeliusContextCaseSummariesSingleCase(caseSummary)

      val result = cas2HdcOffenderService.getPersonByNomsNumberAndActiveCaseLoadId(nomsNumber, "PRISON2")

      assertThat(result).isEqualTo(ProbationOffenderSearchResult.Forbidden(nomsNumber))
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns NotFound when the offender is not found in the prison API`() {
      val caseSummary = CaseSummaryFactory().produce()
      val nomsNumber = caseSummary.nomsId!!
      prisonAPIMockNotFoundInmateDetailsCall(nomsNumber)
      apDeliusContextCaseSummariesSingleCase(caseSummary)

      val result = cas2HdcOffenderService.getPersonByNomsNumberAndActiveCaseLoadId(nomsNumber, "PRISON1")

      assertThat(result).isEqualTo(ProbationOffenderSearchResult.NotFound(nomsNumber))
    }

    @Test
    fun `returns NotFound when the offender does not exist`() {
      val nomsNumber = randomStringUpperCase(8)
      apDeliusContextCaseSummariesEmptyResponseForCrn(nomsNumber)

      val result = cas2HdcOffenderService.getPersonByNomsNumberAndActiveCaseLoadId(nomsNumber, "PRISON1")

      assertThat(result).isEqualTo(ProbationOffenderSearchResult.NotFound(nomsNumber))
      apDeliusContextVerifyUserAccessCalls(times = 0)
    }

    @Test
    fun `returns NotFound when the case summaries call returns a 404`() {
      val nomsNumber = randomStringUpperCase(8)
      apDeliusContextCaseSummariesErrorResponse(404)

      val result = cas2HdcOffenderService.getPersonByNomsNumberAndActiveCaseLoadId(nomsNumber, "PRISON1")

      assertThat(result).isEqualTo(ProbationOffenderSearchResult.NotFound(nomsNumber))
    }

    @Test
    fun `returns Forbidden when the case summaries call returns a 403`() {
      val nomsNumber = randomStringUpperCase(8)
      apDeliusContextCaseSummariesErrorResponse(403)

      val result = cas2HdcOffenderService.getPersonByNomsNumberAndActiveCaseLoadId(nomsNumber, "PRISON1")

      assertThat(result).isInstanceOf(ProbationOffenderSearchResult.Forbidden::class.java)
      assertThat(result.nomsNumber).isEqualTo(nomsNumber)
    }

    @Test
    fun `returns Unknown when the case summaries call fails unexpectedly`() {
      val nomsNumber = randomStringUpperCase(8)
      apDeliusContextCaseSummariesErrorResponse(500)

      val result = cas2HdcOffenderService.getPersonByNomsNumberAndActiveCaseLoadId(nomsNumber, "PRISON1")

      assertThat(result).isInstanceOf(ProbationOffenderSearchResult.Unknown::class.java)
      assertThat(result.nomsNumber).isEqualTo(nomsNumber)
    }
  }

  private fun givenInmateDetailInPrison(nomsNumber: String, prisonId: String): InmateDetail {
    val inmateDetail = InmateDetailFactory()
      .withOffenderNo(nomsNumber)
      .withAssignedLivingUnit(
        AssignedLivingUnit(
          agencyId = prisonId,
          locationId = 1,
          description = null,
          agencyName = "Prison $prisonId",
        ),
      )
      .produce()
    prisonAPIMockSuccessfulInmateDetailsCall(inmateDetail)
    return inmateDetail
  }
}

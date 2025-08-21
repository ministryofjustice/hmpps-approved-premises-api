package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.transformCas2UserEntityToNomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import java.time.LocalDate
import java.time.OffsetDateTime

@SuppressWarnings("UnusedPrivateProperty")
class Cas2OffenderServiceTest {
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockApOASysContextApiClient = mockk<ApOASysContextApiClient>()
  private val mockOffenderDetailsDataSource = mockk<OffenderDetailsDataSource>()

  private val offenderService = Cas2OffenderService(
    mockPrisonsApiClient,
    mockApDeliusContextApiClient,
    mockApOASysContextApiClient,
    mockOffenderDetailsDataSource,
    2,
  )

  @Nested
  inner class GetRisksByCrn {
    // Note that Tier, Mappa and Flags are all hardcoded to NotFound
    // and these unused 'envelopes' will be removed.

    @Test
    fun `returns NotFound result when Community API Client returns 404`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.NOT_FOUND, null)

      assertThat(offenderService.getRiskByCrn("a-crn") is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `throws when Community API Client returns other non-2xx status code`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.BAD_REQUEST, null)

      val exception = assertThrows<RuntimeException> { offenderService.getRiskByCrn("a-crn") }
      assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/a-crn: 400 BAD_REQUEST")
    }

    @Test
    fun `returns NotFound envelope for RoSH when client returns 404`() {
      val crn = "a-crn"

      mockExistingNonLaoOffender()
      mock404RoSH(crn)

      val result = offenderService.getRiskByCrn(crn)
      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.NotFound)

      assertThat(result.entity.tier.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.mappa.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.flags.status).isEqualTo(RiskStatus.NotFound)
    }

    @Test
    fun `returns Error envelope for RoSH when client returns 500`() {
      val crn = "a-crn"

      mockExistingNonLaoOffender()
      mock500RoSH(crn)

      val result = offenderService.getRiskByCrn(crn)
      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.Error)

      assertThat(result.entity.tier.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.mappa.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.flags.status).isEqualTo(RiskStatus.NotFound)
    }

    @Test
    fun `returns Retrieved envelopes with expected contents for RoSH when client returns 200`() {
      val crn = "a-crn"

      mockExistingNonLaoOffender()

      mock200RoSH(
        crn,
        RoshRatingsFactory().apply {
          withDateCompleted(OffsetDateTime.parse("2022-09-06T13:45:00Z"))
          withAssessmentId(34853487)
          withRiskChildrenCommunity(RiskLevel.LOW)
          withRiskPublicCommunity(RiskLevel.MEDIUM)
          withRiskKnownAdultCommunity(RiskLevel.HIGH)
          withRiskStaffCommunity(RiskLevel.VERY_HIGH)
        }.produce(),
      )

      val result = offenderService.getRiskByCrn(crn)
      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.Retrieved)
      result.entity.roshRisks.value!!.let {
        assertThat(it.lastUpdated).isEqualTo(LocalDate.parse("2022-09-06"))
        assertThat(it.overallRisk).isEqualTo("Very High")
        assertThat(it.riskToChildren).isEqualTo("Low")
        assertThat(it.riskToPublic).isEqualTo("Medium")
        assertThat(it.riskToKnownAdult).isEqualTo("High")
        assertThat(it.riskToStaff).isEqualTo("Very High")
      }

      assertThat(result.entity.tier.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.tier.value).isNull()

      assertThat(result.entity.mappa.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.mappa.value).isNull()

      assertThat(result.entity.flags.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.flags.value).isNull()
    }
  }

  @Nested
  inner class GetOffenderByCrn {
    @Test
    fun `returns NotFound result when Client returns 404`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.NOT_FOUND, null)

      assertThat(offenderService.getOffenderByCrnDeprecated("a-crn") is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `throws when Client returns other non-2xx status code except 403`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.BAD_REQUEST, null)

      val exception = assertThrows<RuntimeException> { offenderService.getOffenderByCrnDeprecated("a-crn") }
      assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/a-crn: 400 BAD_REQUEST")
    }
  }

  @Nested
  inner class GetPersonByNomsNumber {
    val nomsNumber = "DEF123"
    private val currentUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).withActiveNomisCaseloadId("my-prison").produce()

    @Test
    fun `returns NotFound result when Probation Offender Search returns 404`() {
      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        StatusCode(HttpMethod.POST, "/case-summaries", HttpStatus.NOT_FOUND, null)

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber, transformCas2UserEntityToNomisUserEntity(currentUser)) is ProbationOffenderSearchResult.NotFound).isTrue
    }

    @Test
    fun `returns NotFound result when Probation Offender Search does not return a matching offender`() {
      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        ClientResult.Success(HttpStatus.OK, CaseSummaries(emptyList()))

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber, transformCas2UserEntityToNomisUserEntity(currentUser)) is ProbationOffenderSearchResult.NotFound).isTrue
    }

    @Test
    fun `returns Full Person when Probation Offender Search and Prison API returns a matching offender if offender has exclusion`() {
      val crn = "ABC123"
      val offenderDetails = CaseSummaryFactory()
        .withCrn(crn)
        .withCurrentExclusion(true)
        .withNomsId(nomsNumber)
        .produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offenderDetails)))

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .withAssignedLivingUnit(
          AssignedLivingUnit(
            agencyId = "my-prison",
            agencyName = "My Prison",
            locationId = 6,
            description = "",
          ),
        )
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      val result = offenderService.getPersonByNomsNumber(nomsNumber, transformCas2UserEntityToNomisUserEntity(currentUser))

      assertThat(result is ProbationOffenderSearchResult.Success.Full).isTrue
      result as ProbationOffenderSearchResult.Success.Full
      assertThat(result.caseSummary.crn).isEqualTo(crn)
      assertThat(result.caseSummary.nomsId).isEqualTo(offenderDetails.nomsId)
      assertThat(result.inmateDetail).isEqualTo(inmateDetail)
      assertThat(result.inmateDetail?.offenderNo).isEqualTo(nomsNumber)
    }

    @Test
    fun `returns Forbidden result when the matching offender has restriction`() {
      val offenderDetails = CaseSummaryFactory()
        .withCurrentRestriction(true)
        .withNomsId(nomsNumber)
        .produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offenderDetails)))

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber, transformCas2UserEntityToNomisUserEntity(currentUser)) is ProbationOffenderSearchResult.Forbidden).isTrue
    }

    @Test
    fun `returns Forbidden result when the matching offender is not in the same prison as the user`() {
      val crn = "ABC123"
      val offenderDetails = CaseSummaryFactory()
        .withCrn(crn)
        .withNomsId(nomsNumber)
        .produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offenderDetails)))

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .withAssignedLivingUnit(
          AssignedLivingUnit(
            agencyId = "not-my-prison",
            agencyName = "Not My Prison",
            locationId = 5,
            description = "",
          ),
        )
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber, transformCas2UserEntityToNomisUserEntity(currentUser)) is ProbationOffenderSearchResult.Forbidden).isTrue
    }

    @Test
    fun `returns Unknown if Probation Offender Search responds with a 500`() {
      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        StatusCode(HttpMethod.POST, "/case-summaries", HttpStatus.INTERNAL_SERVER_ERROR, null)

      val result = offenderService.getPersonByNomsNumber(nomsNumber, transformCas2UserEntityToNomisUserEntity(currentUser))

      assertThat(result is ProbationOffenderSearchResult.Unknown).isTrue
      result as ProbationOffenderSearchResult.Unknown
      assertThat(result.throwable).isNotNull()
    }

    @Test
    fun `returns not found if Prison API does not find a matching offender`() {
      val crn = "ABC123"
      val offenderDetails = CaseSummaryFactory()
        .withCrn(crn)
        .withNomsId(nomsNumber)
        .produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offenderDetails)))

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.BAD_GATEWAY, null)

      val result = offenderService.getPersonByNomsNumber(nomsNumber, transformCas2UserEntityToNomisUserEntity(currentUser))

      assertThat(result is ProbationOffenderSearchResult.NotFound).isTrue
    }

    @Test
    fun `returns Full Person when Probation Offender Search and Prison API returns a matching offender`() {
      val crn = "ABC123"
      val offenderDetails = CaseSummaryFactory()
        .withCrn(crn)
        .withNomsId(nomsNumber)
        .produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offenderDetails)))

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .withAssignedLivingUnit(
          AssignedLivingUnit(
            agencyId = "my-prison",
            agencyName = "My Prison",
            locationId = 6,
            description = "",
          ),
        )
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      val result = offenderService.getPersonByNomsNumber(nomsNumber, transformCas2UserEntityToNomisUserEntity(currentUser))

      assertThat(result is ProbationOffenderSearchResult.Success.Full).isTrue
      result as ProbationOffenderSearchResult.Success.Full
      assertThat(result.caseSummary.crn).isEqualTo(crn)
      assertThat(result.caseSummary.nomsId).isEqualTo(offenderDetails.nomsId)
      assertThat(result.inmateDetail).isEqualTo(inmateDetail)
      assertThat(result.inmateDetail?.offenderNo).isEqualTo(nomsNumber)
    }
  }

  @Nested
  inner class GetInmateDetailByNomsNumber {
    @Test
    fun `returns not found result when for Offender without Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

      val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns not found result when for Offender with Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

      val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns unauthorised result when Client responds with 403`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.FORBIDDEN, null)

      val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `returns successfully when Client responds with 200`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        HttpStatus.OK,
        InmateDetail(
          offenderNo = nomsNumber,
          custodyStatus = InmateStatus.IN,
          assignedLivingUnit = AssignedLivingUnit(
            agencyId = "AGY",
            locationId = 89,
            description = "AGENCY DESCRIPTION",
            agencyName = "AGENCY NAME",
          ),
        ),
      )

      val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.Success)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isNotNull
      assertThat(result.entity!!.offenderNo).isEqualTo(nomsNumber)
      assertThat(result.entity!!.custodyStatus).isEqualTo(InmateStatus.IN)
      assertThat(result.entity!!.assignedLivingUnit).isEqualTo(
        AssignedLivingUnit(
          agencyId = "AGY",
          locationId = 89,
          description = "AGENCY DESCRIPTION",
          agencyName = "AGENCY NAME",
        ),
      )
    }
  }

  private fun mockExistingNonLaoOffender() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentRestriction(false)
      .withCurrentExclusion(false)
      .produce()

    every {
      mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn")
    } returns ClientResult.Success(HttpStatus.OK, resultBody)
  }

  @Nested
  inner class GetFullInfoForPersonOrThrow {

    @Test
    fun `throws NotFoundProblem when offender is PersonInfoResult-NotFound, status 404`() {
      val crn = "ABC123"
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/ABC123",
        HttpStatus.NOT_FOUND,
        null,
        true,
      )
      assertThrows<NotFoundProblem> { offenderService.getFullInfoForPersonOrThrow(crn) }
    }

    @Test
    fun `throws NotFoundProblem exception when offender is PersonInfoResult-Unknown, status 500`() {
      val crn = "ABC123"
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/ABC123",
        HttpStatus.INTERNAL_SERVER_ERROR,
        null,
        true,
      )
      assertThrows<NotFoundProblem> { offenderService.getFullInfoForPersonOrThrow(crn) }
    }

    @Test
    fun `returns PersonInfoResult-Success-Restricted when LAO is restricted, status 200`() {
      val crn = "ABC123"
      val nomsNumber = "NOMSABC"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .withCurrentRestriction(true)
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      val error = assertThrows<ForbiddenProblem> { offenderService.getFullInfoForPersonOrThrow(crn) }
      assertThat(error.message).isEqualTo("Forbidden: Offender $crn is Restricted.")
    }

    @Test
    fun `returns PersonInfoResult-Success-Full when offender and inmate details are found and LAO is excluded, status 200`() {
      val crn = "ABC123"
      val nomsNumber = "NOMSABC"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .withCurrentExclusion(true)
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      val result = offenderService.getFullInfoForPersonOrThrow(crn)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(inmateDetail)
    }

    @Test
    fun `returns PersonInfoResult-Success-Full when offender and inmate details are found, status 200`() {
      val crn = "ABC123"
      val nomsNumber = "NOMSABC"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      val result = offenderService.getFullInfoForPersonOrThrow(crn)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(inmateDetail)
    }
  }

  @Nested
  inner class GetMapOfPersonsNamesAndCrns {
    val offenderDetailSummary = OffenderDetailsSummaryFactory()
      .withFirstName("ExampleFirst")
      .withLastName("ExampleLast")
      .produce()

    @Test
    fun `returns Not Found when offender is PersonInfoResult-NotFound, status 404`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf("NOTFOUND")) } returns mapOf(
        "NOTFOUND" to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/ABC123",
            HttpStatus.NOT_FOUND,
            null,
            true,
          ),
      )

      val result = offenderService.getMapOfPersonNamesAndCrns(listOf("NOTFOUND"))
      assertThat(result).isEqualTo(mapOf("NOTFOUND" to "Person Not Found"))
    }

    @Test
    fun `returns Unknown when offender returns any other code except 404`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf("UNKNOWN")) } returns mapOf(
        "UNKNOWN" to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/ABC123",
            HttpStatus.FORBIDDEN,
            null,
            true,
          ),
      )

      val result = offenderService.getMapOfPersonNamesAndCrns(listOf("UNKNOWN"))
      assertThat(result).isEqualTo(mapOf("UNKNOWN" to "Unknown"))
    }

    @Test
    fun `returns the person's full name when offender is PersonInfoResult-Full`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf("FULL")) } returns mapOf(
        "FULL" to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetailSummary,
        ),
      )
      val result = offenderService.getMapOfPersonNamesAndCrns(listOf("FULL"))
      assertThat(result).isEqualTo(mapOf("FULL" to "ExampleFirst ExampleLast"))
    }

    @Test
    fun `calls offender API 2 times when crn search limit exceed`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(any<List<String>>()) } returns mapOf(
        "CRN" to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetailSummary,
        ),
      )
      val crns = listOf("CRN1", "CRN2", "CRN3")
      offenderService.getMapOfPersonNamesAndCrns(crns)
      verify(exactly = 1) { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf("CRN1", "CRN2")) }
      verify(exactly = 1) { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf("CRN3")) }
    }
  }

  private fun mock404RoSH(crn: String) = every { mockApOASysContextApiClient.getRoshRatings(crn) } returns StatusCode(HttpMethod.GET, "/rosh/a-crn", HttpStatus.NOT_FOUND, body = null)

  private fun mock500RoSH(crn: String) = every { mockApOASysContextApiClient.getRoshRatings(crn) } returns StatusCode(HttpMethod.GET, "/rosh/a-crn", HttpStatus.INTERNAL_SERVER_ERROR, body = null)

  private fun mock200RoSH(crn: String, body: RoshRatings) = every { mockApOASysContextApiClient.getRoshRatings(crn) } returns ClientResult.Success(HttpStatus.OK, body = body)
}

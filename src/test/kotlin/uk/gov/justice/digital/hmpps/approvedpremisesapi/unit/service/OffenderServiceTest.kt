package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerAlertsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.UserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationsPageFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AgencyFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.CheckUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ClientResultFailureArgumentsProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars

class OffenderServiceTest {
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockPrisonerAlertsApiClient = mockk<PrisonerAlertsApiClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockOffenderDetailsDataSource = mockk<OffenderDetailsDataSource>()

  private val adjudicationsConfigBindingModel = PrisonAdjudicationsConfigBindingModel().apply {
    prisonApiPageSize = 2
  }

  private val offenderService = OffenderService(
    mockPrisonsApiClient,
    mockPrisonerAlertsApiClient,
    mockApDeliusContextApiClient,
    mockOffenderDetailsDataSource,
    adjudicationsConfigBindingModel,
  )

  @Test
  fun `getOffenderByCrn returns NotFound result when Client returns 404`() {
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getOffenderByCrn throws when Client returns other non-2xx status code except 403`() {
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.BAD_REQUEST, null)

    val exception = assertThrows<RuntimeException> { offenderService.getOffenderByCrn("a-crn", "distinguished.name") }
    assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/a-crn: 400 BAD_REQUEST")
  }

  @Test
  fun `getOffenderByCrn returns OffenderDetails without further checks when Offender has no LAO constraints`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .produce()

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)

    val result = offenderService.getOffenderByCrn("a-crn", "distinguished.name")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.otherIds.crn).isEqualTo("a-crn")
    assertThat(result.entity.firstName).isEqualTo("Bob")
    assertThat(result.entity.surname).isEqualTo("Doe")
  }

  @Test
  fun `getOffenderByCrn does not enforce LAO when ignoreLaoRestrictions is enabled (because user has LAO qualification)`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name", ignoreLaoRestrictions = true) is AuthorisableActionResult.Success).isTrue
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when distinguished name is excluded from viewing`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when Client returns 403 with valid user access response body`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns StatusCode(
      HttpMethod.GET,
      "/secure/crn/a-crn/user/distinguished.name/userAccess",
      HttpStatus.FORBIDDEN,
      ObjectMapperFactory.createRuntimeLikeObjectMapper().writeValueAsString(accessBody),
    )

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOffenderByCrn throws when Client returns 403 without valid user access response body (problem with our service to service JWT)`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns StatusCode(
      HttpMethod.GET,
      "/secure/crn/a-crn/user/distinguished.name/userAccess",
      HttpStatus.FORBIDDEN,
      null,
    )

    val exception = assertThrows<RuntimeException> { offenderService.getOffenderByCrn("a-crn", "distinguished.name") }
    assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/crn/a-crn/user/distinguished.name/userAccess: 403 FORBIDDEN")
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when distinguished name is not explicitly allowed to view`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentRestriction(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = true, userExcluded = false, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOffenderByCrn returns OffenderDetails when LAO restrictions are passed`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentRestriction(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = false, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)

    val result = offenderService.getOffenderByCrn("a-crn", "distinguished.name")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.otherIds.crn).isEqualTo("a-crn")
    assertThat(result.entity.firstName).isEqualTo("Bob")
    assertThat(result.entity.surname).isEqualTo("Doe")
  }

  @Nested
  inner class CanAccessOffender {

    @Test
    fun `return true and dont make any external calls if IgnoreLimitedAccess strategy`() {
      val crn = randomNumberChars(8)

      val result = offenderService.canAccessOffender(crn, LaoStrategy.NeverRestricted)
      assertThat(result).isTrue()

      verify { mockApDeliusContextApiClient wasNot Called }
    }

    @Test
    fun `return false when crn is user excluded from viewing, ReturnRestrictedIfLimitedAccess strategy`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserExcluded(true)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffender(crn, CheckUserAccess("distinguished.name"))
      assertThat(result).isFalse()
    }

    @Test
    fun `return false when crn is user restricted from viewing, ReturnRestrictedIfLimitedAccess strategy`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserRestricted(true)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffender(crn, CheckUserAccess("distinguished.name"))
      assertThat(result).isFalse()
    }

    @Test
    fun `return true when crn is not user restricted or excluded from viewing, ReturnRestrictedIfLimitedAccess strategy`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserRestricted(false)
        .withUserExcluded(false)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffender(crn, CheckUserAccess("distinguished.name"))
      assertThat(result).isTrue()
    }

    @ParameterizedTest
    @ArgumentsSource(ClientResultFailureArgumentsProvider::class)
    fun `throws exception when getUserAccessForCrns returns client result failure`(response: ClientResult.Failure<UserAccess>) {
      val crn = randomNumberChars(8)

      every { mockApDeliusContextApiClient.getUserAccessForCrns("distinguished.name", listOf(crn)) } returns response

      assertThrows<Throwable> { offenderService.canAccessOffender(crn, CheckUserAccess("distinguished.name")) }
    }
  }

  @Nested
  inner class CanAccessOffenders {

    @Test
    fun `return empty result when crns list is empty`() {
      val result = offenderService.canAccessOffenders("distinguished.name", emptyList())
      assertThat(result.isEmpty()).isTrue()
    }

    @Test
    fun `return error if greater than 500 crns are requested`() {
      assertThatThrownBy {
        offenderService.canAccessOffenders("distinguished.name", (0..500).map { "$it" })
      }.isInstanceOf(InternalServerErrorProblem::class.java)
        .hasMessage("Internal Server Error: Cannot request access details for more than 500 CRNs. 501 have been provided.")
    }

    @Test
    fun `return false when crn is user excluded from viewing`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserExcluded(true)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffenders("distinguished.name", listOf(crn))
      assertThat(result[crn]).isFalse()
    }

    @Test
    fun `return false when crn is user restricted from viewing`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserRestricted(true)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffenders("distinguished.name", listOf(crn))
      assertThat(result[crn]).isFalse()
    }

    @Test
    fun `return true when crn is not user restricted or excluded from viewing`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserRestricted(false)
        .withUserExcluded(false)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffenders("distinguished.name", listOf(crn))
      assertThat(result[crn]).isTrue()
    }

    @Test
    fun `return expected results when multiple crns`() {
      val crns = listOf("CRN1", "CRN2", "CRN3", "CRN4")

      val caseAccess1 = CaseAccessFactory()
        .withCrn("CRN1")
        .withUserRestricted(false)
        .withUserExcluded(false)
        .produce()

      val caseAccess2 = CaseAccessFactory()
        .withCrn("CRN2")
        .withUserRestricted(false)
        .withUserExcluded(true)
        .produce()

      val caseAccess3 = CaseAccessFactory()
        .withCrn("CRN3")
        .withUserRestricted(true)
        .withUserExcluded(false)
        .produce()

      val caseAccess4 = CaseAccessFactory()
        .withCrn("CRN4")
        .withUserRestricted(true)
        .withUserExcluded(false)
        .produce()

      val userAccess = UserAccess(listOf(caseAccess1, caseAccess2, caseAccess3, caseAccess4))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          crns,
        )
      } returns ClientResult.Success(HttpStatus.OK, userAccess)

      val result = offenderService.canAccessOffenders("distinguished.name", crns)

      assertThat(result["CRN1"]).isTrue()
      assertThat(result["CRN2"]).isFalse()
      assertThat(result["CRN3"]).isFalse()
      assertThat(result["CRN4"]).isFalse()
    }

    @ParameterizedTest
    @ArgumentsSource(ClientResultFailureArgumentsProvider::class)
    fun `throws exception when getUserAccessForCrns returns client result failure`(response: ClientResult.Failure<UserAccess>) {
      val crn = randomNumberChars(8)

      every { mockApDeliusContextApiClient.getUserAccessForCrns("distinguished.name", listOf(crn)) } returns response

      assertThrows<Throwable> { offenderService.canAccessOffenders("distinguished.name", listOf(crn)) }
    }
  }

  @Test
  fun `getAdjudicationsByNomsNumber returns NotFound when Adjudications request returns a 404`() {
    val nomsNumber = "NOMS456"

    every {
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 0,
      )
    } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/adjudications", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getAdjudicationsByNomsNumber(nomsNumber) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getAdjudicationsByNomsNumber returns Unauthorised when Case Notes request returns a 403`() {
    val nomsNumber = "NOMS456"

    every {
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 0,
      )
    } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/adjudications", HttpStatus.FORBIDDEN, null)

    assertThat(offenderService.getAdjudicationsByNomsNumber(nomsNumber) is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getAdjudicationsByNomsNumber returns Success, traverses pages from Client`() {
    val nomsNumber = "NOMS456"

    val adjudicationsPageOne = AdjudicationsPageFactory()
      .withResults(
        listOf(
          AdjudicationFactory().withAgencyId("AGNCY1").produce(),
          AdjudicationFactory().withAgencyId("AGNCY2").produce(),
        ),
      )
      .withAgencies(
        listOf(
          AgencyFactory().withAgencyId("AGNCY1").produce(),
          AgencyFactory().withAgencyId("AGNCY2").produce(),
        ),
      )
      .produce()

    val adjudicationsPageTwo = AdjudicationsPageFactory()
      .withResults(
        listOf(
          AdjudicationFactory().withAgencyId("AGNCY3").produce(),
        ),
      )
      .withAgencies(
        listOf(
          AgencyFactory().withAgencyId("AGNCY3").produce(),
        ),
      )
      .produce()

    every {
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 0,
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      adjudicationsPageOne,
    )

    every {
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 2,
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      adjudicationsPageTwo,
    )

    val result = offenderService.getAdjudicationsByNomsNumber(nomsNumber)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.results).containsAll(
      adjudicationsPageOne.results.plus(adjudicationsPageTwo.results),
    )
    assertThat(result.entity.agencies).containsExactlyInAnyOrder(
      *adjudicationsPageOne.agencies.union(adjudicationsPageTwo.agencies).toTypedArray(),
    )
  }

  @Nested
  inner class IsLao {

    @Test
    fun `returns true for Offender with current restriction`() {
      every { mockApDeliusContextApiClient.getCaseSummaries(listOf("CRN123")) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(
          listOf(
            CaseSummaryFactory()
              .withCurrentRestriction(true)
              .withCurrentExclusion(false)
              .produce(),
          ),
        ),
      )

      assertThat(offenderService.isLao("CRN123")).isTrue
    }

    @Test
    fun `returns true for Offender with current exclusion`() {
      every { mockApDeliusContextApiClient.getCaseSummaries(listOf("CRN123")) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(
          listOf(
            CaseSummaryFactory()
              .withCurrentRestriction(false)
              .withCurrentExclusion(true)
              .produce(),
          ),
        ),
      )

      assertThat(offenderService.isLao("CRN123")).isTrue
    }

    @Test
    fun `returns false for Offender without current exclusion or restriction`() {
      every { mockApDeliusContextApiClient.getCaseSummaries(listOf("CRN123")) } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(
          listOf(
            CaseSummaryFactory()
              .withCurrentRestriction(false)
              .withCurrentExclusion(false)
              .produce(),
          ),
        ),
      )

      assertThat(offenderService.isLao("CRN123")).isFalse
    }
  }

  companion object {
    const val OFFENDER_1_CRN = "CRN1"
    const val OFFENDER_1_NOMS: String = "NOMS1"
    const val OFFENDER_2_CRN = "CRN2"
    const val OFFENDER_2_NOMS = "NOMS2"
    const val OFFENDER_3_CRN = "CRN3"
    const val OFFENDER_3_NOMS = "NOMS3"
    const val OFFENDER_4_CRN = "CRN4"
    const val OFFENDER_4_NOMS = "NOMS4"
    const val OFFENDER_5_CRN = "CRN5"
    const val USERNAME = "deliusUsername"
  }

  @Nested
  inner class GetPersonSummaryInfoResults {

    @Test
    fun `if no crns provided immediately return empty list`() {
      val result = offenderService.getPersonSummaryInfoResults(
        crns = emptySet(),
        laoStrategy = CheckUserAccess(USERNAME),
      )

      assertThat(result).isEmpty()
    }

    @ParameterizedTest
    @ArgumentsSource(ClientResultFailureArgumentsProvider::class)
    fun `any error retrieving case summaries is rethrown`(response: ClientResult.Failure<CaseSummaries>) {
      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(OFFENDER_1_CRN))
      } returns response

      assertThrows<Throwable> {
        offenderService.getPersonSummaryInfoResults(
          crns = setOf(OFFENDER_1_CRN),
          laoStrategy = CheckUserAccess(USERNAME),
        )
      }
    }

    @ParameterizedTest
    @ArgumentsSource(ClientResultFailureArgumentsProvider::class)
    fun `any error retrieving case access is rethrown`(response: ClientResult.Failure<UserAccess>) {
      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(
          cases = listOf(CaseSummaryFactory().withCrn(OFFENDER_1_CRN).withCurrentRestriction(true).produce()),
        ),
      )

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns response

      assertThrows<Throwable> {
        offenderService.getPersonSummaryInfoResults(
          crns = setOf(OFFENDER_1_CRN),
          laoStrategy = CheckUserAccess(USERNAME),
        )
      }
    }

    @Test
    fun `single crn, case summary not found, return NotFound`() {
      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(emptyList()))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(emptyList()))

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        laoStrategy = CheckUserAccess(USERNAME),
      )

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.NotFound::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.NotFound
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
    }

    @Test
    fun `single crn with no limited access, return Success`() {
      val offender1CaseSummary = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withNomsId(OFFENDER_1_NOMS)
        .withCurrentExclusion(false)
        .withCurrentRestriction(false)
        .produce()

      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offender1CaseSummary)))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(emptyList()))

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        laoStrategy = CheckUserAccess(USERNAME),
      )

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.Success.Full
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
      assertThat(result0.summary).isSameAs(offender1CaseSummary)
    }

    @ParameterizedTest
    @CsvSource(
      "true,false",
      "false,true",
      "true,true",
    )
    fun `single crn with limited access, ReturnRestrictedIfLimitedAccess strategy, no limited access record returned, return NotFound`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
    ) {
      val offender1CaseSummary = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()

      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offender1CaseSummary)))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(emptyList()))

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        laoStrategy = CheckUserAccess(USERNAME),
      )

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.NotFound::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.NotFound
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
    }

    @ParameterizedTest
    @CsvSource(
      "true,false",
      "false,true",
      "true,true",
    )
    fun `single crn with limited access, ReturnRestrictedIfLimitedAccess strategy, user is not limited, return Success`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
    ) {
      val offender1CaseSummary = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withNomsId(OFFENDER_1_NOMS)
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()

      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offender1CaseSummary)))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(
        HttpStatus.OK,
        UserAccess(
          listOf(
            CaseAccessFactory()
              .withCrn(OFFENDER_1_CRN)
              .withUserExcluded(false)
              .withUserRestricted(false)
              .produce(),
          ),
        ),
      )

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        laoStrategy = CheckUserAccess(USERNAME),
      )

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.Success.Full
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
      assertThat(result0.summary).isSameAs(offender1CaseSummary)
    }

    @ParameterizedTest
    @CsvSource(
      "true,false",
      "false,true",
      "true,true",
    )
    fun `single crn with limited access, ReturnRestrictedIfLimitedAccess strategy, user is limited, return Restricted`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
    ) {
      val offender1CaseSummary = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withNomsId(OFFENDER_1_NOMS)
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()

      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offender1CaseSummary)))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(
        HttpStatus.OK,
        UserAccess(
          listOf(
            CaseAccessFactory()
              .withCrn(OFFENDER_1_CRN)
              .withUserExcluded(currentExclusion)
              .withUserRestricted(currentRestriction)
              .produce(),
          ),
        ),
      )

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        laoStrategy = CheckUserAccess(USERNAME),
      )

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.Success.Restricted::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.Success.Restricted
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
      assertThat(result0.nomsNumber).isEqualTo(OFFENDER_1_NOMS)
    }

    @ParameterizedTest
    @CsvSource(
      "true,false",
      "false,true",
      "true,true",
    )
    fun `single crn with limited access, IgnoreLimitedAccess strategy, user is limited, return Success`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
    ) {
      val offender1CaseSummary = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withNomsId(OFFENDER_1_NOMS)
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()

      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offender1CaseSummary)))

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        laoStrategy = LaoStrategy.NeverRestricted,
      )

      verify(exactly = 0) { mockApDeliusContextApiClient.getUserAccessForCrns(any(), any()) }

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.Success.Full
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
      assertThat(result0.summary).isSameAs(offender1CaseSummary)
    }

    @Test
    fun `multiple crns with all possible return types, don't check permission if no restrictions or exclusions`() {
      val offender1NotLao = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withNomsId(OFFENDER_1_NOMS)
        .withCurrentExclusion(false)
        .withCurrentRestriction(false)
        .produce()

      val offender2RestrictedToUser = CaseSummaryFactory()
        .withCrn(OFFENDER_2_CRN)
        .withNomsId(OFFENDER_2_NOMS)
        .withCurrentExclusion(false)
        .withCurrentRestriction(true)
        .produce()

      val offender3ExcludedButNotToUser = CaseSummaryFactory()
        .withCrn(OFFENDER_3_CRN)
        .withNomsId(OFFENDER_3_NOMS)
        .withCurrentExclusion(true)
        .withCurrentRestriction(false)
        .produce()

      val offender4NotLao = CaseSummaryFactory()
        .withCrn(OFFENDER_4_CRN)
        .withNomsId(OFFENDER_4_NOMS)
        .withCurrentExclusion(false)
        .withCurrentRestriction(false)
        .produce()

      every {
        mockApDeliusContextApiClient.getCaseSummaries(
          listOf(
            OFFENDER_1_CRN,
            OFFENDER_2_CRN,
            OFFENDER_3_CRN,
            OFFENDER_4_CRN,
            OFFENDER_5_CRN,
          ),
        )
      } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(
          listOf(
            offender1NotLao,
            offender2RestrictedToUser,
            offender3ExcludedButNotToUser,
            offender4NotLao,
          ),
        ),
      )

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_2_CRN, OFFENDER_3_CRN))
      } returns ClientResult.Success(
        HttpStatus.OK,
        UserAccess(
          listOf(
            CaseAccessFactory()
              .withCrn(OFFENDER_2_CRN)
              .withUserRestricted(false)
              .withUserExcluded(true)
              .produce(),
            CaseAccessFactory()
              .withCrn(OFFENDER_3_CRN)
              .withUserRestricted(false)
              .withUserExcluded(false)
              .produce(),
          ),
        ),
      )

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN, OFFENDER_2_CRN, OFFENDER_3_CRN, OFFENDER_4_CRN, OFFENDER_5_CRN),
        laoStrategy = CheckUserAccess(USERNAME),
      )

      assertThat(results).hasSize(5)

      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.Success.Full
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
      assertThat(result0.summary).isSameAs(offender1NotLao)

      assertThat(results[1]).isInstanceOf(PersonSummaryInfoResult.Success.Restricted::class.java)
      val result1 = results[1] as PersonSummaryInfoResult.Success.Restricted
      assertThat(result1.crn).isEqualTo(OFFENDER_2_CRN)
      assertThat(result1.nomsNumber).isEqualTo(OFFENDER_2_NOMS)

      assertThat(results[2]).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
      val result2 = results[2] as PersonSummaryInfoResult.Success.Full
      assertThat(result2.crn).isEqualTo(OFFENDER_3_CRN)
      assertThat(result2.summary).isSameAs(offender3ExcludedButNotToUser)

      assertThat(results[3]).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
      val result3 = results[3] as PersonSummaryInfoResult.Success.Full
      assertThat(result3.crn).isEqualTo(OFFENDER_4_CRN)
      assertThat(result3.summary).isSameAs(offender4NotLao)

      assertThat(results[4]).isInstanceOf(PersonSummaryInfoResult.NotFound::class.java)
      val result4 = results[4] as PersonSummaryInfoResult.NotFound
      assertThat(result4.crn).isEqualTo(OFFENDER_5_CRN)
    }
  }

  @Nested
  inner class GetPersonSummaryInfoResultsInBatches {

    @Test
    fun `error if batch size greater than 500`() {
      assertThatThrownBy {
        offenderService.getPersonSummaryInfoResultsInBatches(
          crns = emptySet(),
          batchSize = 501,
          laoStrategy = LaoStrategy.NeverRestricted,
        )
      }.isInstanceOf(InternalServerErrorProblem::class.java)
        .hasMessage("Internal Server Error: Cannot request more than 500 CRNs. A batch size of 501 has been requested.")
    }

    @Test
    fun `request offender info in batches of 400`() {
      val crns = (1..750).map { "CRN$it" }
      val offenderSummaries = crns.map { CaseSummaryFactory().withCrn(it).produce() }
      val caseAccesses = crns.map { CaseAccessFactory().withCrn(it).produce() }

      every {
        mockApDeliusContextApiClient.getCaseSummaries((1..400).map { "CRN$it" })
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(offenderSummaries.subList(0, 400)))

      every {
        mockApDeliusContextApiClient.getCaseSummaries((401..750).map { "CRN$it" })
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(offenderSummaries.subList(400, 750)))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, (1..400).map { "CRN$it" })
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(caseAccesses.subList(0, 400)))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, (401..750).map { "CRN$it" })
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(caseAccesses.subList(401, 750)))

      val results = offenderService.getPersonSummaryInfoResultsInBatches(
        crns = crns.toSet(),
        laoStrategy = CheckUserAccess(USERNAME),
        batchSize = 400,
      )

      assertThat(results).hasSize(750)
      (0..749).forEach {
        val result = results[it]
        assertThat(result).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
        assertThat((result as PersonSummaryInfoResult.Success.Full).summary).isEqualTo(offenderSummaries[it])
      }
    }
  }
}

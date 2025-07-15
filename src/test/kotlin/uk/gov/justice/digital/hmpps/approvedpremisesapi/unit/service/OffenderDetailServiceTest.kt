package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerAlertsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.CheckUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class OffenderDetailServiceTest {
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockPrisonerAlertsApiClient = mockk<PrisonerAlertsApiClient>()
  private val mockCaseNotesClient = mockk<CaseNotesClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockOffenderDetailsDataSource = mockk<OffenderDetailsDataSource>()
  private val mockPersonTransformer = mockk<PersonTransformer>()

  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  private val service = OffenderDetailService(
    mockPrisonsApiClient,
    mockPersonTransformer,
    OffenderService(
      mockPrisonsApiClient,
      mockPrisonerAlertsApiClient,
      mockCaseNotesClient,
      mockApDeliusContextApiClient,
      mockOffenderDetailsDataSource,
      PrisonCaseNotesConfigBindingModel().apply {
        excludedCategories = listOf()
        lookbackDays = 30
        prisonApiPageSize = 30
      },
      PrisonAdjudicationsConfigBindingModel().apply {
        prisonApiPageSize = 30
      },
    ),
  )

  @Nested
  inner class GetInmateDetailByNomsNumber {

    @Test
    fun `returns not found result when for Offender without Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

      val result = service.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns not found result when for Offender with Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

      val result = service.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns unauthorised result when Client responds with 403`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.FORBIDDEN, null)

      val result = service.getInmateDetailByNomsNumber(crn, nomsNumber)

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

      val result = service.getInmateDetailByNomsNumber(crn, nomsNumber)

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

  @Nested
  inner class GetPersonInfoResultWithStrategy {
    @Test
    fun `returns NotFound if ap-and-delius API responds with a 404`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/ABC123",
            HttpStatus.NOT_FOUND,
            null,
            true,
          ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.NOT_FOUND,
            null,
            true,
          ),
      )

      every { mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(PersonSummaryInfoResult.NotFound(crn), null) } returns PersonInfoResult.NotFound(crn)

      val result = service.getPersonInfoResult(
        crn,
        laoStrategy = CheckUserAccess(deliusUsername),
      )

      assertThat(result is PersonInfoResult.NotFound).isTrue
    }

    @Test
    fun `throws Exception when ap-and-delius API responds with a 500`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/ABC123",
            HttpStatus.INTERNAL_SERVER_ERROR,
            null,
            true,
          ),
      )

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn))
      } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.NOT_FOUND,
            null,
            false,
          ),
      )

      val exception = assertThrows<RuntimeException> {
        service.getPersonInfoResult(
          crn,
          laoStrategy = CheckUserAccess(deliusUsername),
        )
      }
      assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/ABC123: 500 INTERNAL_SERVER_ERROR")
    }

    @Test
    fun `throws Exception when LAO response is Forbidden`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.FORBIDDEN,
            null,
          ),
      )

      val exception = assertThrows<RuntimeException> {
        service.getPersonInfoResult(
          crn,
          laoStrategy = CheckUserAccess(deliusUsername),
        )
      }
      assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/ABC123/user/USER/userAccess: 403 FORBIDDEN")
    }

    @Test
    fun `throws Exception when LAO calls fail with BadRequest exception`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn(deliusUsername, crn) } returns StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
        HttpStatus.BAD_REQUEST,
        null,
      )

      assertThrows<RuntimeException> {
        service.getPersonInfoResult(
          crn,
          laoStrategy = CheckUserAccess(deliusUsername),
        )
      }
    }

    @Test
    fun `returns Restricted for LAO Offender where user does not have access and ignoreLaoRestrictions is false`() {
      val crn = "ABC123"
      val deliusUsername = "USER"
      val nomsNumber = randomStringMultiCaseWithNumbers(10)

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = OffenderDetailsSummaryFactory()
            .withCrn(crn)
            .withNomsNumber(nomsNumber)
            .withCurrentRestriction(true)
            .produce(),
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            status = HttpStatus.FORBIDDEN,
            method = HttpMethod.GET,
            path = "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            body = objectMapper.writeValueAsString(
              UserOffenderAccess(
                userRestricted = true,
                userExcluded = false,
                restrictionMessage = null,
              ),
            ),
          ),
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Restricted(crn, nomsNumber),
          null,
        )
      } returns PersonInfoResult.Success.Restricted(crn, nomsNumber)

      val result = service.getPersonInfoResult(
        crn,
        laoStrategy = CheckUserAccess(deliusUsername),
      )

      assertThat(result is PersonInfoResult.Success.Restricted).isTrue
      result as PersonInfoResult.Success.Restricted
      assertThat(result.crn).isEqualTo(crn)
    }

    @Test
    fun `returns Full for LAO Offender where user does not have access and ignoreLaoRestrictions is false`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn))
      } returns mapOf(
        crn to clientResultSuccess(false, false),
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Full(
            crn,
            offenderDetails.asCaseSummary(),
          ),
          null,
        )
      } returns PersonInfoResult.Success.Full(crn, offenderDetails, null)

      val result = service.getPersonInfoResult(
        crn,
        laoStrategy = CheckUserAccess(deliusUsername),
      )

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(null)
    }

    @Test
    fun `returns Full for LAO Offender where user does not have access and ignoreLaoRestrictions is true`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.FORBIDDEN,
            objectMapper.writeValueAsString(
              UserOffenderAccess(
                userRestricted = true,
                userExcluded = false,
                restrictionMessage = null,
              ),
            ),
            true,
          ),
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Full(
            crn,
            offenderDetails.asCaseSummary(),
          ),
          null,
        )
      } returns PersonInfoResult.Success.Full(crn, offenderDetails, null)

      val result = service.getPersonInfoResult(
        crn,
        laoStrategy = LaoStrategy.NeverRestricted,
      )

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(null)
    }

    @Test
    fun `returns Full for Non LAO Offender`() {
      val crn = "ABC123"
      val nomsNumber = "NOMSABC"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to clientResultSuccess(false, false),
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Full(
            crn,
            offenderDetails.asCaseSummary(),
          ),
          inmateDetail,
        )
      } returns PersonInfoResult.Success.Full(crn, offenderDetails, inmateDetail)

      val result = service.getPersonInfoResult(
        crn,
        laoStrategy = CheckUserAccess(deliusUsername),
      )

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(inmateDetail)
    }
  }

  @Nested
  inner class GetPersonInfoResultWithoutStrategy {
    @Test
    fun `returns NotFound if ap-and-delius API responds with a 404`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/ABC123",
            HttpStatus.NOT_FOUND,
            null,
            true,
          ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.NOT_FOUND,
            null,
            true,
          ),
      )

      every { mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(PersonSummaryInfoResult.NotFound(crn), null) } returns PersonInfoResult.NotFound(crn)

      val result = service.getPersonInfoResult(crn, deliusUsername, false)
      assertThat(result is PersonInfoResult.NotFound).isTrue
    }

    @Test
    fun `throws Exception when ap-and-delius API responds with a 500`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/ABC123",
            HttpStatus.INTERNAL_SERVER_ERROR,
            null,
            true,
          ),
      )

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn))
      } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.NOT_FOUND,
            null,
            false,
          ),
      )

      val exception = assertThrows<RuntimeException> { service.getPersonInfoResult(crn, deliusUsername, false) }
      assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/ABC123: 500 INTERNAL_SERVER_ERROR")
    }

    @Test
    fun `throws Exception when LAO response is Forbidden`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.FORBIDDEN,
            null,
          ),
      )

      val exception = assertThrows<RuntimeException> { service.getPersonInfoResult(crn, deliusUsername, false) }
      assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/ABC123/user/USER/userAccess: 403 FORBIDDEN")
    }

    @Test
    fun `throws Exception when LAO calls fail with BadRequest exception`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn(deliusUsername, crn) } returns StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
        HttpStatus.BAD_REQUEST,
        null,
      )

      assertThrows<RuntimeException> { service.getPersonInfoResult(crn, deliusUsername, false) }
    }

    @Test
    fun `returns Restricted for LAO Offender where user does not have access and ignoreLaoRestrictions is false`() {
      val crn = "ABC123"
      val deliusUsername = "USER"
      val nomsNumber = randomStringMultiCaseWithNumbers(10)

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = OffenderDetailsSummaryFactory()
            .withCrn(crn)
            .withNomsNumber(nomsNumber)
            .withCurrentRestriction(true)
            .produce(),
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            status = HttpStatus.FORBIDDEN,
            method = HttpMethod.GET,
            path = "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            body = objectMapper.writeValueAsString(
              UserOffenderAccess(
                userRestricted = true,
                userExcluded = false,
                restrictionMessage = null,
              ),
            ),
          ),
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Restricted(crn, nomsNumber),
          null,
        )
      } returns PersonInfoResult.Success.Restricted(crn, nomsNumber)

      val result = service.getPersonInfoResult(crn, deliusUsername, ignoreLaoRestrictions = false)

      assertThat(result is PersonInfoResult.Success.Restricted).isTrue
      result as PersonInfoResult.Success.Restricted
      assertThat(result.crn).isEqualTo(crn)
    }

    @Test
    fun `returns Full for LAO Offender where where user does have access and ignoreLaoRestrictions is false`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn))
      } returns mapOf(
        crn to clientResultSuccess(false, false),
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Full(
            crn,
            offenderDetails.asCaseSummary(),
          ),
          null,
        )
      } returns PersonInfoResult.Success.Full(crn, offenderDetails, null)

      val result = service.getPersonInfoResult(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(null)
    }

    @Test
    fun `returns Full for LAO Offender where user does not have access and ignoreLaoRestrictions is true`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.FORBIDDEN,
            objectMapper.writeValueAsString(
              UserOffenderAccess(
                userRestricted = true,
                userExcluded = false,
                restrictionMessage = null,
              ),
            ),
            true,
          ),
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Full(
            crn,
            offenderDetails.asCaseSummary(),
          ),
          null,
        )
      } returns PersonInfoResult.Success.Full(crn, offenderDetails, null)

      val result = service.getPersonInfoResult(crn, deliusUsername, ignoreLaoRestrictions = true)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(null)
    }

    @Test
    fun `returns Full for Non LAO Offender`() {
      val crn = "ABC123"
      val nomsNumber = "NOMSABC"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to clientResultSuccess(false, false),
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Full(
            crn,
            offenderDetails.asCaseSummary(),
          ),
          inmateDetail,
        )
      } returns PersonInfoResult.Success.Full(crn, offenderDetails, inmateDetail)

      val result = service.getPersonInfoResult(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(inmateDetail)
    }
  }

  private fun clientResultSuccess(
    userRestricted: Boolean,
    userExcluded: Boolean,
  ) = ClientResult.Success(
    HttpStatus.OK,
    UserOffenderAccess(userRestricted, userExcluded, restrictionMessage = null),
    false,
  )
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy.CheckUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderDetailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

class OffenderDetailServiceTest {
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockPersonInfoResult1 = mockk<PersonInfoResult>()
  private val mockPersonInfoResult2 = mockk<PersonInfoResult>()
  private val mockPersonInfoResult3 = mockk<PersonInfoResult>()

  private val service = OffenderDetailService(
    mockPrisonsApiClient,
    mockPersonTransformer,
    mockOffenderService,
  )

  @Nested
  inner class GetInmateDetailByNomsNumber {

    @Test
    fun `Returns not found when prisons API returns 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

      val result = service.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns unauthorised when prison API Client responds with 403`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.FORBIDDEN, null)

      val result = service.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `returns successfully when prisons API responds with 200`() {
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

  companion object {
    const val CRN1: String = "CRN1"
    const val CRN2: String = "CRN2"
    const val CRN3: String = "CRN3"
    const val NOMS1: String = "NOMS1"
    const val NOMS2: String = "NOMS2"
    const val NOMS3: String = "NOMS3"
    const val DELIUS_USERNAME: String = "user1"
  }

  @Nested
  inner class GetPersonInfoResults {

    @Test
    fun `If no CRNs, immediately return empty results`() {
      val results = service.getPersonInfoResults(
        crns = emptySet(),
        laoStrategy = LaoStrategy.NeverRestricted,
      )

      assertThat(results).isEmpty()

      verify { mockOffenderService wasNot Called }
      verify { mockPrisonsApiClient wasNot Called }
    }

    @Test
    fun `Success for single offender, ignore LAO strategy`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Success.Full(
        crn = CRN1,
        summary = CaseSummaryFactory().withNomsId(NOMS1).produce(),
      )

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = LaoStrategy.NeverRestricted,
        )
      } returns listOf(personSummaryInfoResult)

      val inmateDetails = InmateDetailFactory().produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(NOMS1) } returns ClientResult.Success(
        HttpStatus.OK,
        inmateDetails,
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = inmateDetails,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResults(
        crns = setOf(CRN1),
        laoStrategy = LaoStrategy.NeverRestricted,
      )

      assertThat(result).hasSize(1)
      assertThat(result[0]).isEqualTo(mockPersonInfoResult1)
    }

    @Test
    fun `Success for single offender, check user access strategy`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Success.Full(
        crn = CRN1,
        summary = CaseSummaryFactory().withNomsId(NOMS1).produce(),
      )

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      val inmateDetails = InmateDetailFactory().produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(NOMS1) } returns ClientResult.Success(
        HttpStatus.OK,
        inmateDetails,
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = inmateDetails,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResults(
        crns = setOf(CRN1),
        laoStrategy = CheckUserAccess(DELIUS_USERNAME),
      )

      assertThat(result).hasSize(1)
      assertThat(result[0]).isEqualTo(mockPersonInfoResult1)
    }

    @Test
    fun `Mixture of outcomes for multiple offenders`() {
      val personSummaryInfoResult1Success = PersonSummaryInfoResult.Success.Full(
        crn = CRN1,
        summary = CaseSummaryFactory().withNomsId(NOMS1).produce(),
      )

      val personSummaryInfoResult2Restricted = PersonSummaryInfoResult.Success.Restricted(
        crn = CRN2,
        nomsNumber = NOMS2,
      )

      val personSummaryInfoResult3Success = PersonSummaryInfoResult.Success.Full(
        crn = CRN3,
        summary = CaseSummaryFactory().withNomsId(NOMS3).produce(),
      )

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult1Success, personSummaryInfoResult2Restricted, personSummaryInfoResult3Success)

      val inmateDetails1 = InmateDetailFactory().produce()
      val inmateDetails3 = InmateDetailFactory().produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(NOMS1) } returns ClientResult.Success(
        HttpStatus.OK,
        inmateDetails1,
      )

      every { mockPrisonsApiClient.getInmateDetailsWithWait(NOMS3) } returns ClientResult.Success(
        HttpStatus.OK,
        inmateDetails3,
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult1Success,
          inmateStatus = inmateDetails1,
        )
      } returns mockPersonInfoResult1

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult2Restricted,
          inmateStatus = null,
        )
      } returns mockPersonInfoResult2

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult3Success,
          inmateStatus = inmateDetails3,
        )
      } returns mockPersonInfoResult3

      val result = service.getPersonInfoResults(
        crns = setOf(CRN1),
        laoStrategy = CheckUserAccess(DELIUS_USERNAME),
      )

      assertThat(result).hasSize(3)
      assertThat(result[0]).isEqualTo(mockPersonInfoResult1)
      assertThat(result[1]).isEqualTo(mockPersonInfoResult2)
      assertThat(result[2]).isEqualTo(mockPersonInfoResult3)
    }

    @Test
    fun `If offender unknown don't fetch inmate details and return unknown`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Unknown(CRN1)

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = null,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResults(
        crns = setOf(CRN1),
        laoStrategy = CheckUserAccess(DELIUS_USERNAME),
      )

      assertThat(result).hasSize(1)
      assertThat(result[0]).isEqualTo(mockPersonInfoResult1)

      verify { mockPrisonsApiClient wasNot Called }
    }

    @Test
    fun `If offender not found don't fetch inmate details`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.NotFound(CRN1)

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = null,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResults(
        crns = setOf(CRN1),
        laoStrategy = CheckUserAccess(DELIUS_USERNAME),
      )

      assertThat(result).hasSize(1)
      assertThat(result[0]).isEqualTo(mockPersonInfoResult1)

      verify { mockPrisonsApiClient wasNot Called }
    }

    @Test
    fun `If offender restricted don't fetch inmate details and return restricted`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Success.Restricted(CRN1, NOMS1)

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = null,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResults(
        crns = setOf(CRN1),
        laoStrategy = CheckUserAccess(DELIUS_USERNAME),
      )

      assertThat(result).hasSize(1)
      assertThat(result[0]).isEqualTo(mockPersonInfoResult1)

      verify { mockPrisonsApiClient wasNot Called }
    }
  }

  @Nested
  inner class GetPersonInfoResultWithStrategy {

    @Test
    fun `Success, ignore LAO strategy`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Success.Full(
        crn = CRN1,
        summary = CaseSummaryFactory().withNomsId(NOMS1).produce(),
      )

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = LaoStrategy.NeverRestricted,
        )
      } returns listOf(personSummaryInfoResult)

      val inmateDetails = InmateDetailFactory().produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(NOMS1) } returns ClientResult.Success(
        HttpStatus.OK,
        inmateDetails,
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = inmateDetails,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResult(
        crn = CRN1,
        laoStrategy = LaoStrategy.NeverRestricted,
      )

      assertThat(result).isEqualTo(mockPersonInfoResult1)
    }

    @Test
    fun `Success, check user access strategy`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Success.Full(
        crn = CRN1,
        summary = CaseSummaryFactory().withNomsId(NOMS1).produce(),
      )

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      val inmateDetails = InmateDetailFactory().produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(NOMS1) } returns ClientResult.Success(
        HttpStatus.OK,
        inmateDetails,
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = inmateDetails,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResult(
        crn = CRN1,
        laoStrategy = CheckUserAccess(DELIUS_USERNAME),
      )

      assertThat(result).isEqualTo(mockPersonInfoResult1)
    }

    @Test
    fun `If offender unknown don't fetch inmate details and return unknown`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Unknown(CRN1)

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = null,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResult(
        crn = CRN1,
        laoStrategy = CheckUserAccess(DELIUS_USERNAME),
      )

      assertThat(result).isEqualTo(mockPersonInfoResult1)

      verify { mockPrisonsApiClient wasNot Called }
    }

    @Test
    fun `If offender not found don't fetch inmate details`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.NotFound(CRN1)

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = null,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResult(
        crn = CRN1,
        laoStrategy = CheckUserAccess(DELIUS_USERNAME),
      )

      assertThat(result).isEqualTo(mockPersonInfoResult1)

      verify { mockPrisonsApiClient wasNot Called }
    }

    @Test
    fun `If offender restricted don't fetch inmate details and return restricted`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Success.Restricted(CRN1, NOMS1)

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = null,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResult(
        crn = CRN1,
        laoStrategy = CheckUserAccess(DELIUS_USERNAME),
      )

      assertThat(result).isEqualTo(mockPersonInfoResult1)

      verify { mockPrisonsApiClient wasNot Called }
    }
  }

  @Nested
  inner class GetPersonInfoResultNoStrategy {

    @Test
    fun `Success, ignore LAO strategy`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Success.Full(
        crn = CRN1,
        summary = CaseSummaryFactory().withNomsId(NOMS1).produce(),
      )

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = LaoStrategy.NeverRestricted,
        )
      } returns listOf(personSummaryInfoResult)

      val inmateDetails = InmateDetailFactory().produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(NOMS1) } returns ClientResult.Success(
        HttpStatus.OK,
        inmateDetails,
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = inmateDetails,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResult(
        crn = CRN1,
        deliusUsername = null,
        ignoreLaoRestrictions = true,
      )

      assertThat(result).isEqualTo(mockPersonInfoResult1)
    }

    @Test
    fun `Success, check user access strategy`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Success.Full(
        crn = CRN1,
        summary = CaseSummaryFactory().withNomsId(NOMS1).produce(),
      )

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      val inmateDetails = InmateDetailFactory().produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(NOMS1) } returns ClientResult.Success(
        HttpStatus.OK,
        inmateDetails,
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = inmateDetails,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResult(
        crn = CRN1,
        deliusUsername = DELIUS_USERNAME,
        ignoreLaoRestrictions = false,
      )

      assertThat(result).isEqualTo(mockPersonInfoResult1)
    }

    @Test
    fun `If offender unknown don't fetch inmate details and return unknown`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Unknown(CRN1)

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = null,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResult(
        crn = CRN1,
        deliusUsername = DELIUS_USERNAME,
        ignoreLaoRestrictions = false,
      )

      assertThat(result).isEqualTo(mockPersonInfoResult1)

      verify { mockPrisonsApiClient wasNot Called }
    }

    @Test
    fun `If offender not found don't fetch inmate details`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.NotFound(CRN1)

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = null,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResult(
        crn = CRN1,
        deliusUsername = DELIUS_USERNAME,
        ignoreLaoRestrictions = false,
      )

      assertThat(result).isEqualTo(mockPersonInfoResult1)

      verify { mockPrisonsApiClient wasNot Called }
    }

    @Test
    fun `If offender restricted don't fetch inmate details and return restricted`() {
      val personSummaryInfoResult = PersonSummaryInfoResult.Success.Restricted(CRN1, NOMS1)

      every {
        mockOffenderService.getPersonSummaryInfoResults(
          crns = setOf(CRN1),
          laoStrategy = CheckUserAccess(DELIUS_USERNAME),
        )
      } returns listOf(personSummaryInfoResult)

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          personSummaryInfoResult = personSummaryInfoResult,
          inmateStatus = null,
        )
      } returns mockPersonInfoResult1

      val result = service.getPersonInfoResult(
        crn = CRN1,
        deliusUsername = DELIUS_USERNAME,
        ignoreLaoRestrictions = false,
      )

      assertThat(result).isEqualTo(mockPersonInfoResult1)

      verify { mockPrisonsApiClient wasNot Called }
    }
  }
}

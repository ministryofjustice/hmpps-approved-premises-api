package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2v2OffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory.TierDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TierService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2LaoStrategy

class Cas2OffenderServiceTest {
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockTierService = mockk<TierService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockLaoService = mockk<LaoService>()
  private val mockCas2UserService = mockk<Cas2UserService>()

  private val cas2PersonTransformer = Cas2PersonTransformer()

  private val cas2OffenderService = Cas2OffenderService(
    mockPrisonsApiClient,
    mockApDeliusContextApiClient,
    cas2PersonTransformer,
    mockTierService,
    mockOffenderService,
    mockLaoService,
    mockCas2UserService,
  )

  private val user = Cas2UserEntityFactory().produce()
  private val laoStrategy = user.cas2LaoStrategy()

  @BeforeEach
  fun setupUser() {
    every { mockCas2UserService.getUserForRequest() } returns user
  }

  private fun givenVisibility(caseSummary: CaseSummary, isVisible: Boolean) {
    every {
      mockLaoService.isVisibleForCaseSummaries(listOf(caseSummary), laoStrategy)
    } returns mapOf(caseSummary.crn to isVisible)
  }

  @Nested
  inner class GetPersonByNomsNumber {
    val nomsNumber = "DEF123"

    @Test
    fun `returns NotFound result when Probation Offender Search returns 404`() {
      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        StatusCode(HttpMethod.POST, "/case-summaries", HttpStatus.NOT_FOUND, null)

      assertThat(cas2OffenderService.getPersonByNomisIdOrCrn(nomsNumber) is Cas2v2OffenderSearchResult.NotFound).isTrue
      verify(exactly = 0) { mockLaoService.isVisibleForCaseSummaries(any(), any()) }
    }

    @Test
    fun `returns NotFound result when Probation Offender Search does not return a matching offender`() {
      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        ClientResult.Success(HttpStatus.OK, CaseSummaries(emptyList()))

      assertThat(cas2OffenderService.getPersonByNomisIdOrCrn(nomsNumber) is Cas2v2OffenderSearchResult.NotFound).isTrue
      verify(exactly = 0) { mockLaoService.isVisibleForCaseSummaries(any(), any()) }
    }

    @Test
    fun `returns result when the matching offender is not in the same prison as the user`() {
      val crn = "ABC123"

      val offenderDetails = CaseSummaryFactory().withCrn(crn).withNomsId(nomsNumber).produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offenderDetails)))

      givenVisibility(offenderDetails, isVisible = true)

      every { mockTierService.getTier(crn) } returns null

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

      assertThat(cas2OffenderService.getPersonByNomisIdOrCrn(nomsNumber) is Cas2v2OffenderSearchResult.Success).isTrue
    }

    @Test
    fun `returns Unknown if Probation Offender Search responds with a 500`() {
      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        StatusCode(HttpMethod.POST, "/case-summaries", HttpStatus.INTERNAL_SERVER_ERROR, null)

      val result = cas2OffenderService.getPersonByNomisIdOrCrn(nomsNumber)

      assertThat(result is Cas2v2OffenderSearchResult.Unknown).isTrue
      result as Cas2v2OffenderSearchResult.Unknown
      assertThat(result.throwable).isNotNull()
      verify(exactly = 0) { mockLaoService.isVisibleForCaseSummaries(any(), any()) }
    }

    @Test
    fun `returns Full Person when Probation Offender Search and Prison API returns a matching offender and populates tier`() {
      val crn = "ABC123"
      val tier = TierDtoFactory().withTierScore("A2").produce()

      val offenderDetails = CaseSummaryFactory().withCrn(crn).withNomsId(nomsNumber).produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offenderDetails)))

      givenVisibility(offenderDetails, isVisible = true)

      every { mockTierService.getTier(crn) } returns tier

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

      val result = cas2OffenderService.getPersonByNomisIdOrCrn(nomsNumber)

      assertThat(result is Cas2v2OffenderSearchResult.Success.Full).isTrue
      result as Cas2v2OffenderSearchResult.Success.Full
      assertThat(result.person.crn).isEqualTo(crn)
      assertThat(result.person.nomsNumber).isEqualTo(offenderDetails.nomsId)
      assertThat(result.person.tier).isEqualTo(tier)
    }

    @Test
    fun `returns Full Person when Probation Offender Search and Prison API returns a matching offender`() {
      val crn = "ABC123"

      val offenderDetails = CaseSummaryFactory().withCrn(crn).withNomsId(nomsNumber).produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offenderDetails)))

      givenVisibility(offenderDetails, isVisible = true)

      every { mockTierService.getTier(crn) } returns null

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

      val result = cas2OffenderService.getPersonByNomisIdOrCrn(nomsNumber)

      assertThat(result is Cas2v2OffenderSearchResult.Success.Full).isTrue
      result as Cas2v2OffenderSearchResult.Success.Full
      assertThat(result.person.crn).isEqualTo(crn)
      assertThat(result.person.nomsNumber).isEqualTo(offenderDetails.nomsId)
    }

    @Test
    fun `returns Forbidden when LaoService does not return a visibility result for the offender`() {
      val offenderDetails = CaseSummaryFactory().withCrn("ABC123").withNomsId(nomsNumber).produce()

      every { mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber)) } returns
        ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offenderDetails)))

      every { mockLaoService.isVisibleForCaseSummaries(listOf(offenderDetails), laoStrategy) } returns emptyMap()

      assertThat(cas2OffenderService.getPersonByNomisIdOrCrn(nomsNumber) is Cas2v2OffenderSearchResult.Forbidden).isTrue
      verify(exactly = 0) { mockTierService.getTier(any()) }
    }
  }

  @Nested
  inner class GetInmateDetailByNomsNumber {
    @Test
    fun `returns not found result when for Offender without Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(
        HttpMethod.GET,
        "/api/offenders/$nomsNumber",
        HttpStatus.NOT_FOUND,
        null,
      )

      val result = cas2OffenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns not found result when for Offender with Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(
        HttpMethod.GET,
        "/api/offenders/$nomsNumber",
        HttpStatus.NOT_FOUND,
        null,
      )

      val result = cas2OffenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns unauthorised result when Client responds with 403`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(
        HttpMethod.GET,
        "/api/offenders/$nomsNumber",
        HttpStatus.FORBIDDEN,
        null,
      )

      val result = cas2OffenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

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
          bookingId = 123,
        ),
      )

      val result = cas2OffenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

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
  inner class CheckRestrictedOffender {
    val crn = "CRN123"
    val nomsNumber = "DEF123"

    @BeforeEach
    fun setup() {
      val caseSummary = CaseSummaryFactory()
        .withCrn(crn)
        .withNomsId(nomsNumber)
        .withCurrentRestriction(true)
        .withCurrentExclusion(false)
        .produce()

      val caseSummaries = CaseSummaries(listOf(caseSummary))

      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(crn))
      } returns ClientResult.Success(HttpStatus.OK, caseSummaries)

      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber))
      } returns ClientResult.Success(HttpStatus.OK, caseSummaries)

      givenVisibility(caseSummary, isVisible = false)
    }

    @Test
    fun `Check searching by crn cannot view an offender with a currentRestriction`() {
      assertThat(cas2OffenderService.getPersonByNomisIdOrCrn(crn) is Cas2v2OffenderSearchResult.Forbidden).isTrue
    }

    @Test
    fun `Check searching by nomis cannot view an offender with a currentRestriction`() {
      assertThat(cas2OffenderService.getPersonByNomisIdOrCrn(nomsNumber) is Cas2v2OffenderSearchResult.Forbidden).isTrue
    }
  }

  @Nested
  inner class CheckExcludedOffender {
    val crn = "CRN123"
    val nomsNumber = "DEF123"

    @BeforeEach
    fun setup() {
      val caseSummary = CaseSummaryFactory()
        .withCrn(crn)
        .withNomsId(nomsNumber)
        .withCurrentRestriction(false)
        .withCurrentExclusion(true)
        .produce()

      val caseSummaries = CaseSummaries(listOf(caseSummary))

      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(crn))
      } returns ClientResult.Success(HttpStatus.OK, caseSummaries)

      every {
        mockApDeliusContextApiClient.getCaseSummaries(listOf(nomsNumber))
      } returns ClientResult.Success(HttpStatus.OK, caseSummaries)

      givenVisibility(caseSummary, isVisible = true)

      every { mockTierService.getTier(crn) } returns null
    }

    @Test
    fun `Check searching by crn can view an offender with a currentExclusion`() {
      assertThat(cas2OffenderService.getPersonByNomisIdOrCrn(crn)).isInstanceOf(Cas2v2OffenderSearchResult.Success.Full::class.java)
    }

    @Test
    fun `Check searching by nomis can view an offender with a currentExclusion`() {
      assertThat(cas2OffenderService.getPersonByNomisIdOrCrn(nomsNumber)).isInstanceOf(Cas2v2OffenderSearchResult.Success.Full::class.java)
    }
  }
}

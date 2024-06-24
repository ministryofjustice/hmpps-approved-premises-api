package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.datasource

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.MarshallableHttpMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderRisksDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.MappaDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Registration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshRatings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID

class OffenderRisksDataSourceTest {
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockApOASysContextApiClient = mockk<ApOASysContextApiClient>()
  private val mockHMPPSTierApiClient = mockk<HMPPSTierApiClient>()

  private val apDeliusContextApiOffenderRisksDataSource = OffenderRisksDataSource(
    mockApDeliusContextApiClient,
    mockApOASysContextApiClient,
    mockHMPPSTierApiClient,
  )

  @Test
  fun `getPersonRisks returns NotFound envelopes for RoSH, Tier, Mappa & flags when respective Clients return 404`() {
    val crn = "a-crn"

    mock404RoSH(crn)
    mock404Tier(crn)
    mock404CaseDetail(crn)

    val result = apDeliusContextApiOffenderRisksDataSource.getPersonRisks(crn)
    assertThat(result.roshRisks.status).isEqualTo(RiskStatus.NotFound)
    assertThat(result.tier.status).isEqualTo(RiskStatus.NotFound)
    assertThat(result.mappa.status).isEqualTo(RiskStatus.NotFound)
    assertThat(result.flags.status).isEqualTo(RiskStatus.NotFound)
  }

  @Test
  fun `getPersonRisks returns Error envelopes for RoSH, Tier, Mappa & flags when respective Clients return 500`() {
    val crn = "a-crn"

    mock500RoSH(crn)
    mock500Tier(crn)
    mock500CaseDetail(crn)

    val result = apDeliusContextApiOffenderRisksDataSource.getPersonRisks(crn)
    assertThat(result.roshRisks.status).isEqualTo(RiskStatus.Error)
    assertThat(result.tier.status).isEqualTo(RiskStatus.Error)
    assertThat(result.mappa.status).isEqualTo(RiskStatus.Error)
    assertThat(result.flags.status).isEqualTo(RiskStatus.Error)
  }

  @Test
  fun `getPersonRisks returns Retrieved envelopes with expected contents for RoSH, Tier, Mappa & flags when respective Clients return 200`() {
    val crn = "a-crn"

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

    mock200Tier(
      crn,
      Tier(
        tierScore = "M2",
        calculationId = UUID.randomUUID(),
        calculationDate = LocalDateTime.parse("2022-09-06T14:59:00"),
      ),
    )

    mock200CaseDetail(
      crn,
      CaseDetailFactory()
        .withRegistrations(
          listOf(
            Registration(
              code = "MAPP",
              description = "MAPPA",
              startDate = LocalDate.parse("2022-09-06"),
            ),
            Registration(
              code = "FLAG",
              description = "RISK FLAG",
              startDate = LocalDate.parse("2022-09-06"),
            ),
          ),
        )
        .withMappaDetail(
          MappaDetail(
            level = 1,
            levelDescription = "L1",
            category = 1,
            categoryDescription = "C1",
            startDate = LocalDate.parse("2022-09-06"),
            lastUpdated = ZonedDateTime.parse("2022-09-06T00:00:00Z"),
          ),
        )
        .produce(),
    )

    val result = apDeliusContextApiOffenderRisksDataSource.getPersonRisks(crn)

    assertThat(result.roshRisks.status).isEqualTo(RiskStatus.Retrieved)
    result.roshRisks.value!!.let {
      assertThat(it.lastUpdated).isEqualTo(LocalDate.parse("2022-09-06"))
      assertThat(it.overallRisk).isEqualTo("Very High")
      assertThat(it.riskToChildren).isEqualTo("Low")
      assertThat(it.riskToPublic).isEqualTo("Medium")
      assertThat(it.riskToKnownAdult).isEqualTo("High")
      assertThat(it.riskToStaff).isEqualTo("Very High")
    }

    assertThat(result.tier.status).isEqualTo(RiskStatus.Retrieved)
    result.tier.value!!.let {
      assertThat(it.lastUpdated).isEqualTo(LocalDate.parse("2022-09-06"))
      assertThat(it.level).isEqualTo("M2")
    }

    assertThat(result.mappa.status).isEqualTo(RiskStatus.Retrieved)
    result.mappa.value!!.let {
      assertThat(it.lastUpdated).isEqualTo(LocalDate.parse("2022-09-06"))
      assertThat(it.level).isEqualTo("CAT C1/LEVEL L1")
    }

    assertThat(result.flags.status).isEqualTo(RiskStatus.Retrieved)
    assertThat(result.flags.value).contains("RISK FLAG")
  }

  @Test
  fun `getPersonRisks returns Retrieved envelopes with expected contents for RoSH, Tier, Mappa & flags when Mappa is missing required information`() {
    val crn = "a-crn"

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

    mock200Tier(
      crn,
      Tier(
        tierScore = "M2",
        calculationId = UUID.randomUUID(),
        calculationDate = LocalDateTime.parse("2022-09-06T14:59:00"),
      ),
    )

    mock200CaseDetail(
      crn,
      CaseDetailFactory()
        .withRegistrations(
          listOf(
            Registration(
              code = "MAPP",
              description = "MAPPA",
              startDate = LocalDate.parse("2022-09-06"),
            ),
            Registration(
              code = "FLAG",
              description = "RISK FLAG",
              startDate = LocalDate.parse("2022-09-06"),
            ),
          ),
        )
        .withMappaDetail(
          MappaDetail(
            level = null,
            levelDescription = null,
            category = null,
            categoryDescription = null,
            startDate = LocalDate.parse("2022-09-06"),
            lastUpdated = ZonedDateTime.parse("2022-09-06T00:00:00Z"),
          ),
        )
        .produce(),
    )

    val result = apDeliusContextApiOffenderRisksDataSource.getPersonRisks(crn)

    assertThat(result.roshRisks.status).isEqualTo(RiskStatus.Retrieved)
    result.roshRisks.value!!.let {
      assertThat(it.lastUpdated).isEqualTo(LocalDate.parse("2022-09-06"))
      assertThat(it.overallRisk).isEqualTo("Very High")
      assertThat(it.riskToChildren).isEqualTo("Low")
      assertThat(it.riskToPublic).isEqualTo("Medium")
      assertThat(it.riskToKnownAdult).isEqualTo("High")
      assertThat(it.riskToStaff).isEqualTo("Very High")
    }

    assertThat(result.tier.status).isEqualTo(RiskStatus.Retrieved)
    result.tier.value!!.let {
      assertThat(it.lastUpdated).isEqualTo(LocalDate.parse("2022-09-06"))
      assertThat(it.level).isEqualTo("M2")
    }

    assertThat(result.mappa.status).isEqualTo(RiskStatus.NotFound)
    assertThat(result.mappa.value).isNull()

    assertThat(result.flags.status).isEqualTo(RiskStatus.Retrieved)
    assertThat(result.flags.value).contains("RISK FLAG")
  }

  private fun mock404RoSH(crn: String) {
    every { mockApOASysContextApiClient.getRoshRatings(crn) } returns
      ClientResult.Failure.StatusCode(
        MarshallableHttpMethod.GET,
        "/rosh/a-crn",
        HttpStatus.NOT_FOUND,
        body = null,
      )
  }

  private fun mock404Tier(crn: String) {
    every { mockHMPPSTierApiClient.getTier(crn) } returns
      ClientResult.Failure.StatusCode(
        MarshallableHttpMethod.GET,
        "/crn/a-crn/tier",
        HttpStatus.NOT_FOUND,
        body = null,
      )
  }

  private fun mock404CaseDetail(crn: String) {
    every { mockApDeliusContextApiClient.getCaseDetail(crn) } returns
      ClientResult.Failure.StatusCode(
        MarshallableHttpMethod.GET,
        "/probation-cases/$crn/details",
        HttpStatus.NOT_FOUND,
        body = null,
      )
  }

  private fun mock500RoSH(crn: String) {
    every { mockApOASysContextApiClient.getRoshRatings(crn) } returns
      ClientResult.Failure.StatusCode(
        MarshallableHttpMethod.GET,
        "/rosh/a-crn",
        HttpStatus.INTERNAL_SERVER_ERROR,
        body = null,
      )
  }

  private fun mock500Tier(crn: String) {
    every { mockHMPPSTierApiClient.getTier(crn) } returns
      ClientResult.Failure.StatusCode(
        MarshallableHttpMethod.GET,
        "/crn/a-crn/tier",
        HttpStatus.INTERNAL_SERVER_ERROR,
        body = null,
      )
  }

  private fun mock500CaseDetail(crn: String) {
    every { mockApDeliusContextApiClient.getCaseDetail(crn) } returns
      ClientResult.Failure.StatusCode(
        MarshallableHttpMethod.GET,
        "/probation-cases/$crn/details",
        HttpStatus.INTERNAL_SERVER_ERROR,
        body = null,
      )
  }

  private fun mock200RoSH(crn: String, body: RoshRatings) {
    every { mockApOASysContextApiClient.getRoshRatings(crn) } returns
      ClientResult.Success(HttpStatus.OK, body = body)
  }

  private fun mock200Tier(crn: String, body: Tier) {
    every { mockHMPPSTierApiClient.getTier(crn) } returns
      ClientResult.Success(HttpStatus.OK, body = body)
  }

  private fun mock200CaseDetail(crn: String, body: CaseDetail) {
    every { mockApDeliusContextApiClient.getCaseDetail(crn) } returns
      ClientResult.Success(HttpStatus.OK, body = body)
  }
}

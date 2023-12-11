package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.datasource

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.CommunityApiOffenderRisksDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationClientResponseFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.RegistrationKeyValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshRatings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

class CommunityApiOffenderRisksDataSourceTest {
  private val mockCommunityApiClient = mockk<CommunityApiClient>()
  private val mockApOASysContextApiClient = mockk<ApOASysContextApiClient>()
  private val mockHMPPSTierApiClient = mockk<HMPPSTierApiClient>()

  private val communityApiOffenderRisksDataSource = CommunityApiOffenderRisksDataSource(
    mockCommunityApiClient,
    mockApOASysContextApiClient,
    mockHMPPSTierApiClient,
  )

  @Test
  fun `getPersonRisks returns NotFound envelopes for RoSH, Tier, Mappa & flags when respective Clients return 404`() {
    val crn = "a-crn"

    mock404RoSH(crn)
    mock404Tier(crn)
    mock404Registrations(crn)

    val result = communityApiOffenderRisksDataSource.getPersonRisks(crn)
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
    mock500Registrations(crn)

    val result = communityApiOffenderRisksDataSource.getPersonRisks(crn)
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

    mock200Registrations(
      crn,
      Registrations(
        registrations = listOf(
          RegistrationClientResponseFactory()
            .withType(RegistrationKeyValue(code = "MAPP", description = "MAPPA"))
            .withRegisterCategory(RegistrationKeyValue(code = "C1", description = "C1"))
            .withRegisterLevel(RegistrationKeyValue(code = "L1", description = "L1"))
            .withStartDate(LocalDate.parse("2022-09-06"))
            .produce(),
          RegistrationClientResponseFactory()
            .withType(RegistrationKeyValue(code = "FLAG", description = "RISK FLAG"))
            .produce(),
        ),
      ),
    )

    val result = communityApiOffenderRisksDataSource.getPersonRisks(crn)

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
  fun `getPersonRisks returns Retrieved envelopes with expected contents for RoSH, Tier,flags and Mappa with Error status when missing 'registration-registerCategory' element`() {
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

    mock200Registrations(
      crn,
      Registrations(
        registrations = listOf(
          RegistrationClientResponseFactory()
            .withType(RegistrationKeyValue(code = "MAPP", description = "MAPPA"))
            .withRegisterCategory(null)
            .withRegisterLevel(RegistrationKeyValue(code = "L1", description = "L1"))
            .withStartDate(LocalDate.parse("2022-09-06"))
            .produce(),
          RegistrationClientResponseFactory()
            .withType(RegistrationKeyValue(code = "FLAG", description = "RISK FLAG"))
            .produce(),
        ),
      ),
    )

    val result = communityApiOffenderRisksDataSource.getPersonRisks(crn)

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

    assertThat(result.mappa.status).isEqualTo(RiskStatus.Error)
    assertThat(result.mappa.value).isNull()

    assertThat(result.flags.status).isEqualTo(RiskStatus.Retrieved)
    assertThat(result.flags.value).contains("RISK FLAG")
  }

  @Test
  fun `getPersonRisks returns Retrieved envelopes with expected contents for RoSH, Tier,flags and Mappa with Rrror status when missing 'registration-registerLevel' element`() {
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

    mock200Registrations(
      crn,
      Registrations(
        registrations = listOf(
          RegistrationClientResponseFactory()
            .withType(RegistrationKeyValue(code = "MAPP", description = "MAPPA"))
            .withRegisterCategory(RegistrationKeyValue(code = "C1", description = "C1"))
            .withRegisterLevel(null)
            .withStartDate(LocalDate.parse("2022-09-06"))
            .produce(),
          RegistrationClientResponseFactory()
            .withType(RegistrationKeyValue(code = "FLAG", description = "RISK FLAG"))
            .produce(),
        ),
      ),
    )

    val result = communityApiOffenderRisksDataSource.getPersonRisks(crn)

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

    assertThat(result.mappa.status).isEqualTo(RiskStatus.Error)
    assertThat(result.mappa.value).isNull()

    assertThat(result.flags.status).isEqualTo(RiskStatus.Retrieved)
    assertThat(result.flags.value).contains("RISK FLAG")
  }

  @Test
  fun `getPersonRisks returns Retrieved envelopes with expected contents for RoSH, Tier,flags and Mappa with Retrieved status when missing 'registration' element`() {
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

    mock200Registrations(
      crn,
      Registrations(
        registrations = listOf(
          RegistrationClientResponseFactory()
            .withType(RegistrationKeyValue(code = "TEST", description = "TEST"))
            .withRegisterCategory(RegistrationKeyValue(code = "C1", description = "C1"))
            .withRegisterLevel(RegistrationKeyValue(code = "L1", description = "L1"))
            .withStartDate(LocalDate.parse("2022-09-06"))
            .produce(),
          RegistrationClientResponseFactory()
            .withType(RegistrationKeyValue(code = "FLAG", description = "RISK FLAG"))
            .produce(),
        ),
      ),
    )

    val result = communityApiOffenderRisksDataSource.getPersonRisks(crn)

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
    assertThat(result.mappa.value).isNull()

    assertThat(result.flags.status).isEqualTo(RiskStatus.Retrieved)
    assertThat(result.flags.value).contains("RISK FLAG")
  }

  private fun mock404RoSH(crn: String) {
    every { mockApOASysContextApiClient.getRoshRatings(crn) } returns
      ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/rosh/a-crn",
        HttpStatus.NOT_FOUND,
        body = null,
      )
  }

  private fun mock404Tier(crn: String) {
    every { mockHMPPSTierApiClient.getTier(crn) } returns
      ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/crn/a-crn/tier",
        HttpStatus.NOT_FOUND,
        body = null,
      )
  }

  private fun mock404Registrations(crn: String) {
    every { mockCommunityApiClient.getRegistrationsForOffenderCrn(crn) } returns
      ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/a-crn/registrations?activeOnly=true",
        HttpStatus.NOT_FOUND,
        body = null,
      )
  }

  private fun mock500RoSH(crn: String) {
    every { mockApOASysContextApiClient.getRoshRatings(crn) } returns
      ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/rosh/a-crn",
        HttpStatus.INTERNAL_SERVER_ERROR,
        body = null,
      )
  }

  private fun mock500Tier(crn: String) {
    every { mockHMPPSTierApiClient.getTier(crn) } returns
      ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/crn/a-crn/tier",
        HttpStatus.INTERNAL_SERVER_ERROR,
        body = null,
      )
  }

  private fun mock500Registrations(crn: String) {
    every { mockCommunityApiClient.getRegistrationsForOffenderCrn(crn) } returns
      ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/a-crn/registrations?activeOnly=true",
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

  private fun mock200Registrations(crn: String, body: Registrations) {
    every { mockCommunityApiClient.getRegistrationsForOffenderCrn(crn) } returns
      ClientResult.Success(HttpStatus.OK, body = body)
  }
}

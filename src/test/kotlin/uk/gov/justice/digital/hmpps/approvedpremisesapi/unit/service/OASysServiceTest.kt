package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApAndOASysClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ClientResultFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.HealthDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskManagementPlanFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RisksToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.client.apandoasys.OASysAssessmentSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysSuitabilityService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysSuitabilityService.OASysAssessmentDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysSuitabilityService.SuitabilityStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import java.time.OffsetDateTime

@ExtendWith(MockKExtension::class)
class OASysServiceTest {

  @MockK
  private lateinit var apAndOASysClient: ApAndOASysClient

  @MockK
  private lateinit var oasysApplicabilitySevice: OASysSuitabilityService

  @InjectMockKs
  private lateinit var service: OASysService

  companion object {
    const val CRN = "CRN1122"

    val INITIATION_DATE: OffsetDateTime =
      OffsetDateTime.parse("2007-12-03T10:15:30+01:00")

    val COMPLETION_DATE: OffsetDateTime =
      OffsetDateTime.parse("2007-12-04T10:15:30+01:00")

    val APPLICABILITY_INFO =
      OASysAssessmentDates(CRN, INITIATION_DATE, COMPLETION_DATE)

    val DEFAULT_SUITABILITY_STRATEGY =
      SuitabilityStrategy.CompletedInLastSixMonths

    @Suppress("UNCHECKED_CAST")
    private fun <T> ClientResult<*>.cast() = this as ClientResult<T>

    @JvmStatic
    fun failureCases() = listOf(
      FailureCase(
        name = "AssessmentSummary",
        mockSetup = { client, res -> every { client.getLatestAssessmentSummary(CRN) } returns res.cast() },
        invoke = { it.getAssessmentSummary(CRN) },
        stubResponse = OASysAssessmentSummaryFactory().produce(),
      ),

      FailureCase(
        name = "NeedsDetails",
        mockSetup = { client, res -> every { client.getNeedsDetails(CRN) } returns res.cast() },
        invoke = { it.getNeedsDetails(CRN) },
        stubResponse = NeedsDetailsFactory().produce(),
      ),

      FailureCase(
        name = "OffenceDetails",
        mockSetup = { client, res -> every { client.getOffenceDetails(CRN) } returns res.cast() },
        invoke = { it.getOffenceDetails(CRN) },
        stubResponse = OffenceDetailsFactory().produce(),
      ),

      FailureCase(
        name = "RiskManagementPlan",
        mockSetup = { client, res -> every { client.getRiskManagementPlan(CRN) } returns res.cast() },
        invoke = { it.getRiskManagementPlan(CRN) },
        stubResponse = RiskManagementPlanFactory().produce(),
      ),

      FailureCase(
        name = "RoshSummary",
        mockSetup = { client, res -> every { client.getRoshSummary(CRN) } returns res.cast() },
        invoke = { it.getRoshSummary(CRN) },
        stubResponse = RoshSummaryFactory().produce(),
      ),

      FailureCase(
        name = "RiskToTheIndividual",
        mockSetup = { client, res -> every { client.getRiskToTheIndividual(CRN) } returns res.cast() },
        invoke = { it.getRiskToTheIndividual(CRN) },
        stubResponse = RisksToTheIndividualFactory().produce(),
      ),

      FailureCase(
        name = "HealthDetails",
        mockSetup = { client, res -> every { client.getHealth(CRN) } returns res.cast() },
        invoke = { it.getHealthDetails(CRN) },
        stubResponse = HealthDetailsFactory().produce(),
      ),

      FailureCase(
        name = "RoshRatings",
        mockSetup = { client, res -> every { client.getRoshRatings(CRN) } returns res.cast() },
        invoke = { it.getRoshRatings(CRN) },
        stubResponse = RoshRatingsFactory().produce(),
      ),
    )
  }

  @Nested
  inner class SharedFailureScenarios {

    @ParameterizedTest(name = "{0} - 404 returns not found")
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.OASysServiceTest#failureCases")
    fun `404 returns not found`(case: FailureCase) {
      case.mockSetup(apAndOASysClient, ClientResultFactory.notFound<Any>())

      assertThatCasResult(case.invoke(service))
        .isNotFound("OASysAssessment", CRN)
    }

    @ParameterizedTest(name = "{0} - unexpected status throws")
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.OASysServiceTest#failureCases")
    fun `unexpected status throws`(case: FailureCase) {
      case.mockSetup(apAndOASysClient, ClientResultFactory.statusCode<Any>(HttpStatus.TEMPORARY_REDIRECT))

      assertThatThrownBy {
        case.invoke(service)
      }.isInstanceOf(RuntimeException::class.java)
    }

    @ParameterizedTest(name = "{0} - failure throws")
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.OASysServiceTest#failureCases")
    fun `failure throws`(case: FailureCase) {
      case.mockSetup(apAndOASysClient, ClientResultFactory.failureOther<Any>())

      assertThatThrownBy {
        case.invoke(service)
      }.isInstanceOf(RuntimeException::class.java)
    }

    @ParameterizedTest(name = "{0} - not found if assessment exists but isnt usable")
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.OASysServiceTest#failureCases")
    fun `not found if assessment exists but isnt usable`(case: FailureCase) {
      every { oasysApplicabilitySevice.isSuitable(any(), any()) } returns false
      case.mockSetup(apAndOASysClient, ClientResult.Success(HttpStatus.OK, case.stubResponse))

      val result = case.invoke(service)

      assertThatCasResult(result).isNotFound("OASysAssessment", CRN)
    }
  }

  @Nested
  inner class SuccessScenarios {

    @Test
    fun `AssessmentSummary success`() {
      val response = OASysAssessmentSummaryFactory()
        .withInitiationDate(INITIATION_DATE)
        .withCompletedDate(COMPLETION_DATE)
        .produce()

      every {
        oasysApplicabilitySevice.isSuitable(APPLICABILITY_INFO, DEFAULT_SUITABILITY_STRATEGY)
      } returns true

      every {
        apAndOASysClient.getLatestAssessmentSummary(CRN)
      } returns ClientResult.Success(HttpStatus.OK, response)

      val result = service.getAssessmentSummary(CRN)

      assertSuccess(result, response)
    }

    @Test
    fun `NeedsDetails success`() {
      val response = NeedsDetailsFactory()
        .withInitiationDate(INITIATION_DATE)
        .withDateCompleted(COMPLETION_DATE)
        .produce()

      every {
        oasysApplicabilitySevice.isSuitable(APPLICABILITY_INFO, DEFAULT_SUITABILITY_STRATEGY)
      } returns true

      every {
        apAndOASysClient.getNeedsDetails(CRN)
      } returns ClientResult.Success(HttpStatus.OK, response)

      val result = service.getNeedsDetails(CRN)

      assertSuccess(result, response)
    }

    @Test
    fun `OffenceDetails success`() {
      val response = OffenceDetailsFactory()
        .withInitiationDate(INITIATION_DATE)
        .withDateCompleted(COMPLETION_DATE)
        .produce()

      every {
        oasysApplicabilitySevice.isSuitable(APPLICABILITY_INFO, DEFAULT_SUITABILITY_STRATEGY)
      } returns true

      every {
        apAndOASysClient.getOffenceDetails(CRN)
      } returns ClientResult.Success(HttpStatus.OK, response)

      val result = service.getOffenceDetails(CRN)

      assertSuccess(result, response)
    }

    @Test
    fun `RiskManagementPlan success`() {
      val response = RiskManagementPlanFactory()
        .withInitiationDate(INITIATION_DATE)
        .withDateCompleted(COMPLETION_DATE)
        .produce()

      every {
        oasysApplicabilitySevice.isSuitable(APPLICABILITY_INFO, DEFAULT_SUITABILITY_STRATEGY)
      } returns true

      every {
        apAndOASysClient.getRiskManagementPlan(CRN)
      } returns ClientResult.Success(HttpStatus.OK, response)

      val result = service.getRiskManagementPlan(CRN)

      assertSuccess(result, response)
    }

    @Test
    fun `RoshSummary success`() {
      val response = RoshSummaryFactory()
        .withInitiationDate(INITIATION_DATE)
        .withDateCompleted(COMPLETION_DATE)
        .produce()

      every {
        oasysApplicabilitySevice.isSuitable(APPLICABILITY_INFO, DEFAULT_SUITABILITY_STRATEGY)
      } returns true

      every {
        apAndOASysClient.getRoshSummary(CRN)
      } returns ClientResult.Success(HttpStatus.OK, response)

      val result = service.getRoshSummary(CRN)

      assertSuccess(result, response)
    }

    @Test
    fun `RiskToTheIndividual success`() {
      val response = RisksToTheIndividualFactory()
        .withInitiationDate(INITIATION_DATE)
        .withDateCompleted(COMPLETION_DATE)
        .produce()

      every {
        oasysApplicabilitySevice.isSuitable(APPLICABILITY_INFO, DEFAULT_SUITABILITY_STRATEGY)
      } returns true

      every {
        apAndOASysClient.getRiskToTheIndividual(CRN)
      } returns ClientResult.Success(HttpStatus.OK, response)

      val result = service.getRiskToTheIndividual(CRN)

      assertSuccess(result, response)
    }

    @Test
    fun `HealthDetails success`() {
      val response = HealthDetailsFactory()
        .withInitiationDate(INITIATION_DATE)
        .withDateCompleted(COMPLETION_DATE)
        .produce()

      every {
        oasysApplicabilitySevice.isSuitable(APPLICABILITY_INFO, DEFAULT_SUITABILITY_STRATEGY)
      } returns true

      every {
        apAndOASysClient.getHealth(CRN)
      } returns ClientResult.Success(HttpStatus.OK, response)

      val result = service.getHealthDetails(CRN)

      assertSuccess(result, response)
    }

    @Test
    fun `RoshRatings success`() {
      val response = RoshRatingsFactory()
        .withInitiationDate(INITIATION_DATE)
        .withDateCompleted(COMPLETION_DATE)
        .produce()

      every {
        oasysApplicabilitySevice.isSuitable(APPLICABILITY_INFO, DEFAULT_SUITABILITY_STRATEGY)
      } returns true

      every {
        apAndOASysClient.getRoshRatings(CRN)
      } returns ClientResult.Success(HttpStatus.OK, response)

      val result = service.getRoshRatings(CRN)

      assertSuccess(result, response)
    }
  }

  data class FailureCase(
    val name: String,
    val mockSetup: (ApAndOASysClient, ClientResult<*>) -> Unit,
    val invoke: (OASysService) -> CasResult<*>,
    val stubResponse: Any,
  ) {
    override fun toString() = name
  }

  private fun <T : Any> assertSuccess(result: CasResult<T>, expected: T) {
    assertThatCasResult(result)
      .isSuccess()
      .withValueEqualTo(expected)
  }
}

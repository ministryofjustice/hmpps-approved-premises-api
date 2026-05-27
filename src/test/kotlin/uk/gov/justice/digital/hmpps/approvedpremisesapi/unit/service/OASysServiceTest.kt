package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApAndOASysClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ClientResultFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.HealthDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskManagementPlanFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.client.apandoasys.OASysAssessmentSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult

@ExtendWith(MockKExtension::class)
class OASysServiceTest {

  @MockK
  private lateinit var apAndOASysClient: ApAndOASysClient

  @InjectMockKs
  private lateinit var service: OASysService

  companion object {
    const val CRN = "CRN1122"
  }

  @Nested
  inner class AssessmentSummary {

    @Test
    fun success() {
      val upstreamResponse = OASysAssessmentSummaryFactory().produce()

      every { apAndOASysClient.getLatestAssessmentSummary(CRN) } returns ClientResult.Success(HttpStatus.OK, upstreamResponse)

      val result = service.getAssessmentSummary(CRN)

      assertThatCasResult(result).isSuccess().withValueEqualTo(upstreamResponse)
    }

    @Test
    fun `not found status code returns not found`() {
      every { apAndOASysClient.getLatestAssessmentSummary(CRN) } returns ClientResultFactory.notFound()

      val result = service.getAssessmentSummary(CRN)

      assertThatCasResult(result).isNotFound("OASysAssessment", CRN)
    }

    @Test
    fun `other status code throws exception`() {
      every { apAndOASysClient.getLatestAssessmentSummary(CRN) } returns ClientResultFactory.statusCode(HttpStatus.TEMPORARY_REDIRECT)

      assertThatThrownBy {
        service.getAssessmentSummary(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `general error throws Exception`() {
      every { apAndOASysClient.getLatestAssessmentSummary(CRN) } returns ClientResultFactory.failureOther()

      assertThatThrownBy {
        service.getAssessmentSummary(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }
  }

  @Nested
  inner class NeedsDetails {

    @Test
    fun success() {
      val upstreamResponse = NeedsDetailsFactory().produce()

      every { apAndOASysClient.getNeedsDetails(CRN) } returns ClientResult.Success(HttpStatus.OK, upstreamResponse)

      val result = service.getNeedsDetails(CRN)

      assertThatCasResult(result).isSuccess().withValueEqualTo(upstreamResponse)
    }

    @Test
    fun `not found status code returns not found`() {
      every { apAndOASysClient.getNeedsDetails(CRN) } returns ClientResultFactory.notFound()

      val result = service.getNeedsDetails(CRN)

      assertThatCasResult(result).isNotFound("Person", CRN)
    }

    @Test
    fun `forbidden status code returns forbidden`() {
      every { apAndOASysClient.getNeedsDetails(CRN) } returns ClientResultFactory.forbidden()

      val result = service.getNeedsDetails(CRN)

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `other status code throws exception`() {
      every { apAndOASysClient.getNeedsDetails(CRN) } returns ClientResultFactory.statusCode(HttpStatus.TEMPORARY_REDIRECT)

      assertThatThrownBy {
        service.getNeedsDetails(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `general error throws not found problem`() {
      every { apAndOASysClient.getNeedsDetails(CRN) } returns ClientResultFactory.failureOther()

      assertThatThrownBy {
        service.getNeedsDetails(CRN)
      }.isInstanceOf(NotFoundProblem::class.java)
    }
  }

  @Nested
  inner class OffenceDetails {

    @Test
    fun success() {
      val upstreamResponse = OffenceDetailsFactory().produce()

      every { apAndOASysClient.getOffenceDetails(CRN) } returns ClientResult.Success(HttpStatus.OK, upstreamResponse)

      val result = service.getOffenceDetails(CRN)

      assertThatCasResult(result).isSuccess().withValueEqualTo(upstreamResponse)
    }

    @Test
    fun `not found status code returns not found`() {
      every { apAndOASysClient.getOffenceDetails(CRN) } returns ClientResultFactory.notFound()

      val result = service.getOffenceDetails(CRN)

      assertThatCasResult(result).isNotFound("Person", CRN)
    }

    @Test
    fun `forbidden status code returns forbidden`() {
      every { apAndOASysClient.getOffenceDetails(CRN) } returns ClientResultFactory.forbidden()

      val result = service.getOffenceDetails(CRN)

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `other status code throws exception`() {
      every { apAndOASysClient.getOffenceDetails(CRN) } returns ClientResultFactory.statusCode(HttpStatus.TEMPORARY_REDIRECT)

      assertThatThrownBy {
        service.getOffenceDetails(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `general error throws exception`() {
      every { apAndOASysClient.getOffenceDetails(CRN) } returns ClientResultFactory.failureOther()

      assertThatThrownBy {
        service.getOffenceDetails(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }
  }

  @Nested
  inner class RiskManagementPlan {

    @Test
    fun success() {
      val upstreamResponse = RiskManagementPlanFactory().produce()

      every { apAndOASysClient.getRiskManagementPlan(CRN) } returns ClientResult.Success(HttpStatus.OK, upstreamResponse)

      val result = service.getRiskManagementPlan(CRN)

      assertThatCasResult(result).isSuccess().withValueEqualTo(upstreamResponse)
    }

    @Test
    fun `not found status code returns not found`() {
      every { apAndOASysClient.getRiskManagementPlan(CRN) } returns ClientResultFactory.notFound()

      val result = service.getRiskManagementPlan(CRN)

      assertThatCasResult(result).isNotFound("Person", CRN)
    }

    @Test
    fun `forbidden status code returns forbidden`() {
      every { apAndOASysClient.getRiskManagementPlan(CRN) } returns ClientResultFactory.forbidden()

      val result = service.getRiskManagementPlan(CRN)

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `other status code throws exception`() {
      every { apAndOASysClient.getRiskManagementPlan(CRN) } returns ClientResultFactory.statusCode(HttpStatus.TEMPORARY_REDIRECT)

      assertThatThrownBy {
        service.getRiskManagementPlan(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `general error throws exception`() {
      every { apAndOASysClient.getRiskManagementPlan(CRN) } returns ClientResultFactory.failureOther()

      assertThatThrownBy {
        service.getRiskManagementPlan(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }
  }

  @Nested
  inner class RoshSummary {

    @Test
    fun success() {
      val upstreamResponse = RoshSummaryFactory().produce()

      every { apAndOASysClient.getRoshSummary(CRN) } returns ClientResult.Success(HttpStatus.OK, upstreamResponse)

      val result = service.getRoshSummary(CRN)

      assertThatCasResult(result).isSuccess().withValueEqualTo(upstreamResponse)
    }

    @Test
    fun `not found status code returns not found`() {
      every { apAndOASysClient.getRoshSummary(CRN) } returns ClientResultFactory.notFound()

      val result = service.getRoshSummary(CRN)

      assertThatCasResult(result).isNotFound("Person", CRN)
    }

    @Test
    fun `forbidden status code returns forbidden`() {
      every { apAndOASysClient.getRoshSummary(CRN) } returns ClientResultFactory.forbidden()

      val result = service.getRoshSummary(CRN)

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `other status code throws exception`() {
      every { apAndOASysClient.getRoshSummary(CRN) } returns ClientResultFactory.statusCode(HttpStatus.TEMPORARY_REDIRECT)

      assertThatThrownBy {
        service.getRoshSummary(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `general error throws exception`() {
      every { apAndOASysClient.getRoshSummary(CRN) } returns ClientResultFactory.failureOther()

      assertThatThrownBy {
        service.getRoshSummary(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }
  }

  @Nested
  inner class RiskToTheIndividual {

    @Test
    fun success() {
      val upstreamResponse = RiskToTheIndividualFactory().produce()

      every { apAndOASysClient.getRiskToTheIndividual(CRN) } returns ClientResult.Success(HttpStatus.OK, upstreamResponse)

      val result = service.getRiskToTheIndividual(CRN)

      assertThatCasResult(result).isSuccess().withValueEqualTo(upstreamResponse)
    }

    @Test
    fun `not found status code returns not found`() {
      every { apAndOASysClient.getRiskToTheIndividual(CRN) } returns ClientResultFactory.notFound()

      val result = service.getRiskToTheIndividual(CRN)

      assertThatCasResult(result).isNotFound("Person", CRN)
    }

    @Test
    fun `forbidden status code returns forbidden`() {
      every { apAndOASysClient.getRiskToTheIndividual(CRN) } returns ClientResultFactory.forbidden()

      val result = service.getRiskToTheIndividual(CRN)

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `other status code throws exception`() {
      every { apAndOASysClient.getRiskToTheIndividual(CRN) } returns ClientResultFactory.statusCode(HttpStatus.TEMPORARY_REDIRECT)

      assertThatThrownBy {
        service.getRiskToTheIndividual(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `general error throws exception`() {
      every { apAndOASysClient.getRiskToTheIndividual(CRN) } returns ClientResultFactory.failureOther()

      assertThatThrownBy {
        service.getRiskToTheIndividual(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }
  }

  @Nested
  inner class GetHealthDetails {

    @Test
    fun success() {
      val upstreamResponse = HealthDetailsFactory().produce()

      every { apAndOASysClient.getHealth(CRN) } returns ClientResult.Success(HttpStatus.OK, upstreamResponse)

      val result = service.getHealthDetails(CRN)

      assertThatCasResult(result).isSuccess().withValueEqualTo(upstreamResponse)
    }

    @Test
    fun `not found status code returns not found`() {
      every { apAndOASysClient.getHealth(CRN) } returns ClientResultFactory.notFound()

      val result = service.getHealthDetails(CRN)

      assertThatCasResult(result).isNotFound("Person", CRN)
    }

    @Test
    fun `forbidden status code returns forbidden`() {
      every { apAndOASysClient.getHealth(CRN) } returns ClientResultFactory.forbidden()

      val result = service.getHealthDetails(CRN)

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `other status code throws exception`() {
      every { apAndOASysClient.getHealth(CRN) } returns ClientResultFactory.statusCode(HttpStatus.TEMPORARY_REDIRECT)

      assertThatThrownBy {
        service.getHealthDetails(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `general error throws exception`() {
      every { apAndOASysClient.getHealth(CRN) } returns ClientResultFactory.failureOther()

      assertThatThrownBy {
        service.getHealthDetails(CRN)
      }.isInstanceOf(RuntimeException::class.java)
    }
  }
}

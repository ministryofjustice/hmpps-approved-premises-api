package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroupName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.HealthDetailsInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.HealthIssue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.HealthDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskManagementPlanFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RisksToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockHealthDetails404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockNeedsDetails404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockOffenceDetails404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockRiskManagementPlan404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockRiskToTheIndividual404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockRoshSummary404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulHealthDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulOffenceDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulRiskManagementPlanCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulRiskToTheIndividualCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulRoSHSummaryCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysAssessmentInfoTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysNeedsQuestionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysOffenceDetailsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.time.OffsetDateTime

class Cas1OAsysTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var oaSysNeedsQuestionTransformer: Cas1OASysNeedsQuestionTransformer

  @Autowired
  lateinit var oaSysOffenceDetailsTransformer: Cas1OASysOffenceDetailsTransformer

  @Autowired
  lateinit var cas1OASysAssessmentInfoTransformer: Cas1OASysAssessmentInfoTransformer

  companion object {
    const val CRN = "CRN123"
  }

  @Nested
  inner class Metadata {

    @Test
    fun `No JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/people/CRN/oasys/metadata")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `success when assessment not found`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      apAndOASysMockOffenceDetails404Call(CRN)
      apAndOASysMockNeedsDetails404Call(CRN)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/metadata")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysMetadata>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.supportingInformation).isEmpty()
    }

    @Test
    fun `Return forbidden if user can't access the CRN`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(
        CaseAccessFactory()
          .withCrn(CRN)
          .withUserExcluded(true)
          .produce(),
      )

      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/metadata")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `default 6 month suitability strategy, assessment is less than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val needsDetails = NeedsDetailsFactory().produce()
      apAndOASysMockSuccessfulNeedsDetailsCall(CRN, needsDetails)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/metadata")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysMetadata>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.supportingInformation).isEqualTo(oaSysNeedsQuestionTransformer.transformToSupportingInformationMetadata(needsDetails))
    }

    @Test
    fun `default 6 month suitability strategy, assessment is more than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val needsDetails = NeedsDetailsFactory()
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulNeedsDetailsCall(CRN, needsDetails)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/metadata")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysMetadata>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.supportingInformation).isEmpty()
    }

    @Test
    fun `allow all suitability strategy, assessment is more than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val needsDetails = NeedsDetailsFactory()
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulNeedsDetailsCall(CRN, needsDetails)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/metadata?suitabilityStrategy=allow_all")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysMetadata>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.supportingInformation).isEqualTo(oaSysNeedsQuestionTransformer.transformToSupportingInformationMetadata(needsDetails))
    }
  }

  @Nested
  inner class Answers {

    @Test
    fun `No JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskManagementPlan")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Return forbidden if user can't access the CRN`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(
        CaseAccessFactory()
          .withCrn(CRN)
          .withUserExcluded(true)
          .produce(),
      )

      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskManagementPlan")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Risk Management - Not Found, return empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())
      apAndOASysMockRiskManagementPlan404Call(CRN)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskManagementPlan")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.RISK_MANAGEMENT_PLAN)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = null,
        ),
      )
    }

    @Test
    fun `Risk Management - Default 6 month suitability strategy, assessment is less than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskManagementPlan = RiskManagementPlanFactory()
        .withSupervision("The supervision answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(5))
        .produce()
      apAndOASysMockSuccessfulRiskManagementPlanCall(CRN, riskManagementPlan)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskManagementPlan")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.RISK_MANAGEMENT_PLAN)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = "The supervision answer",
        ),
      )
    }

    @Test
    fun `Risk Management - Default 6 month suitability strategy, assessment is more than 6 months old, return empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskManagementPlan = RiskManagementPlanFactory()
        .withSupervision("The supervision answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulRiskManagementPlanCall(CRN, riskManagementPlan)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskManagementPlan")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.RISK_MANAGEMENT_PLAN)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = null,
        ),
      )
    }

    @Test
    fun `Risk Management - Allow all suitability strategy, assessment is more than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskManagementPlan = RiskManagementPlanFactory()
        .withSupervision("The supervision answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulRiskManagementPlanCall(CRN, riskManagementPlan)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskManagementPlan&suitabilityStrategy=allow_all")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.RISK_MANAGEMENT_PLAN)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = "The supervision answer",
        ),
      )
    }

    @Test
    fun `Offence Details - Not Found, return empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())
      apAndOASysMockOffenceDetails404Call(CRN)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=offenceDetails")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.OFFENCE_DETAILS)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Impact on the victim",
          questionNumber = "2.5",
          answer = null,
        ),
      )
    }

    @Test
    fun `Offence Details - Default 6 month suitability strategy, assessment is less than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val offenceDetails = OffenceDetailsFactory()
        .withVictimImpact("Victim impact answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(5))
        .produce()
      apAndOASysMockSuccessfulOffenceDetailsCall(CRN, offenceDetails)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=offenceDetails")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.OFFENCE_DETAILS)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Impact on the victim",
          questionNumber = "2.5",
          answer = "Victim impact answer",
        ),
      )
    }

    @Test
    fun `Offence Details - Default 6 month suitability strategy, assessment is more than 6 months old, return empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val offenceDetails = OffenceDetailsFactory()
        .withVictimImpact("Victim impact answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulOffenceDetailsCall(CRN, offenceDetails)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=offenceDetails")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.OFFENCE_DETAILS)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Impact on the victim",
          questionNumber = "2.5",
          answer = null,
        ),
      )
    }

    @Test
    fun `Offence Details - Allow all suitability strategy, assessment is more than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val offenceDetails = OffenceDetailsFactory()
        .withVictimImpact("Victim impact answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulOffenceDetailsCall(CRN, offenceDetails)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=offenceDetails&suitabilityStrategy=allow_all")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.OFFENCE_DETAILS)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Impact on the victim",
          questionNumber = "2.5",
          answer = "Victim impact answer",
        ),
      )
    }

    @Test
    fun `ROSH Summary - Not Found, return empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      apAndOASysMockRoshSummary404Call(CRN)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=roshSummary")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.ROSH_SUMMARY)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answer = null,
        ),
      )
    }

    @Test
    fun `ROSH Summary - Default 6 month suitability strategy, assessment is less than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val roshSummary = RoshSummaryFactory()
        .withWhoAtRisk("Who at risk answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(5))
        .produce()
      apAndOASysMockSuccessfulRoSHSummaryCall(CRN, roshSummary)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=roshSummary")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.ROSH_SUMMARY)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answer = "Who at risk answer",
        ),
      )
    }

    @Test
    fun `ROSH Summary - Default 6 month suitability strategy, assessment is more than 6 months old, return empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val roshSummary = RoshSummaryFactory()
        .withWhoAtRisk("Who at risk answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulRoSHSummaryCall(CRN, roshSummary)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=roshSummary")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.ROSH_SUMMARY)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answer = null,
        ),
      )
    }

    @Test
    fun `ROSH Summary - Allow all suitability strategy, assessment is more than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val roshSummary = RoshSummaryFactory()
        .withWhoAtRisk("Who at risk answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulRoSHSummaryCall(CRN, roshSummary)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=roshSummary&suitabilityStrategy=allow_all")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.ROSH_SUMMARY)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Who is at risk",
          questionNumber = "R10.1",
          answer = "Who at risk answer",
        ),
      )
    }

    @Test
    fun `Risk to self - Not Found, return empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())
      apAndOASysMockRiskToTheIndividual404Call(CRN)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskToSelf")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.RISK_TO_SELF)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Analysis of vulnerabilities",
          questionNumber = "FA64",
          answer = null,
        ),
      )
    }

    @Test
    fun `Risk to self - Default 6 month suitability strategy, assessment is less than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskToIndividual = RisksToTheIndividualFactory()
        .withCurrentVulnerability("Current vuln answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(5))
        .produce()
      apAndOASysMockSuccessfulRiskToTheIndividualCall(CRN, riskToIndividual)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskToSelf")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.RISK_TO_SELF)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Current concerns about Vulnerability",
          questionNumber = "R8.3.1",
          answer = "Current vuln answer",
        ),
      )
    }

    @Test
    fun `Risk to self - Default 6 month suitability strategy, assessment is more than 6 months old, return empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskToIndividual = RisksToTheIndividualFactory()
        .withCurrentVulnerability("Current vuln answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulRiskToTheIndividualCall(CRN, riskToIndividual)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskToSelf")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.RISK_TO_SELF)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Analysis of vulnerabilities",
          questionNumber = "FA64",
          answer = null,
        ),
      )
    }

    @Test
    fun `Risk to self - Allow all suitability strategy, assessment is more than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskToIndividual = RisksToTheIndividualFactory()
        .withCurrentVulnerability("Current vuln answer")
        .withDateCompleted(OffsetDateTime.now().minusMonths(7))
        .produce()
      apAndOASysMockSuccessfulRiskToTheIndividualCall(CRN, riskToIndividual)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskToSelf&suitabilityStrategy=allow_all")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.RISK_TO_SELF)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Current concerns about Vulnerability",
          questionNumber = "R8.3.1",
          answer = "Current vuln answer",
        ),
      )
    }

    @Test
    fun `Supporting Information - Not Found, return empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())
      apAndOASysMockNeedsDetails404Call(CRN)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=supportingInformation&includeOptionalSections=")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.SUPPORTING_INFORMATION)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Relationship issues contributing to risks of offending and harm",
          questionNumber = "6.9",
          answer = null,
        ),
        OASysQuestion(
          label = "Issues of emotional well-being contributing to risks of offending and harm",
          questionNumber = "10.9",
          answer = null,
        ),
        OASysQuestion(
          label = "Lifestyle issues contributing to risks of offending and harm",
          questionNumber = "7.9",
          answer = null,
        ),
      )
    }

    @Test
    fun `Supporting Information - Default 6 month suitability strategy, assessment is less than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val needsDetails = NeedsDetailsFactory().apply {
        withDateCompleted(OffsetDateTime.now().minusMonths(5))
        withRelationshipIssuesDetails(linkedToHarm = true, relationshipIssuesDetails = "relationship answer")
        withLifestyleIssuesDetails(linkedToHarm = false, lifestyleIssuesDetails = "lifestyle answer")
        withEmotionalIssuesDetails(linkedToHarm = null, emotionalIssuesDetails = "emotional answer")
      }.produce()

      val healthDetails = HealthDetailsFactory().apply {
        withDateCompleted(OffsetDateTime.now().minusMonths(5))
        withGeneralHealth(generalHealth = false, generalHealthSpecify = "health answer")
      }.produce()

      apAndOASysMockSuccessfulNeedsDetailsCall(CRN, needsDetails)
      apAndOASysMockSuccessfulHealthDetailsCall(CRN, healthDetails)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=supportingInformation&includeOptionalSections=10")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.SUPPORTING_INFORMATION)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Relationship issues contributing to risks of offending and harm",
          questionNumber = "6.9",
          answer = "relationship answer",
        ),
        OASysQuestion(
          label = "Issues of emotional well-being contributing to risks of offending and harm",
          questionNumber = "10.9",
          answer = "emotional answer",
        ),
      )
      assertThat(result.answers).doesNotContain(
        OASysQuestion(
          label = "Lifestyle issues contributing to risks of offending and harm",
          questionNumber = "7.9",
          answer = "relationship answer",
        ),
      )
    }

    @Test
    fun `Supporting Information - Default 6 month suitability strategy, assessment is more than 6 months old, return empty answers`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val needsDetails = NeedsDetailsFactory().apply {
        withDateCompleted(OffsetDateTime.now().minusMonths(7))
        withRelationshipIssuesDetails(linkedToHarm = true, relationshipIssuesDetails = "relationship answer")
        withLifestyleIssuesDetails(linkedToHarm = false, lifestyleIssuesDetails = "lifestyle answer")
        withEmotionalIssuesDetails(linkedToHarm = null, emotionalIssuesDetails = "emotional answer")
      }.produce()

      val healthDetails = HealthDetailsFactory().apply {
        withDateCompleted(OffsetDateTime.now().minusMonths(7))
        withGeneralHealth(generalHealth = false, generalHealthSpecify = "health answer")
      }.produce()

      apAndOASysMockSuccessfulNeedsDetailsCall(CRN, needsDetails)
      apAndOASysMockSuccessfulHealthDetailsCall(CRN, healthDetails)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=supportingInformation&includeOptionalSections=10")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isFalse()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.SUPPORTING_INFORMATION)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Relationship issues contributing to risks of offending and harm",
          questionNumber = "6.9",
          answer = null,
        ),
        OASysQuestion(
          label = "Issues of emotional well-being contributing to risks of offending and harm",
          questionNumber = "10.9",
          answer = null,
        ),
        OASysQuestion(
          label = "Lifestyle issues contributing to risks of offending and harm",
          questionNumber = "7.9",
          answer = null,
        ),
      )
    }

    @Test
    fun `Supporting Information - Allow all suitability strategy, assessment is more than 6 months old`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val needsDetails = NeedsDetailsFactory().apply {
        withDateCompleted(OffsetDateTime.now().minusMonths(7))
        withRelationshipIssuesDetails(linkedToHarm = true, relationshipIssuesDetails = "relationship answer")
        withLifestyleIssuesDetails(linkedToHarm = false, lifestyleIssuesDetails = "lifestyle answer")
        withEmotionalIssuesDetails(linkedToHarm = null, emotionalIssuesDetails = "emotional answer")
      }.produce()

      val healthDetails = HealthDetailsFactory().apply {
        withDateCompleted(OffsetDateTime.now().minusMonths(7))
        withGeneralHealth(generalHealth = false, generalHealthSpecify = "health answer")
      }.produce()

      apAndOASysMockSuccessfulNeedsDetailsCall(CRN, needsDetails)
      apAndOASysMockSuccessfulHealthDetailsCall(CRN, healthDetails)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=supportingInformation&includeOptionalSections=10&suitabilityStrategy=allow_all")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.group).isEqualTo(Cas1OASysGroupName.SUPPORTING_INFORMATION)
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Relationship issues contributing to risks of offending and harm",
          questionNumber = "6.9",
          answer = "relationship answer",
        ),
        OASysQuestion(
          label = "Issues of emotional well-being contributing to risks of offending and harm",
          questionNumber = "10.9",
          answer = "emotional answer",
        ),
      )
      assertThat(result.answers).doesNotContain(
        OASysQuestion(
          label = "Lifestyle issues contributing to risks of offending and harm",
          questionNumber = "7.9",
          answer = "relationship answer",
        ),
      )
    }
  }

  @Nested
  inner class GetRisksToIndividual {
    @Test
    fun `Request without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/risks-to-individual")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Returns OK with correct body`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER))
      val risksToTheIndividual = RisksToTheIndividualFactory().produce()

      apAndOASysMockSuccessfulRiskToTheIndividualCall(CRN, risksToTheIndividual)

      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/risks-to-individual")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          jsonMapper.writeValueAsString(risksToTheIndividual.riskToTheIndividual),
        )
    }

    @Test
    fun `Returns OK with correct body if assessment is older than 6 months`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER))
      val risksToTheIndividual = RisksToTheIndividualFactory()
        .withDateCompleted(OffsetDateTime.now().minusMonths(10))
        .produce()

      apAndOASysMockSuccessfulRiskToTheIndividualCall(CRN, risksToTheIndividual)

      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/risks-to-individual")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          jsonMapper.writeValueAsString(risksToTheIndividual.riskToTheIndividual),
        )
    }

    @Test
    fun `Returns 404 when OASys returns 404`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER))
      apAndOASysMockRiskToTheIndividual404Call(CRN)

      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/risks-to-individual")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Nested
  inner class GetHealthDetails {
    @Test
    fun `Request without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/health-details")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Returns OK with correct body`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER))
      val drugsMisuse = HealthIssue(
        community = "drugs community",
        electronicMonitoring = "drugs em",
        programme = "drugs programme",
      )
      val alcoholMisuse = HealthIssue(
        community = "alcohol community",
        electronicMonitoring = "alcohol em",
        programme = "alcohol programme",
      )

      val healthDetails = HealthDetailsFactory()
        .withGeneralHealth(true, "some health issues")
        .withDrugsMisuse(drugsMisuse)
        .withAlcoholMisuse(alcoholMisuse)
        .produce()

      apAndOASysMockSuccessfulHealthDetailsCall(CRN, healthDetails)

      val response = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/health-details")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<HealthDetailsInner>()

      assertThat(response.generalHealth).isEqualTo(healthDetails.health.generalHealth)
      assertThat(response.generalHealthSpecify).isEqualTo(healthDetails.health.generalHealthSpecify)
      assertThat(response.drugsMisuse!!.community).isEqualTo(drugsMisuse.community)
      assertThat(response.drugsMisuse.electronicMonitoring).isEqualTo(drugsMisuse.electronicMonitoring)
      assertThat(response.drugsMisuse.programme).isEqualTo(drugsMisuse.programme)
      assertThat(response.alcoholMisuse!!.community).isEqualTo(alcoholMisuse.community)
      assertThat(response.alcoholMisuse.electronicMonitoring).isEqualTo(alcoholMisuse.electronicMonitoring)
      assertThat(response.alcoholMisuse.programme).isEqualTo(alcoholMisuse.programme)
    }

    @Test
    fun `Returns OK with correct body if assessment is older than 6 months`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER))

      val healthDetails = HealthDetailsFactory()
        .withDateCompleted(OffsetDateTime.now().minusMonths(10))
        .produce()

      apAndOASysMockSuccessfulHealthDetailsCall(CRN, healthDetails)

      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/health-details")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
    }

    @Test
    fun `Returns 404 when OASys returns 404`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER))
      apAndOASysMockHealthDetails404Call(CRN)

      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/health-details")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }
}

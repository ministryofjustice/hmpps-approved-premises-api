package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysGroupName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OASysMetadata
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.HealthDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenceDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskManagementPlanFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskToTheIndividualFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextUserAccessEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockNeedsDetails404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockOffenceDetails404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockRiskManagementPlan404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockRiskToTheIndividual404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulHealthDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulOffenceDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulRiskManagementPlanCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulRiskToTheIndividualCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulRoSHSummaryCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysNeedsQuestionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysOffenceDetailsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject

class Cas1OAsysTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var oaSysNeedsQuestionTransformer: Cas1OASysNeedsQuestionTransformer

  @Autowired
  lateinit var oaSysOffenceDetailsTransformer: Cas1OASysOffenceDetailsTransformer

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
    fun `Return 404 if access record can't be found for the CRN  (legacy behaviour)`() {
      val (_, jwt) = givenAUser()

      apDeliusContextUserAccessEmptyResponse()

      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/metadata")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `success when assessment not found`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      apOASysContextMockOffenceDetails404Call(CRN)
      apOASysContextMockNeedsDetails404Call(CRN)

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
    fun success() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val offenceDetails = OffenceDetailsFactory().produce()
      apOASysContextMockSuccessfulOffenceDetailsCall(CRN, offenceDetails)

      val needsDetails = NeedsDetailsFactory().apply {
        withAssessmentId(34853487)
        withAccommodationIssuesDetails("Accommodation", true, false)
        withAttitudeIssuesDetails("Attitude", false, true)
        withFinanceIssuesDetails(null, null, null)
      }.produce()
      apOASysContextMockSuccessfulNeedsDetailsCall(CRN, needsDetails)

      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/metadata")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            Cas1OASysMetadata(
              oaSysOffenceDetailsTransformer.toAssessmentMetadata(offenceDetails),
              oaSysNeedsQuestionTransformer.transformToSupportingInformationMetadata(needsDetails),
            ),
          ),
        )
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
    fun `Return 404 if access record can't be found for the CRN`() {
      val (_, jwt) = givenAUser()

      apDeliusContextUserAccessEmptyResponse()

      webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskManagementPlan")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
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
    fun `Risk Management Not Found, return empty questions`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())
      apOASysContextMockOffenceDetails404Call(CRN)
      apOASysContextMockRiskManagementPlan404Call(CRN)

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
    fun `Risk Management Plan Success`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      apOASysContextMockSuccessfulOffenceDetailsCall(CRN, OffenceDetailsFactory().produce())

      val riskManagementPlan = RiskManagementPlanFactory()
        .withSupervision("The supervision answer")
        .produce()
      apOASysContextMockSuccessfulRiskManagementPlanCall(CRN, riskManagementPlan)

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
    fun `Offence Details Not Found, return empty questions`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())
      apOASysContextMockOffenceDetails404Call(CRN)

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
    fun `Offence Details Success`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val offenceDetails = OffenceDetailsFactory()
        .withVictimImpact("Victim impact answer")
        .produce()
      apOASysContextMockSuccessfulOffenceDetailsCall(CRN, offenceDetails)

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
    fun `ROSH Summary Success`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      apOASysContextMockSuccessfulOffenceDetailsCall(CRN, OffenceDetailsFactory().produce())

      val roshSummary = RoshSummaryFactory()
        .withWhoAtRisk("Who at risk answer")
        .produce()
      apOASysContextMockSuccessfulRoSHSummaryCall(CRN, roshSummary)

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
    fun `Risk to self Success Not Found, return empty questions`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())
      apOASysContextMockSuccessfulOffenceDetailsCall(CRN, OffenceDetailsFactory().produce())
      apOASysContextMockRiskToTheIndividual404Call(CRN)

      val result = webTestClient.get()
        .uri("/cas1/people/$CRN/oasys/answers?group=riskToSelf")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1OASysGroup>()

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
    fun `Risk to self Success`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      apOASysContextMockSuccessfulOffenceDetailsCall(CRN, OffenceDetailsFactory().produce())

      val riskToIndividual = RiskToTheIndividualFactory()
        .withCurrentVulnerability("Current vuln answer")
        .produce()
      apOASysContextMockSuccessfulRiskToTheIndividualCall(CRN, riskToIndividual)

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
    fun `Supporting Information Not Found, return empty questions`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())
      apOASysContextMockOffenceDetails404Call(CRN)
      apOASysContextMockNeedsDetails404Call(CRN)

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
    fun `Supporting Information Success`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      apOASysContextMockSuccessfulOffenceDetailsCall(CRN, OffenceDetailsFactory().produce())

      val needsDetails = NeedsDetailsFactory().apply {
        withRelationshipIssuesDetails(linkedToHarm = true, relationshipIssuesDetails = "relationship answer")
        withLifestyleIssuesDetails(linkedToHarm = false, lifestyleIssuesDetails = "lifestyle answer")
        withEmotionalIssuesDetails(linkedToHarm = null, emotionalIssuesDetails = "emotional answer")
      }.produce()

      val healthDetails = HealthDetailsFactory().apply {
        withGeneralHealth(generalHealth = false, generalHealthSpecify = "health answer")
      }.produce()

      apOASysContextMockSuccessfulNeedsDetailsCall(CRN, needsDetails)
      apOASysContextMockSuccessfulHealthDetailsCall(CRN, healthDetails)

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
  }
}

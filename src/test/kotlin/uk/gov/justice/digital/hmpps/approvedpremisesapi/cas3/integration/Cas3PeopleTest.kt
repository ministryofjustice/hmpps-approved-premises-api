package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3OASysGroup
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MappaDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RiskManagementPlanFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1OAsysTest.Companion.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockNeedsDetails404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulRiskManagementPlanCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulRoshRatingsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMockSuccessfulTierCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.time.LocalDateTime
import java.util.UUID

class Cas3PeopleTest : InitialiseDatabasePerClassTestBase() {

  @Nested
  inner class OASysRiskManagement {

    @Test
    fun `No JWT returns 401`() {
      webTestClient.get()
        .uri("/cas3/people/CRN/oasys/riskManagement")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Return 404 if needs record can't be found for the CRN`() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())
      apAndOASysMockNeedsDetails404Call(CRN)

      webTestClient.get()
        .uri("/cas3/people/$CRN/oasys/riskManagement")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun success() {
      val (_, jwt) = givenAUser()

      apDeliusContextMockUserAccess(CaseAccessFactory().withCrn(CRN).produce())

      val riskManagementPlan = RiskManagementPlanFactory()
        .withSupervision("The supervision answer")
        .produce()
      apAndOASysMockSuccessfulRiskManagementPlanCall(CRN, riskManagementPlan)

      val result = webTestClient.get()
        .uri("/cas3/people/$CRN/oasys/riskManagement")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas3OASysGroup>()

      assertThat(result.assessmentMetadata.hasApplicableAssessment).isTrue()
      assertThat(result.answers).contains(
        OASysQuestion(
          label = "Supervision",
          questionNumber = "RM30",
          answer = "The supervision answer",
        ),
      )
    }
  }

  @Nested
  inner class GetPersonRiskProfileTest {

    @Test
    fun `Getting a person risk profile for a CRN without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas3/people/CRN/risk-profile")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Getting a person risk profile for a CRN returns OK with correct body`() {
      val caseDetail = CaseDetailFactory()
        .withMappaDetail(
          MappaDetailFactory()
            .withLevelDescription("Level1 MAPPA")
            .withCategoryDescription("CAT2")
            .produce(),
        )
        .withRegistrations(
          listOf(
            RegistrationFactory().withDescription("Arson").produce(),
            RegistrationFactory().withDescription("Weapon").produce(),

          ),
        )
        .produce()
      val roshRatings = RoshRatingsFactory().produce()
      val tier = Tier(tierScore = "A1", calculationId = UUID.randomUUID(), calculationDate = LocalDateTime.now(), changeReason = "Change Reason")
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR))
      givenAnOffender { offenderDetails, _ ->
        apDeliusContextMockSuccessfulCaseDetailCall(offenderDetails.otherIds.crn, caseDetail)
        apAndOASysMockSuccessfulRoshRatingsCall(offenderDetails.otherIds.crn, roshRatings)
        hmppsTierMockSuccessfulTierCall(
          offenderDetails.otherIds.crn,
          tier,
        )

        val response = webTestClient.get()
          .uri("/cas3/people/${offenderDetails.otherIds.crn}/risk-profile")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<PersonRisks>()

        val personRisks = response.responseBody.blockFirst()!!

        assertThat(personRisks).isNotNull
        assertThat(personRisks.tier.value!!.level).isEqualTo("A1")
        assertThat(personRisks.roshRisks.value!!.overallRisk).isEqualTo("Low")
        assertThat(personRisks.mappa.value!!.level).isEqualTo("CAT CAT2/LEVEL Level1 MAPPA")
        assertThat(personRisks.flags.value!![0]).isEqualTo("Arson")
        assertThat(personRisks.flags.value[1]).isEqualTo("Weapon")
      }
    }
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MappaDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TierFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apAndOASysMockSuccessfulRoshRatingsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMockSuccessfulTierCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import java.time.LocalDateTime
import java.util.UUID

class Cas3PeopleTest : IntegrationTestBase() {

  @Test
  fun `Getting a person risk profile without a JWT returns 401`() {
    webTestClient.get()
      .uri("/cas3/people/CRN/risk-profile")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting a person risk profile for an unregistered CRN returns 404`() {
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR))

    webTestClient.get()
      .uri("/cas3/people/NOT_FOUND_CRN/risk-profile")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Getting a person risk profile for a registered CRN returns OK with the correct body`() {
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR))

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
    val tier = Tier(
      tierScore = "A",
      calculationId = UUID.randomUUID(),
      calculationDate = LocalDateTime.now(),
      changeReason = "Change Reason",
    )

    givenAnOffender { offenderDetails, _ ->
      val crn = offenderDetails.otherIds.crn

      caseRepository.saveAndFlush(
        CaseEntityFactory()
          .withCrn(crn)
          .withTierV2(
            TierFactory()
              .withTierScore("A")
              .produce(),
          )
          .produce(),
      )

      apDeliusContextMockSuccessfulCaseDetailCall(crn, caseDetail)
      apAndOASysMockSuccessfulRoshRatingsCall(crn, roshRatings)
      hmppsTierMockSuccessfulTierCall(crn, tier)

      val personRisks = webTestClient.get()
        .uri("/cas3/people/$crn/risk-profile")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(PersonRisks::class.java)
        .responseBody
        .blockFirst()!!

      assertThat(personRisks.tier.value!!.level).isEqualTo("A")
      assertThat(personRisks.roshRisks.value!!.overallRisk).isEqualTo("Low")
      assertThat(personRisks.mappa.value!!.level).isEqualTo("CAT CAT2/LEVEL Level1 MAPPA")
      assertThat(personRisks.flags.value!!).containsExactly("Arson", "Weapon")
    }
  }
}

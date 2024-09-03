package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole

class Cas1PremisesTest : IntegrationTestBase() {

  @Nested
  inner class GetPremisesSummary : InitialiseDatabasePerClassTestBase() {

    lateinit var premises: ApprovedPremisesEntity

    @BeforeAll
    fun setupTestData() {
      val region = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea {
          apAreaEntityFactory.produceAndPersist()
        }
      }

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name")
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = `Given a User`(roles = listOf(UserRole.CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Returns premises summary`() {
      val (_, jwt) = `Given a User`(roles = listOf(UserRole.CAS1_FUTURE_MANAGER))

      val summary = webTestClient.get()
        .uri("/cas1/premises/${premises.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesSummary::class.java).responseBody.blockFirst()!!

      assertThat(summary.name).isEqualTo("the premises name")
    }
  }
}

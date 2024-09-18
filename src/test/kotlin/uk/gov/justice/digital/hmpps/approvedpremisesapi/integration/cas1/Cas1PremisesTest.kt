package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an AP Area`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_FUTURE_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1PremisesTest : IntegrationTestBase() {

  @Nested
  inner class GetPremisesSummary : InitialiseDatabasePerClassTestBase() {

    lateinit var premises: ApprovedPremisesEntity

    @BeforeAll
    fun setupTestData() {
      val region = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea { `Given an AP Area`(name = "The ap area name") }
      }

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name")
        withApCode("the ap code")
        withPostcode("the postcode")
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Returns 404 if premise doesn't exist`() {
      val (_, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.get()
        .uri("/cas1/premises/${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Returns premises summary`() {
      val (user, jwt) = `Given a User`(roles = listOf(CAS1_FUTURE_MANAGER))

      val beds = bedEntityFactory.produceAndPersistMultiple(5) {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      cas1OutOfServiceBedEntityFactory.produceAndPersist {
        withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
        withBed(beds[0])
      }.apply {
        this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
          withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
          withCreatedBy(user)
          withOutOfServiceBed(this@apply)
          withStartDate(LocalDate.now().plusDays(2))
          withEndDate(LocalDate.now().plusDays(4))
          withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
        }
      }

      val summary = webTestClient.get()
        .uri("/cas1/premises/${premises.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesSummary::class.java).responseBody.blockFirst()!!

      assertThat(summary.id).isEqualTo(premises.id)
      assertThat(summary.name).isEqualTo("the premises name")
      assertThat(summary.apCode).isEqualTo("the ap code")
      assertThat(summary.postcode).isEqualTo("the postcode")
      assertThat(summary.bedCount).isEqualTo(5)
      assertThat(summary.availableBeds).isEqualTo(4)
      assertThat(summary.outOfServiceBeds).isEqualTo(1)
      assertThat(summary.apArea.name).isEqualTo("The ap area name")
    }
  }

  @Nested
  inner class GetPremisesSummaries : InitialiseDatabasePerClassTestBase() {

    lateinit var premises1Male: ApprovedPremisesEntity
    lateinit var premises2Female: ApprovedPremisesEntity
    lateinit var premises3Male: ApprovedPremisesEntity

    @BeforeAll
    fun setupTestData() {
      val region1 = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea {
          apAreaEntityFactory.produceAndPersist {
            withName("the ap area name 1")
          }
        }
      }

      val region2 = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea {
          apAreaEntityFactory.produceAndPersist {
            withName("the ap area name 2")
          }
        }
      }

      premises1Male = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name 1")
        withGender(ApprovedPremisesGender.MALE)
        withYieldedProbationRegion { region1 }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      premises2Female = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name 2")
        withGender(ApprovedPremisesGender.FEMALE)
        withYieldedProbationRegion { region2 }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }

      premises3Male = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name 3")
        withGender(ApprovedPremisesGender.MALE)
        withYieldedProbationRegion { region2 }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      }
    }

    @Test
    fun `Returns premises summaries with no filters applied`() {
      val (_, jwt) = `Given a User`()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(3)

      assertThat(summaries[0].id).isEqualTo(premises1Male.id)
      assertThat(summaries[0].name).isEqualTo("the premises name 1")
      assertThat(summaries[0].apArea.name).isEqualTo("the ap area name 1")

      assertThat(summaries[1].id).isEqualTo(premises2Female.id)
      assertThat(summaries[1].name).isEqualTo("the premises name 2")
      assertThat(summaries[1].apArea.name).isEqualTo("the ap area name 2")

      assertThat(summaries[2].id).isEqualTo(premises3Male.id)
      assertThat(summaries[2].name).isEqualTo("the premises name 3")
      assertThat(summaries[2].apArea.name).isEqualTo("the ap area name 2")
    }

    @Test
    fun `Returns premises summaries where gender is male`() {
      val (_, jwt) = `Given a User`()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary?gender=male")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(2)

      assertThat(summaries[0].id).isEqualTo(premises1Male.id)
      assertThat(summaries[0].name).isEqualTo("the premises name 1")
      assertThat(summaries[0].apArea.name).isEqualTo("the ap area name 1")

      assertThat(summaries[1].id).isEqualTo(premises3Male.id)
      assertThat(summaries[1].name).isEqualTo("the premises name 3")
      assertThat(summaries[1].apArea.name).isEqualTo("the ap area name 2")
    }

    @Test
    fun `Returns premises summaries where gender is female`() {
      val (_, jwt) = `Given a User`()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary?gender=female")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(1)

      assertThat(summaries[0].id).isEqualTo(premises2Female.id)
      assertThat(summaries[0].name).isEqualTo("the premises name 2")
      assertThat(summaries[0].apArea.name).isEqualTo("the ap area name 2")
    }
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUnsuccessfulCaseSummaryCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMockSuccessfulTierCall
import java.time.LocalDateTime
import java.util.UUID

class BackfillCasesJobTest : MigrationJobTestBase() {

  @Test
  fun `backfill unique cases from all 3 application tables`() {
    val probationRegion = givenAProbationRegion()
    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }
    val cas2User = cas2UserEntityFactory.produceAndPersist()

    // 1. Offline Application
    val offlineApp = offlineApplicationEntityFactory.produceAndPersist {
      withCrn("CRN1")
      withName("Offline Name")
    }

    // 2. CAS2 Application
    val cas2App = cas2ApplicationEntityFactory.produceAndPersist {
      withCrn("CRN2")
      withNomsNumber("NOMS2")
      withCreatedByUser(cas2User)
      withSubmittedAt(java.time.OffsetDateTime.now())
    }
    cas2AssessmentEntityFactory.produceAndPersist {
      withApplication(cas2App)
    }

    // 3. TA Application
    val taApp = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn("CRN3")
      withName("TA Name")
      withNomsNumber("NOMS3")
      withProbationRegion(probationRegion)
      withCreatedByUser(user)
    }

    val taAppWithCrnExistInCas2 = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn("CRN2")
      withName("TA Name")
      withNomsNumber("NOMS3")
      withProbationRegion(probationRegion)
      withCreatedByUser(user)
    }

    // Mock API responses
    val case1 = CaseSummaryFactory().withCrn("CRN1").withName(NameFactory().withForename("Delius").withSurname("One").produce()).withNomsId("NOMS1").produce()
    val case2 = CaseSummaryFactory().withCrn("CRN2").withName(NameFactory().withForename("Delius").withSurname("Two").produce()).withNomsId("NOMS2").produce()
    val case3 = CaseSummaryFactory().withCrn("CRN3").withName(NameFactory().withForename("Delius").withSurname("Three").produce()).withNomsId("NOMS3").produce()

    apDeliusContextAddListCaseSummaryToBulkResponse(listOf(case1, case2, case3))

    hmppsTierMockSuccessfulTierCall("CRN1", Tier("A1", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason1"))
    hmppsTierMockSuccessfulTierCall("CRN2", Tier("B2", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason2"))
    hmppsTierMockSuccessfulTierCall("CRN3", Tier("C3", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason3"))

    migrationJobService.runMigrationJob(MigrationJobType.backfillCases, 10)

    val c1 = caseRepository.findByCrn("CRN1")!!
    assertThat(c1.name).isEqualTo("DELIUS ONE")
    assertThat(c1.nomsNumber).isEqualTo("NOMS1")
    assertThat(c1.tierV2!!.tierScore).isEqualTo("A1")

    val c2 = caseRepository.findByCrn("CRN2")!!
    assertThat(c2.name).isEqualTo("DELIUS TWO")
    assertThat(c2.nomsNumber).isEqualTo("NOMS2")
    assertThat(c2.tierV2!!.tierScore).isEqualTo("B2")

    val c3 = caseRepository.findByCrn("CRN3")!!
    assertThat(c3.name).isEqualTo("DELIUS THREE")
    assertThat(c3.nomsNumber).isEqualTo("NOMS3")
    assertThat(c3.tierV2!!.tierScore).isEqualTo("C3")
  }

  @Test
  fun `backfill cases ignores CRNs already in cases table`() {
    val probationRegion = givenAProbationRegion()
    userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    // Existing case
    caseRepository.save(
      CaseEntity(
        id = UUID.randomUUID(),
        crn = "EXISTING_CRN",
        name = "Existing Name",
        nomsNumber = "NOMS_EXISTING",
        tierV2 = uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.Tier("B1", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason1", version = TierVersion.V2),
        createdAt = java.time.OffsetDateTime.now(),
        lastUpdatedAt = java.time.OffsetDateTime.now(),
      ),
    )

    // Application with same CRN
    offlineApplicationEntityFactory.produceAndPersist {
      withCrn("EXISTING_CRN")
    }

    // Application with new CRN
    offlineApplicationEntityFactory.produceAndPersist {
      withCrn("NEW_CRN")
    }

    val caseNew = CaseSummaryFactory().withCrn("NEW_CRN").withName(NameFactory().withForename("New").withSurname("Name").produce()).withNomsId("NOMS_NEW").produce()
    apDeliusContextAddListCaseSummaryToBulkResponse(listOf(caseNew))
    hmppsTierMockSuccessfulTierCall("NEW_CRN", Tier("A1", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason1"))

    migrationJobService.runMigrationJob(MigrationJobType.backfillCases, 10)

    val existing = caseRepository.findByCrn("EXISTING_CRN")!!
    assertThat(existing.name).isEqualTo("Existing Name") // Unchanged

    val new = caseRepository.findByCrn("NEW_CRN")!!
    assertThat(new.name).isEqualTo("NEW NAME")
  }

  @Test
  fun `backfill cases uses fallbacks when Delius summary fails`() {
    val probationRegion = givenAProbationRegion()
    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    // TA Application provides name and NOMS
    temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn("CRN_FALLBACK")
      withName("Persisted Name")
      withNomsNumber("NOMS_PERSISTED")
      withProbationRegion(probationRegion)
      withCreatedByUser(user)
    }

    apDeliusContextMockUnsuccessfulCaseSummaryCall(500)

    hmppsTierMockSuccessfulTierCall("CRN_FALLBACK", Tier("D4", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason 2"))

    migrationJobService.runMigrationJob(MigrationJobType.backfillCases, 10)

    val c = caseRepository.findByCrn("CRN_FALLBACK")!!
    assertThat(c.name).isEqualTo("PERSISTED NAME")
    assertThat(c.nomsNumber).isEqualTo("NOMS_PERSISTED")
    assertThat(c.tierV2!!.tierScore).isEqualTo("D4")
  }
}

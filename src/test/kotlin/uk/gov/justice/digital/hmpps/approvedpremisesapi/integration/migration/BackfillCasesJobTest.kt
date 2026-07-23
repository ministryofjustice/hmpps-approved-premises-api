package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesErrorResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextCaseSummariesMultipleCases
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMock404V3TierCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMockSuccessfulTierCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMockSuccessfulV3TierCall
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier as UpstreamTier

class BackfillCasesJobTest : MigrationJobTestBase() {

  @Test
  fun `backfill job adds missing cases from all application tables`() {
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

    val apApp = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn("CRN4")
      withName("AP Name")
      withNomsNumber("NOMS4")
      withCreatedByUser(user)
    }

    // 4. CAS3 Booking
    val pdu = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }
    val cas3Premises = cas3PremisesEntityFactory.produceAndPersist {
      withProbationDeliveryUnit(pdu)
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }
    val cas3Bedspace = cas3BedspaceEntityFactory.produceAndPersist {
      withPremises(cas3Premises)
    }
    cas3BookingEntityFactory.produceAndPersist {
      withCrn("CRN5")
      withOffenderName("CAS3 Name")
      withNomsNumber("NOMS5")
      withPremises(cas3Premises)
      withBedspace(cas3Bedspace)
    }

    // Mock API responses
    val case1 = CaseSummaryFactory().withCrn("CRN1").withName(NameFactory().withForename("Delius").withSurname("One").produce()).withNomsId("NOMS1").produce()
    val case2 = CaseSummaryFactory().withCrn("CRN2").withName(NameFactory().withForename("Delius").withSurname("Two").produce()).withNomsId("NOMS2").produce()
    val case3 = CaseSummaryFactory().withCrn("CRN3").withName(NameFactory().withForename("Delius").withSurname("Three").produce()).withNomsId("NOMS3").produce()
    val case4 = CaseSummaryFactory().withCrn("CRN4").withName(NameFactory().withForename("Delius").withSurname("Four").produce()).withNomsId("NOMS4").produce()
    val case5 = CaseSummaryFactory().withCrn("CRN5").withName(NameFactory().withForename("Delius").withSurname("Five").produce()).withNomsId("NOMS5").produce()

    apDeliusContextCaseSummariesMultipleCases(listOf(case1, case2, case3, case4, case5))

    // tier_v2
    hmppsTierMockSuccessfulTierCall("CRN1", UpstreamTier("A1", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason1"))
    hmppsTierMockSuccessfulTierCall("CRN2", UpstreamTier("B2", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason2"))
    hmppsTierMockSuccessfulTierCall("CRN3", UpstreamTier("C3", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason3"))
    hmppsTierMockSuccessfulTierCall("CRN4", UpstreamTier("D4", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason4"))
    hmppsTierMockSuccessfulTierCall("CRN5", UpstreamTier("E5", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason5"))

    // tier_v3 with failure
    hmppsTierMockSuccessfulV3TierCall("CRN1", UpstreamTier("A1", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason1"))
    hmppsTierMock404V3TierCall("CRN2")
    hmppsTierMockSuccessfulV3TierCall("CRN3", UpstreamTier("C3", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason3"))
    hmppsTierMockSuccessfulV3TierCall("CRN4", UpstreamTier("D4", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason4"))
    hmppsTierMockSuccessfulV3TierCall("CRN5", UpstreamTier("E5", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason5"))

    migrationJobService.runMigrationJob(MigrationJobType.backfillCases, 10)

    val c1 = caseRepository.findByCrn("CRN1")!!
    assertThat(c1.name).isEqualTo("DELIUS ONE")
    assertThat(c1.nomsNumber).isEqualTo("NOMS1")
    assertThat(c1.tierV2!!.tierScore).isEqualTo("A1")
    assertThat(c1.tierV3!!.tierScore).isEqualTo("A1")

    val c2 = caseRepository.findByCrn("CRN2")!!
    assertThat(c2.name).isEqualTo("DELIUS TWO")
    assertThat(c2.nomsNumber).isEqualTo("NOMS2")
    assertThat(c2.tierV2!!.tierScore).isEqualTo("B2")
    assertThat(c2.tierV3).isNull()

    val c3 = caseRepository.findByCrn("CRN3")!!
    assertThat(c3.name).isEqualTo("DELIUS THREE")
    assertThat(c3.nomsNumber).isEqualTo("NOMS3")
    assertThat(c3.tierV2!!.tierScore).isEqualTo("C3")
    assertThat(c3.tierV3!!.tierScore).isEqualTo("C3")

    val c4 = caseRepository.findByCrn("CRN4")!!
    assertThat(c4.name).isEqualTo("DELIUS FOUR")
    assertThat(c4.nomsNumber).isEqualTo("NOMS4")
    assertThat(c4.tierV2!!.tierScore).isEqualTo("D4")
    assertThat(c4.tierV3!!.tierScore).isEqualTo("D4")

    val c5 = caseRepository.findByCrn("CRN5")!!
    assertThat(c5.name).isEqualTo("DELIUS FIVE")
    assertThat(c5.nomsNumber).isEqualTo("NOMS5")
    assertThat(c5.tierV2!!.tierScore).isEqualTo("E5")
    assertThat(c5.tierV3!!.tierScore).isEqualTo("E5")
  }

  @Test
  fun `backfill job updates cases already in cases table with missing tiers`() {
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
        tierV2 = Tier("B1", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason1", version = TierVersion.V2),
        createdAt = java.time.OffsetDateTime.now(),
        lastUpdatedAt = java.time.OffsetDateTime.now(),
        tierV3 = null,
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
    apDeliusContextCaseSummariesMultipleCases(listOf(caseNew))

    hmppsTierMockSuccessfulTierCall("NEW_CRN", UpstreamTier("A1", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason1"))
    hmppsTierMockSuccessfulV3TierCall("NEW_CRN", UpstreamTier("C1", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason1"))

    hmppsTierMockSuccessfulV3TierCall("EXISTING_CRN", UpstreamTier("B1", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason1"))

    migrationJobService.runMigrationJob(MigrationJobType.backfillCases, 10)

    val existing = caseRepository.findByCrn("EXISTING_CRN")!!
    assertThat(existing.name).isEqualTo("Existing Name") // Unchanged
    assertThat(existing.tierV2!!.tierScore).isEqualTo("B1") // Unchanged
    assertThat(existing.tierV3!!.tierScore).isEqualTo("B1")

    val new = caseRepository.findByCrn("NEW_CRN")!!
    assertThat(new.name).isEqualTo("NEW NAME")
    assertThat(new.nomsNumber).isEqualTo("NOMS_NEW")
    assertThat(new.tierV2!!.tierScore).isEqualTo("A1")
    assertThat(new.tierV3!!.tierScore).isEqualTo("C1")
  }

  @Test
  fun `backfill job uses fallbacks when Delius summary fails`() {
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

    apDeliusContextCaseSummariesErrorResponse(500)

    hmppsTierMockSuccessfulTierCall("CRN_FALLBACK", UpstreamTier("D4", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason 2"))
    hmppsTierMockSuccessfulV3TierCall("CRN_FALLBACK", UpstreamTier("D4", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason 2"))

    migrationJobService.runMigrationJob(MigrationJobType.backfillCases, 10)

    val c = caseRepository.findByCrn("CRN_FALLBACK")!!
    assertThat(c.name).isEqualTo("PERSISTED NAME")
    assertThat(c.nomsNumber).isEqualTo("NOMS_PERSISTED")
    assertThat(c.tierV2!!.tierScore).isEqualTo("D4")
    assertThat(c.tierV3!!.tierScore).isEqualTo("D4")
  }

  @Test
  fun `backfill job handles duplicate CRNs by picking the latest application details`() {
    val crn = "DUPLICATE_CRN"

    val probationRegion = givenAProbationRegion()
    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    // Older application
    temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withCrn(crn)
      withName("Old Name")
      withCreatedAt(OffsetDateTime.now().minusDays(2))
      withProbationRegion(probationRegion)
      withCreatedByUser(user)
    }

    // Newer application
    approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(crn)
      withName("New Name")
      withCreatedAt(OffsetDateTime.now().minusDays(1))
      withCreatedByUser(user)
    }

    apDeliusContextCaseSummariesErrorResponse(500)

    hmppsTierMockSuccessfulTierCall(crn, UpstreamTier("A1", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason1"))
    hmppsTierMockSuccessfulV3TierCall(crn, UpstreamTier("A1", UUID.randomUUID(), LocalDateTime.now(), changeReason = "reason1"))

    migrationJobService.runMigrationJob(MigrationJobType.backfillCases, 10)

    val c = caseRepository.findByCrn(crn)!!
    assertThat(c.name).isEqualTo("NEW NAME")
    assertThat(c.tierV2!!.tierScore).isEqualTo("A1")
    assertThat(c.tierV3!!.tierScore).isEqualTo("A1")
  }
}

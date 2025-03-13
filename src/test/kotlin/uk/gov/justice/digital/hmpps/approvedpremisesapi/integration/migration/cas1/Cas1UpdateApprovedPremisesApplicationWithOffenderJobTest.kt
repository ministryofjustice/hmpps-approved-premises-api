package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1OffenderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextEmptyCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMockSuccessfulTierCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppstier.Tier
import java.time.LocalDateTime
import java.util.UUID

class Cas1UpdateApprovedPremisesApplicationWithOffenderJobTest : IntegrationTestBase() {

  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun `should link all applications without offender to new offender`() {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val probationRegion = givenAProbationRegion()

    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val levels = listOf("B1", "C2", "D3")

    apDeliusContextAddListCaseSummaryToBulkResponse(
      listOf(
        CaseSummaryFactory()
          .withCrn("CRN1")
          .withName(Name("John", "Smith", listOf("Andrew")))
          .produce(),
      ),
    )

    hmppsTierMockSuccessfulTierCall(
      "CRN1",
      Tier(
        tierScore = levels.random(),
        calculationId = UUID.randomUUID(),
        calculationDate = LocalDateTime.parse("2022-09-06T14:59:00"),
      ),
    )

    val applications = generateSequence {
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
        withCreatedByUser(user)
        withCrn("CRN1")
      }
    }.take(2).toList()

    val existingOffenders = cas1OffenderRepository.findAll()
    assertThat(existingOffenders.size).isEqualTo(0)

    migrationJobService.runMigrationJob(MigrationJobType.cas1ApplicationsWithOffender, 1)

    val newOffenders = cas1OffenderRepository.findAll()
    assertThat(newOffenders.size).isEqualTo(1)

    applications.forEach {
      val approvedPremisesApplicationEntity = approvedPremisesApplicationRepository.findByIdOrNull(it.id) as ApprovedPremisesApplicationEntity
      assertThat(approvedPremisesApplicationEntity.cas1OffenderEntity).isNotNull
      assertThat(approvedPremisesApplicationEntity.cas1OffenderEntity?.crn).isEqualTo(approvedPremisesApplicationEntity.crn)
    }
  }

  @Test
  fun `should update an existing offender and associate all applications with the same CRN`() {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val probationRegion = givenAProbationRegion()

    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val levels = listOf("B1", "C2", "D3")

    apDeliusContextAddListCaseSummaryToBulkResponse(
      listOf(
        CaseSummaryFactory()
          .withCrn("CRN3")
          .withName(Name("John", "Smith", listOf("Andrew")))
          .produce(),
      ),
    )

    hmppsTierMockSuccessfulTierCall(
      "CRN3",
      Tier(
        tierScore = levels.random(),
        calculationId = UUID.randomUUID(),
        calculationDate = LocalDateTime.parse("2022-09-06T14:59:00"),
      ),
    )

    val existingOffender = Cas1OffenderEntityFactory().withCrn("CRN3").withName("name").produce()
    cas1OffenderRepository.saveAndFlush(existingOffender)

    val applications = generateSequence {
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
        withCreatedByUser(user)
        withCrn("CRN3")
      }
    }.take(2).toList()

    val existingOffenders = cas1OffenderRepository.findAll()
    assertThat(existingOffenders.size).isEqualTo(1)
    assertThat(existingOffenders.first().name).isEqualTo("name")

    migrationJobService.runMigrationJob(MigrationJobType.cas1ApplicationsWithOffender, 1)

    val offendersAfterMigration = cas1OffenderRepository.findAll()
    assertThat(offendersAfterMigration.size).isEqualTo(1)
    assertThat(offendersAfterMigration.first().name).isEqualTo("John Smith".uppercase())

    applications.forEach {
      val approvedPremisesApplicationEntity = approvedPremisesApplicationRepository.findByIdOrNull(it.id) as ApprovedPremisesApplicationEntity
      assertThat(approvedPremisesApplicationEntity.cas1OffenderEntity).isNotNull
      assertThat(approvedPremisesApplicationEntity.cas1OffenderEntity?.crn).isEqualTo(approvedPremisesApplicationEntity.crn)
    }
  }

  @Test
  fun `should create offender from application and associate when not found in delius`() {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val probationRegion = givenAProbationRegion()

    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    val levels = listOf("B1", "C2", "D3")

    apDeliusContextEmptyCaseSummaryToBulkResponse("CRN4")

    hmppsTierMockSuccessfulTierCall(
      "CRN4",
      Tier(
        tierScore = levels.random(),
        calculationId = UUID.randomUUID(),
        calculationDate = LocalDateTime.parse("2022-09-06T14:59:00"),
      ),
    )

    val applications = generateSequence {
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
        withCreatedByUser(user)
        withCrn("CRN4")
        withName("OffenderName")
      }
    }.take(2).toList()

    val existingOffenders = cas1OffenderRepository.findAll()
    assertThat(existingOffenders.size).isEqualTo(0)

    migrationJobService.runMigrationJob(MigrationJobType.cas1ApplicationsWithOffender, 1)

    val offendersAfterMigration = cas1OffenderRepository.findAll()
    assertThat(offendersAfterMigration.size).isEqualTo(1)
    assertThat(offendersAfterMigration.first().name).isEqualTo("OffenderName".uppercase())

    applications.forEach {
      val approvedPremisesApplicationEntity = approvedPremisesApplicationRepository.findByIdOrNull(it.id) as ApprovedPremisesApplicationEntity
      assertThat(approvedPremisesApplicationEntity.cas1OffenderEntity).isNotNull
      assertThat(approvedPremisesApplicationEntity.cas1OffenderEntity?.crn).isEqualTo(approvedPremisesApplicationEntity.crn)
    }
  }
}

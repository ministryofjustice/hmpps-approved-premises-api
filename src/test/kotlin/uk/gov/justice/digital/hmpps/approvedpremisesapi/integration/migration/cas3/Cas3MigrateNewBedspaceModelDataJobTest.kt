package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService

class Cas3MigrateNewBedspaceModelDataJobTest : IntegrationTestBase() {

  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun `should merged all cas3 related premises and temporary_accommodation data and migrate into new cas3_premises table`() {
    val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
    val probationRegion = givenAProbationRegion()
    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }
    val temporaryAccommodationApplications = generateSequence {
      temporaryAccommodationApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
        withProbationRegion(probationRegion)
        withCreatedByUser(user)
      }
    }.take(100).toList()

    migrationJobService.runMigrationJob(MigrationJobType.cas3BedspaceModelData, 1)

//    temporaryAccommodationApplications.forEach {
//      val application = applicationRepository.findTemporaryAccommodationApplicationById(it.id)!!
//      val offenderName = "${offendersCrnAndName[it.crn]?.forename} ${offendersCrnAndName[it.crn]?.surname}"
//      Assertions.assertThat(application.name).isEqualTo(offenderName)
//    }
    val newCasPremises = cas3PremisesRepository.findAll()
    assertThat(newCasPremises.size).isEqualTo(1)
  }

}

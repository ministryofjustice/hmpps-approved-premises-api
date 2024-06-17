package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.midlandsApplicationIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.northEastApplicationIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.seeApplicationIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.MigrationJobService
import java.time.OffsetDateTime

class ApAreaMigrationTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun `it should migrate the AP Area of all applications`() {
    probationAreaProbationRegionMappingRepository.deleteAll()
    probationDeliveryUnitRepository.deleteAll()
    probationRegionRepository.deleteAll()
    apAreaRepository.deleteAll()

    val apArea1 = apAreaEntityFactory.produceAndPersist()
    val apArea2 = apAreaEntityFactory.produceAndPersist()

    val probationRegion1 = probationRegionEntityFactory.produceAndPersist {
      withApArea(apArea1)
    }

    val probationRegion2 = probationRegionEntityFactory.produceAndPersist {
      withApArea(apArea2)
    }

    val midlandsApArea = apAreaEntityFactory.produceAndPersist {
      withName("Midlands")
    }
    val southEastAndEasternApArea = apAreaEntityFactory.produceAndPersist {
      withName("South East & Eastern")
    }
    val northEastApArea = apAreaEntityFactory.produceAndPersist {
      withName("North East")
    }

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val midlandsApplications = midlandsApplicationIds.map {
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withId(it)
        withApplicationSchema(applicationSchema)
        withApArea(null)
        withYieldedCreatedByUser {
          userEntityFactory.produceAndPersist {
            withProbationRegion(probationRegion1)
            withApArea(apArea1)
          }
        }
      }
    }

    val seeApplications = seeApplicationIds.map {
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withId(it)
        withApplicationSchema(applicationSchema)
        withApArea(null)
        withYieldedCreatedByUser {
          userEntityFactory.produceAndPersist {
            withProbationRegion(probationRegion1)
            withApArea(apArea1)
          }
        }
      }
    }

    val northEastApplications = northEastApplicationIds.map {
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withId(it)
        withApplicationSchema(applicationSchema)
        withApArea(null)
        withYieldedCreatedByUser {
          userEntityFactory.produceAndPersist {
            withProbationRegion(probationRegion1)
            withApArea(apArea1)
          }
        }
      }
    }

    val apArea1Applications = approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(3) {
      withApplicationSchema(applicationSchema)
      withApArea(null)
      withSubmittedAt(OffsetDateTime.now())
      withYieldedCreatedByUser {
        userEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion1)
          withApArea(apArea1)
        }
      }
    }.toMutableList()

    val apArea2Applications = approvedPremisesApplicationEntityFactory.produceAndPersistMultiple(5) {
      withApplicationSchema(applicationSchema)
      withApArea(null)
      withSubmittedAt(OffsetDateTime.now())
      withYieldedCreatedByUser {
        userEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion2)
          withApArea(apArea2)
        }
      }
    }.toMutableList()

    migrationJobService.runMigrationJob(MigrationJobType.applicationApAreas)

    assertApplicationsHaveTheCorrectApArea(midlandsApplications, midlandsApArea)
    assertApplicationsHaveTheCorrectApArea(seeApplications, southEastAndEasternApArea)
    assertApplicationsHaveTheCorrectApArea(northEastApplications, northEastApArea)

    assertApplicationsHaveTheCorrectApArea(apArea1Applications, apArea1)
    assertApplicationsHaveTheCorrectApArea(apArea2Applications, apArea2)
  }

  private fun assertApplicationsHaveTheCorrectApArea(applications: List<ApprovedPremisesApplicationEntity>, apArea: ApAreaEntity) {
    val apIds = applications.map { it.id }
    val updatedApplications = approvedPremisesApplicationRepository.findAllById(apIds)

    updatedApplications.forEach {
      assertThat(it.apArea).isNotNull()
      assertThat(it.apArea!!.id).isEqualTo(apArea.id)
    }
  }
}

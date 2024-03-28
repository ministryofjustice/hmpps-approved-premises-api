package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.ApDeliusContext_addListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.MigrationJobService

class Cas3UpdateApplicationOffenderNameJobTest : IntegrationTestBase() {

  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Autowired
  lateinit var applicationRepository: ApplicationRepository

  @Test
  fun `all applications offender name are updated from Community API`() {
    `Given an Offender`(
      offenderDetailsConfigBlock = {
        withNomsNumber(null)
      },
    ) { offenderDetails, _ ->

      val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val probationRegion = probationRegionEntityFactory.produceAndPersist {
        withApArea(apAreaEntityFactory.produceAndPersist())
      }

      val user = userEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      val temporaryAccommodationApplications = generateSequence {
        temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withProbationRegion(probationRegion)
          withCreatedByUser(user)
        }
      }.take(10).toList()

      val offendersCrnAndName = temporaryAccommodationApplications.associateBy(
        keySelector = { it.crn },
        valueTransform = { NameFactory().produce() },
      )

      val cases = temporaryAccommodationApplications.map {
        CaseSummaryFactory()
          .withCrn(it.crn)
          .withName(offendersCrnAndName[it.crn]!!)
          .produce()
      }

      ApDeliusContext_addListCaseSummaryToBulkResponse(cases)

      migrationJobService.runMigrationJob(MigrationJobType.cas3ApplicationOffenderName, 10)

      temporaryAccommodationApplications.forEach {
        val application = applicationRepository.findTemporaryAccommodationApplicationById(it.id)!!
        val offenderName = "${offendersCrnAndName[it.crn]?.forename} ${offendersCrnAndName[it.crn]?.surname}"
        Assertions.assertThat(application.name).isEqualTo(offenderName)
      }
    }
  }

  @Test
  fun `all applications offender name are updated from Community API when delius name is empty`() {
    `Given an Offender`(
      offenderDetailsConfigBlock = {
        withNomsNumber(null)
      },
    ) { offenderDetails, _ ->

      val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val probationRegion = probationRegionEntityFactory.produceAndPersist {
        withApArea(apAreaEntityFactory.produceAndPersist())
      }

      val user = userEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      val temporaryAccommodationApplications = generateSequence {
        temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withProbationRegion(probationRegion)
          withCreatedByUser(user)
        }
      }.take(10).toList()

      val offendersCrnAndName = temporaryAccommodationApplications.associateBy(
        keySelector = { it.crn },
        valueTransform = { NameFactory().produce() },
      )

      val cases = temporaryAccommodationApplications.map {
        CaseSummaryFactory()
          .withCrn(it.crn)
          .withName(offendersCrnAndName[it.crn]!!)
          .produce()
      }

      ApDeliusContext_addListCaseSummaryToBulkResponse(cases)

      mockOffenderUserAccessCommunityApiCall("", temporaryAccommodationApplications.first().crn, true, true)

      migrationJobService.runMigrationJob(MigrationJobType.cas3ApplicationOffenderName, 10)

      temporaryAccommodationApplications.forEach {
        val application = applicationRepository.findTemporaryAccommodationApplicationById(it.id)!!
        val offenderName = "${offendersCrnAndName[it.crn]?.forename} ${offendersCrnAndName[it.crn]?.surname}"
        Assertions.assertThat(application.name).isEqualTo(offenderName)
      }
    }
  }
}

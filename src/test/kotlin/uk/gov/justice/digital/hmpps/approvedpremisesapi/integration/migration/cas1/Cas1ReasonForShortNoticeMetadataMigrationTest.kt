package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.MigrationJobService
import java.time.OffsetDateTime

class Cas1ReasonForShortNoticeMetadataMigrationTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  lateinit var application: ApprovedPremisesApplicationEntity

  @BeforeEach
  fun setup() {
    val (user, _) = `Given a User`()
    val (offenderDetails, _) = `Given an Offender`()

    application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withCrn(offenderDetails.otherIds.crn)
      withConvictionId(12345)
      withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
      withSubmittedAt(OffsetDateTime.now())
      withData("{ }")
    }
  }

  @Test
  fun do_nothing_if_no_short_notice_reason_on_application() {
    domainEventFactory.produceAndPersist {
      withApplicationId(application.id)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
    }

    migrationJobService.runMigrationJob(MigrationJobType.cas1PopulateAppReasonForShortNoticeMetadata)

    val submittedDomainEvent = domainEventRepository.findByApplicationId(application.id)
      .first { it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED }
    assertThat(submittedDomainEvent.metadata).isEmpty()
  }

  @Test
  fun populate_short_notice_metadata_reason_and_other_if_defined() {
    domainEventFactory.produceAndPersist {
      withApplicationId(application.id)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
    }

    application.data = """
         {
            "basic-information": {
                "reason-for-short-notice": {
                    "reason": "riskEscalated",
                    "other": "some reason here"
                }
            }
         }
    """.trimMargin()
    approvedPremisesApplicationRepository.save(application)

    migrationJobService.runMigrationJob(MigrationJobType.cas1PopulateAppReasonForShortNoticeMetadata)

    val submittedDomainEvent = domainEventRepository.findByApplicationId(application.id)
      .first { it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED }
    assertThat(submittedDomainEvent.metadata).hasSize(2)
    assertThat(submittedDomainEvent.metadata).isEqualTo(
      mapOf(
        MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE to "riskEscalated",
        MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER to "some reason here",
      ),
    )
  }

  @Test
  fun dont_populate_short_notice_other_metadata_if_not_defined() {
    domainEventFactory.produceAndPersist {
      withApplicationId(application.id)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
    }

    application.data = """
         {
            "basic-information": {
                "reason-for-short-notice": {
                    "reason": "theReason"
                }
            }
         }
    """.trimMargin()
    approvedPremisesApplicationRepository.save(application)

    migrationJobService.runMigrationJob(MigrationJobType.cas1PopulateAppReasonForShortNoticeMetadata)

    val submittedDomainEvent = domainEventRepository.findByApplicationId(application.id)
      .first { it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED }
    assertThat(submittedDomainEvent.metadata).hasSize(1)
    assertThat(submittedDomainEvent.metadata).isEqualTo(
      mapOf(
        MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE to "theReason",
      ),
    )
  }

  @Test
  fun dont_overwrite_existing_metadata() {
    domainEventFactory.produceAndPersist {
      withApplicationId(application.id)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
      withMetadata(
        mapOf(
          MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE to "originalShortReasonValue",
          MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER to "originalShortNoticeOtherValue",
        ),
      )
    }

    application.data = """
         {
            "basic-information": {
                "reason-for-short-notice": {
                    "reason": "newReason",
                    "other": "newOther"
                }
            }
         }
    """.trimMargin()
    approvedPremisesApplicationRepository.save(application)

    migrationJobService.runMigrationJob(MigrationJobType.cas1PopulateAppReasonForShortNoticeMetadata)

    val submittedDomainEvent = domainEventRepository.findByApplicationId(application.id)
      .first { it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED }
    assertThat(submittedDomainEvent.metadata).hasSize(2)
    assertThat(submittedDomainEvent.metadata).isEqualTo(
      mapOf(
        MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE to "originalShortReasonValue",
        MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER to "originalShortNoticeOtherValue",
      ),
    )
  }
}

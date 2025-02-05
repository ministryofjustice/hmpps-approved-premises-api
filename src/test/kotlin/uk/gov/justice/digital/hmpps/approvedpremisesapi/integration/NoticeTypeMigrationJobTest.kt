package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.random.Random

class NoticeTypeMigrationJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun `it should migrate notice types of applications`() {
    val emergencyApplicationIds = generateSequence {
      createApplication(true, OffsetDateTime.now(), OffsetDateTime.now())
    }.take(5).toMutableList().map { it.id }

    val shortNoticeApplicationIds = generateSequence {
      val createdAt = OffsetDateTime.now().minusDays(Random.nextLong(1, 365))
      val arrivalDate = createdAt.plusDays(Random.nextLong(1, 27))
      createApplication(false, createdAt, arrivalDate)
    }.take(9).toMutableList().map { it.id }

    val standardApplicationIds = generateSequence {
      val createdAt = OffsetDateTime.now().minusDays(Random.nextLong(1, 365))
      val arrivalDate = createdAt.plusDays(Random.nextLong(28, 365))
      createApplication(false, createdAt, arrivalDate)
    }.take(12).toMutableList().map { it.id }

    val unsubmittedApplicationIds = generateSequence {
      val createdAt = OffsetDateTime.now().minusDays(Random.nextLong(1, 365))
      createApplication(null, createdAt, null)
    }.take(4).toMutableList().map { it.id }

    migrationJobService.runMigrationJob(MigrationJobType.cas1NoticeTypes, 1)

    assertApplicationsHaveCorrectNoticeType(emergencyApplicationIds, Cas1ApplicationTimelinessCategory.emergency)
    assertApplicationsHaveCorrectNoticeType(shortNoticeApplicationIds, Cas1ApplicationTimelinessCategory.shortNotice)
    assertApplicationsHaveCorrectNoticeType(standardApplicationIds, Cas1ApplicationTimelinessCategory.standard)
    assertApplicationsHaveCorrectNoticeType(unsubmittedApplicationIds, null)
  }

  private fun assertApplicationsHaveCorrectNoticeType(applicationIds: List<UUID>, noticeType: Cas1ApplicationTimelinessCategory?) {
    val applications = approvedPremisesApplicationRepository.findAllById(applicationIds)

    applications.forEach {
      assertThat(it.noticeType).isEqualTo(noticeType)
    }
  }

  private fun createApplication(isEmergencyApplication: Boolean?, createdAt: OffsetDateTime, arrivalDate: OffsetDateTime?): ApprovedPremisesApplicationEntity {
    val (applicant, _) = givenAUser()
    val (offenderDetails, _) = givenAnOffender()

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
      withPermissiveSchema()
    }

    return approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(applicant)
      withApplicationSchema(applicationSchema)
      withSubmittedAt(OffsetDateTime.now())
      withIsEmergencyApplication(isEmergencyApplication)
      withCreatedAt(createdAt)
      withArrivalDate(arrivalDate)
    }
  }
}

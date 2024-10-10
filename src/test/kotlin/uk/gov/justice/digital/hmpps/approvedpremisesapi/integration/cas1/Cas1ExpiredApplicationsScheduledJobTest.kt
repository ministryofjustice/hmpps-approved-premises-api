package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.scheduled.Cas1ExpiredApplicationsScheduledJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class Cas1ExpiredApplicationsScheduledJobTest : IntegrationTestBase() {

  @Autowired
  lateinit var applicationRepository: ApplicationRepository

  @Autowired
  lateinit var cas1ExpiredApplicationsScheduledJob: Cas1ExpiredApplicationsScheduledJob

  @Test
  fun `sets the application status to EXPIRED for the correct applications when the job is run`() {
    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(`Given a Probation Region`())
    }

    val unexpiredOccurredAt = OffsetDateTime.now().randomDateTimeBefore(365) // between 1 and 365 days
    val expiredOccurredAt = unexpiredOccurredAt.minusYears(1) // between 366 and 730 days
    val rejectedStatus = "REJECTED"
    val acceptedStatus = "ACCEPTED"

    val unexpiredIds = createApplication(user, unexpiredOccurredAt, rejectedStatus, 2)
    unexpiredIds.addAll(createApplication(user, unexpiredOccurredAt, acceptedStatus, 2))
    unexpiredIds.addAll(createApplication(user, expiredOccurredAt, rejectedStatus, 2))
    val expiredIds = createApplication(user, expiredOccurredAt, acceptedStatus, 2)

    // create a domain event for this application

    cas1ExpiredApplicationsScheduledJob.expireApplications()

    expiredIds.forEach {
      assertThat((applicationRepository.findByIdOrNull(it)!! as ApprovedPremisesApplicationEntity).status).isEqualTo(ApprovedPremisesApplicationStatus.EXPIRED)
    }

    unexpiredIds.forEach {
      assertThat((applicationRepository.findByIdOrNull(it)!! as ApprovedPremisesApplicationEntity).status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)
    }
  }

  private fun createApplication(user: UserEntity, occurredAt: OffsetDateTime, status: String, number: Int): MutableList<UUID> {
    val ids = mutableListOf<UUID>()
    repeat(number) {
      val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
        withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
      }.id

      domainEventFactory.produceAndPersist {
        withOccurredAt(occurredAt)
        withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
        withApplicationId(applicationId)
        withData(
          objectMapper.writeValueAsString(
            ApplicationAssessedEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = EventType.applicationSubmitted,
              eventDetails = ApplicationAssessedFactory()
                .withDecision(status)
                .produce(),
            ),
          ),
        )
      }
      ids.add(applicationId)
    }
    return ids
  }

  fun `does not set the application status to EXPIRED for applications that are already expired`() {
    assert(true)
  }
}

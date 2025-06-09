package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.scheduled.Cas1ExpireUnsubmittedApplicationsScheduledJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.DomainEventSummaryImpl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBeyond
import java.time.OffsetDateTime

class Cas1ExpireUnsubmittedApplicationsScheduledJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var cas1ExpireUnsubmittedApplicationsScheduledJob: Cas1ExpireUnsubmittedApplicationsScheduledJob

  @Autowired
  lateinit var domainEventService: Cas1DomainEventService

  @Autowired
  lateinit var cas1ApplicationTimelineTransformer: Cas1ApplicationTimelineTransformer

  @Test
  fun `sets the application status to EXPIRED for the correct applications when the job is run`() {
    givenAProbationRegion { probationRegion ->
      givenAUser(probationRegion = probationRegion) { user, jwt ->

        val unsubmittedApplicationsOlderThan6Months = createApplicationsWithCreatedAtDate(user, ApprovedPremisesApplicationStatus.STARTED, 5) {
          OffsetDateTime.now().randomDateTimeBeyond(180, 365)
        }
        val unsubmittedApplicationsNewerThan6Months = createApplicationsWithCreatedAtDate(user, ApprovedPremisesApplicationStatus.STARTED, 5) {
          OffsetDateTime.now().randomDateTimeBeyond(0, 179)
        }
        val applicationsWithOtherStatus = createApplicationsWithCreatedAtDate(user, ApprovedPremisesApplicationStatus.SUBMITTED, 2) {
          OffsetDateTime.now().randomDateTimeBeyond(180, 365)
        }
        applicationsWithOtherStatus.addAll(
          createApplicationsWithCreatedAtDate(user, ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS, 2) {
            OffsetDateTime.now().randomDateTimeBeyond(180, 365)
          },
        )

        cas1ExpireUnsubmittedApplicationsScheduledJob.expireApplicationsOlderThanSixMonths()

        applicationsWithOtherStatus.forEach {
          assertThat(approvedPremisesApplicationRepository.findByIdOrNull(it.id)!!.status)
            .isNotEqualTo(ApprovedPremisesApplicationStatus.EXPIRED)
        }

        unsubmittedApplicationsNewerThan6Months.forEach {
          assertThat(approvedPremisesApplicationRepository.findByIdOrNull(it.id)!!.status.name)
            .isEqualTo(ApprovedPremisesApplicationStatus.STARTED.name)
        }

        unsubmittedApplicationsOlderThan6Months.forEach {
          assertThat(approvedPremisesApplicationRepository.findByIdOrNull(it.id)!!.status.name)
            .isEqualTo(ApprovedPremisesApplicationStatus.EXPIRED.name)

          domainEventAsserter.assertDomainEventStoreCount(it.id, 1)
          val persistedApplicationExpiredEvent = domainEventAsserter.assertDomainEventOfTypeStored(
            it.id,
            DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED,
          )

          assertThat(persistedApplicationExpiredEvent).isNotNull
          assertThat(persistedApplicationExpiredEvent.applicationId).isEqualTo(it.id)
          assertThat(persistedApplicationExpiredEvent.crn).isEqualTo(it.crn)
          assertThat(persistedApplicationExpiredEvent.nomsNumber).isEqualTo(it.nomsNumber)
          assertThat(persistedApplicationExpiredEvent.occurredAt).isNotNull()
          assertThat(persistedApplicationExpiredEvent.createdAt).isNotNull()
          assertThat(persistedApplicationExpiredEvent.service).isEqualTo("CAS1")
          assertThat(persistedApplicationExpiredEvent.triggerSource).isEqualTo(TriggerSourceType.SYSTEM)

          val eventEnvelope: Cas1DomainEventEnvelope<ApplicationExpired> = objectMapper.readValue(
            persistedApplicationExpiredEvent.data,
            object : TypeReference<Cas1DomainEventEnvelope<ApplicationExpired>>() {},
          )

          val applicationExpired = eventEnvelope.eventDetails

          assertThat(applicationExpired.previousStatus).isEqualTo(it.status.name)
          assertThat(applicationExpired.statusBeforeExpiry).isEqualTo(it.status.name)
          assertThat(applicationExpired.expiryReason).isEqualTo(ApplicationExpired.ExpiryReason.unsubmittedApplicationExpired)
          assertThat(applicationExpired.updatedStatus).isEqualTo(ApprovedPremisesApplicationStatus.EXPIRED.name)
        }

        val idToCheck = unsubmittedApplicationsOlderThan6Months.first().id

        val responseBody = webTestClient.get()
          .uri("/cas1/applications/$idToCheck/timeline")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Cas1TimelineEvent>()

        val domainEvents = domainEventService.getAllDomainEventsForApplication(idToCheck)

        val expectedItems = mutableListOf<Cas1TimelineEvent>()
        expectedItems.addAll(
          domainEvents.map {
            cas1ApplicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(
              DomainEventSummaryImpl(
                it.id,
                it.type,
                it.occurredAt,
                it.applicationId,
                it.assessmentId,
                null,
                null,
                null,
                null,
                it.triggerSource,
                null,
                it.schemaVersion,
              ),
            )
          },
        )

        assertThat(responseBody.count()).isEqualTo(expectedItems.count())
        assertThat(responseBody).hasSameElementsAs(expectedItems)
        assertThat(responseBody.last().type).isEqualTo(Cas1TimelineEventType.applicationExpired)
      }
    }
  }

  private fun createApplicationsWithCreatedAtDate(user: UserEntity, status: ApprovedPremisesApplicationStatus, number: Int, createdAtGenerator: () -> OffsetDateTime): MutableList<ApprovedPremisesApplicationEntity> {
    val applications = mutableListOf<ApprovedPremisesApplicationEntity>()
    repeat(number) {
      val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
          .withCreatedAt(createdAtGenerator())
          .withStatus(status)
        withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
      }
      applications.add(application)
    }
    return applications
  }
}

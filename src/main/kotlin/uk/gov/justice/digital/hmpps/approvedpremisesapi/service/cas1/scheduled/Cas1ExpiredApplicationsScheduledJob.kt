package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.scheduled

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SaveCas1DomainEventWithPayload
import java.time.Instant
import java.util.UUID

@Service
class Cas1ExpiredApplicationsScheduledJob(
  private val domainEventService: Cas1DomainEventService,
  private val applicationRepository: ApplicationRepository,
  private val transactionTemplate: TransactionTemplate,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Scheduled(cron = "0 0 2 * * ?")
  @SchedulerLock(name = "cas1_expire_applications", lockAtMostFor = "5m", lockAtLeastFor = "1m")
  fun expireApplications() {
    val applicationIds = applicationRepository.findAllExpiredApplications()

    log.info("There are ${applicationIds.size} applications to update.")

    applicationIds.forEach { applicationId ->
      log.info("Expiring application $applicationId.")

      transactionTemplate.executeWithoutResult {
        val application = applicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
        expireApplication(application)
      }
    }
  }

  fun expireApplication(
    application: ApprovedPremisesApplicationEntity,
  ) {
    val applicationId = application.id
    val previousStatus = application.status.name
    application.status = ApprovedPremisesApplicationStatus.EXPIRED
    applicationRepository.save(application)

    log.info("Status changed from $previousStatus to ${application.status} for application $applicationId.")

    val applicationExpiredPayLoad = ApplicationExpired(
      applicationId,
      previousStatus,
      (applicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity).status.name,
      statusBeforeExpiry = previousStatus,
      expiryReason = ApplicationExpired.ExpiryReason.assessmentExpired,
    )

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()
    domainEventService.save(
      SaveCas1DomainEventWithPayload(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = application.nomsNumber,
        occurredAt = eventOccurredAt,
        data = applicationExpiredPayLoad,
        triggerSource = TriggerSourceType.SYSTEM,
        schemaVersion = 2,
        type = DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED,
        assessmentId = null,
        bookingId = null,
        cas1SpaceBookingId = null,
        metadata = emptyMap(),
        emit = true,
      ),
    )
    log.info("Domain event id $domainEventId emitted for application $applicationId.")
  }
}

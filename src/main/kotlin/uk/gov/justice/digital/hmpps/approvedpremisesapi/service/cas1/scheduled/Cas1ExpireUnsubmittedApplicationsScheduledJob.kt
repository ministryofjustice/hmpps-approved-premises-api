package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.scheduled

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SaveCas1DomainEventWithPayload
import java.time.Instant
import java.util.UUID

@Service
class Cas1ExpireUnsubmittedApplicationsScheduledJob(
  private val domainEventService: Cas1DomainEventService,
  private val approvedPremisesApplicationRepository: ApprovedPremisesApplicationRepository,
  private val transactionTemplate: TransactionTemplate,
  private val featureFlagService: FeatureFlagService,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
  @Scheduled(cron = "0 0 3 * * ?")
  @SchedulerLock(name = "cas1_expire_unsubmitted_applications", lockAtMostFor = "5m", lockAtLeastFor = "1m")
  fun expireApplicationsOlderThanSixMonths() {
    if (featureFlagService.getBooleanFlag("cas1-expire-unsubmitted-applications-job-enabled")) {
      val applicationIdsToExpire =
        approvedPremisesApplicationRepository.findIdsForUnsubmittedApplicationsOlderThanSixMonths()

      log.info("Found ${applicationIdsToExpire.size} unsubmitted applications older than six months to expire")

      val batchSize = 100
      applicationIdsToExpire.chunked(batchSize).forEach { batch ->
        log.info("Processing batch of ${batch.size} applications")

        transactionTemplate.executeWithoutResult {
          val applications = approvedPremisesApplicationRepository.findAllById(batch)

          applications.forEach { application ->
            application.status = ApprovedPremisesApplicationStatus.EXPIRED
          }

          approvedPremisesApplicationRepository.saveAll(applications)

          log.info("Successfully updated batch of ${applications.size} applications with EXPIRED status")

          applications.forEach { application ->
            val previousStatus = ApprovedPremisesApplicationStatus.STARTED.name
            val applicationExpiredPayload = ApplicationExpired(
              application.id,
              previousStatus,
              application.status.name,
              statusBeforeExpiry = previousStatus,
              expiryReason = ApplicationExpired.ExpiryReason.unsubmittedApplicationExpired,
            )

            domainEventService.save(
              SaveCas1DomainEventWithPayload(
                id = UUID.randomUUID(),
                applicationId = application.id,
                crn = application.crn,
                nomsNumber = application.nomsNumber,
                occurredAt = Instant.now(),
                data = applicationExpiredPayload,
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
            log.info("Domain event raised for application ID: ${application.id}")
          }
        }
      }
    }
  }
}

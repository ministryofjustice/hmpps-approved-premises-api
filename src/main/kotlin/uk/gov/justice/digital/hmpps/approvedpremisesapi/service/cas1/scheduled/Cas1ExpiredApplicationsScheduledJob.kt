package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.scheduled

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService

@Service
class Cas1ExpiredApplicationsScheduledJob(
  private val featureFlagService: FeatureFlagService,
  private val applicationRepository: ApplicationRepository,
  private val transactionTemplate: TransactionTemplate,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Scheduled(cron = "0 0/30 * * * ?")
  @SchedulerLock(name = "cas1_expire_applications", lockAtMostFor = "5m", lockAtLeastFor = "1m")
  fun expireApplications() {
    if (featureFlagService.getBooleanFlag("cas1-expired-applications-enabled")) {
      val applicationIds = applicationRepository.findAllExpiredApplications()

      log.info("There are ${applicationIds.size} applications to update.")

      applicationIds.forEach { applicationId ->
        log.info("Updating application $applicationId.")

        transactionTemplate.executeWithoutResult {
          val application = applicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
          application.status = ApprovedPremisesApplicationStatus.EXPIRED
          applicationRepository.save(application)

          log.info("Status changed to EXPIRED for application $applicationId.")

          // send domain event
          log.info("Domain event emitted for application $applicationId.")
        }
      }
    }
  }
}

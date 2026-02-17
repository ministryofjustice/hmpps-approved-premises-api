package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.scheduled

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository.Companion.BED_ON_HOLD_REASON_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import java.time.LocalDate

@Service
class Cas1CancelBedOnHoldScheduledJob(
  private val cas1OutOfServiceBedRepository: Cas1OutOfServiceBedRepository,
  private val cas1OutOfServiceBedService: Cas1OutOfServiceBedService,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Scheduled(cron = "0 0 1 * * ?")
  @SchedulerLock(name = "cas1_cancel_bed_on_hold", lockAtMostFor = "5m", lockAtLeastFor = "1m")
  fun autoCancelBedOnHoldsPastStartDate() {
    log.info("Starting cancellation of BedOnHolds past start date")

    val bedsOnHoldPastStartDate = cas1OutOfServiceBedRepository.findBedOnHoldsPastStartDate(BED_ON_HOLD_REASON_ID, LocalDate.now())

    log.info("Found ${bedsOnHoldPastStartDate.size} BedOnHolds past start date")

    bedsOnHoldPastStartDate.forEach { bedOnHold ->
      cas1OutOfServiceBedService.cancelOutOfServiceBed(bedOnHold, "Auto Cancellation")
    }
    log.info("Finished cancellation of BedOnHolds past start date")
  }
}

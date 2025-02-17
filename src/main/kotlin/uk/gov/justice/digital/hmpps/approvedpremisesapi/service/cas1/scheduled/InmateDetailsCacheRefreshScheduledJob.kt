package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.scheduled

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.InmateDetailsCacheRefreshResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.InmateDetailsCacheRefreshService

@Service
class InmateDetailsCacheRefreshScheduledJob(
  private val inmateDetailsCacheRefreshService: InmateDetailsCacheRefreshService,
) {

  @Scheduled(cron = "0 0 0/4 * * *")
  @SchedulerLock(
    name = "inmate_details_cache_refresh",
    lockAtMostFor = "\${refresh-inmate-details-cache.lock-at-most-for}",
    lockAtLeastFor = "\${refresh-inmate-details-cache.lock-at-least-for}",
  )
  fun refreshInmateDetailsCache(): InmateDetailsCacheRefreshResults? = inmateDetailsCacheRefreshService.refreshInmateDetailsCache()
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType

@Service
class SeedService {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Async
  fun seedData(seedFileType: SeedFileType, filename: String) {
    log.info("Starting seed request: $seedFileType - $filename")
  }
}

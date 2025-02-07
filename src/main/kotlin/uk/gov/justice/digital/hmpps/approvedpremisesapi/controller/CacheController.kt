package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CacheType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.CacheApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CacheService

@Service
class CacheController(private val cacheService: CacheService) : CacheApiDelegate {
  override fun cacheCacheNameDelete(cacheName: CacheType): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    cacheService.clearCache(cacheName)

    return ResponseEntity(HttpStatus.OK)
  }
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CacheType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CacheService

@RestController
@Tag(name = "default")
class CacheController(private val cacheService: CacheService) {

  @Operation(
    summary = "Clears the given cache, can only be called from a local connection",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully cleared cache"),
    ],
  )
  @DeleteMapping(
    value = ["/cache/{cacheName}"],
  )
  fun cacheCacheNameDelete(
    @PathVariable cacheName: CacheType,
  ): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    cacheService.clearCache(cacheName)

    return ResponseEntity(HttpStatus.OK)
  }
}

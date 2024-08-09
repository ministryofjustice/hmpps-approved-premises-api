package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CacheType

@Service
class CacheService(
  private val cacheManager: CacheManager,
  private val redisTemplate: RedisTemplate<String, String>,
  @Value("\${preemptive-cache-key-prefix}") private val preemptiveCacheKeyPrefix: String,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun clearCache(cacheName: CacheType) {
    when (cacheName) {
      CacheType.inmateDetails -> clearPreemptiveCache("inmateDetails")
      CacheType.qCodeStaffMembers -> clearRegularCache("qCodeStaffMembersCache")
      CacheType.userAccess -> clearRegularCache("userAccessCache")
      CacheType.staffDetails -> clearRegularCache("staffDetailsCache")
      CacheType.teamsManagingCase -> clearRegularCache("teamsManagingCaseCache")
      CacheType.ukBankHolidays -> clearRegularCache("ukBankHolidaysCache")
    }
  }

  private fun clearRegularCache(cacheName: String) {
    cacheManager.getCache(cacheName)!!.invalidate()

    log.info("Cleared regular cache $cacheName")
  }

  private fun clearPreemptiveCache(cacheName: String) {
    var total: Int
    redisTemplate.keys("$preemptiveCacheKeyPrefix-$cacheName-**")
      .apply { total = size }
      .forEach(redisTemplate::delete)

    log.info("Cleared $total entries from preemptive cache $cacheName")
  }
}

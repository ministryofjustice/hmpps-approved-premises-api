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
      CacheType.INMATE_DETAILS -> clearPreemptiveCache("inmateDetails")
      CacheType.Q_CODE_STAFF_MEMBERS -> clearRegularCache("qCodeStaffMembersCache")
      CacheType.USER_ACCESS -> clearRegularCache("userAccessCache")
      CacheType.STAFF_DETAILS -> clearRegularCache("staffDetailsCache")
      CacheType.TEAMS_MANAGING_CASE -> clearRegularCache("teamsManagingCaseCache")
      CacheType.UK_BANK_HOLIDAYS -> clearRegularCache("ukBankHolidaysCache")
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

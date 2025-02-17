package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CacheKeySet
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.client.PreemptiveCacheTest.Companion.CACHE_NAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockSuccessfulInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.InmateDetailsCacheRefreshService
import java.util.UUID

class InmateDetailsCacheRefreshServiceTest : IntegrationTestBase() {

  @Autowired
  lateinit var inmateDetailsCacheRefreshService: InmateDetailsCacheRefreshService

  @Autowired
  lateinit var domainEventService: Cas1DomainEventService

  @Test
  fun `should not refresh inmate details cache when feature flag off`() {
    mockFeatureFlagService.setFlag("cas1-enable-scheduled-job-refresh-inmate-details", false)
    assertThat(inmateDetailsCacheRefreshService.refreshInmateDetailsCache()).isNull()
  }

  @Test
  fun `successfully updates 1 cache entry`() {
    mockFeatureFlagService.setFlag("cas1-enable-scheduled-job-refresh-inmate-details", true)
    givenAProbationRegion { probationRegion ->
      givenAUser(probationRegion = probationRegion) { user, _ ->
        val inmateDetail = createInmateDetailAndApplication(user)
        prisonAPIMockSuccessfulInmateDetailsCall(inmateDetail)

        val results = inmateDetailsCacheRefreshService.refreshInmateDetailsCache()!!
        assertThat(results.candidatesCount).isEqualTo(1)
        assertThat(results.entriesProcessed).isEqualTo(1)
        assertThat(results.entryUpdates).isEqualTo(1)
        assertThat(results.entryUpdateFails).isEqualTo(0)

        isInmateDetailNowInRedis(inmateDetail)
      }
    }
  }

  @Test
  fun `fails update of 1 cache entry as inmate details not found in PrisonApi`() {
    mockFeatureFlagService.setFlag("cas1-enable-scheduled-job-refresh-inmate-details", true)
    givenAProbationRegion { probationRegion ->
      givenAUser(probationRegion = probationRegion) { user, _ ->
        createInmateDetailAndApplication(user)

        val results = inmateDetailsCacheRefreshService.refreshInmateDetailsCache()!!
        assertThat(results.candidatesCount).isEqualTo(1)
        assertThat(results.entriesProcessed).isEqualTo(1)
        assertThat(results.entryUpdates).isEqualTo(0)
        assertThat(results.entryUpdateFails).isEqualTo(1)
      }
    }
  }

  @Test
  fun `successfully updates 4 cache entries and fails 1 cache entry`() {
    mockFeatureFlagService.setFlag("cas1-enable-scheduled-job-refresh-inmate-details", true)
    givenAProbationRegion { probationRegion ->
      givenAUser(probationRegion = probationRegion) { user, _ ->
        for (x in 1..5) {
          val inmateDetail = createInmateDetailAndApplication(user)
          if (x != 1) {
            prisonAPIMockSuccessfulInmateDetailsCall(inmateDetail)
          }
        }

        val results = inmateDetailsCacheRefreshService.refreshInmateDetailsCache()!!
        assertThat(results.candidatesCount).isEqualTo(5)
        assertThat(results.entriesProcessed).isEqualTo(5)
        assertThat(results.entryUpdates).isEqualTo(4)
        assertThat(results.entryUpdateFails).isEqualTo(1)
      }
    }
  }

  private fun isInmateDetailNowInRedis(inmateDetail: InmateDetail) {
    val keys = CacheKeySet(preemptiveCacheKeyPrefix, CACHE_NAME, inmateDetail.offenderNo)
    val cachedResult = objectMapper.readValue<InmateDetail>(
      redisTemplate.boundValueOps(keys.dataKey).get()!!,
    )
    assertThat(cachedResult).isEqualTo(inmateDetail)
  }

  private fun createInmateDetailAndApplication(user: UserEntity): InmateDetail {
    val inmateDetail = InmateDetail(
      offenderNo = UUID.randomUUID().toString(),
      custodyStatus = InmateStatus.IN,
      assignedLivingUnit = null,
    )
    approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
      withNomsNumber(inmateDetail.offenderNo)
    }
    return inmateDetail
  }
}

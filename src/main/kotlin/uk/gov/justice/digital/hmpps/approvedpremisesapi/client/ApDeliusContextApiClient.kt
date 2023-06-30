package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.IS_NOT_SUCCESSFUL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage

@Component
class ApDeliusContextApiClient(
  @Qualifier("apDeliusContextApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper,
  redisTemplate: RedisTemplate<String, String>,
  @Value("\${preemptive-cache-key-prefix}") preemptiveCacheKeyPrefix: String,
) : BaseHMPPSClient(webClient, objectMapper, redisTemplate, preemptiveCacheKeyPrefix) {
  @Cacheable(value = ["qCodeStaffMembersCache"], unless = IS_NOT_SUCCESSFUL)
  fun getStaffMembers(qCode: String) = getRequest<StaffMembersPage> {
    path = "/approved-premises/$qCode/staff"
  }

  @Cacheable(value = ["teamsManagingCaseCache"], unless = IS_NOT_SUCCESSFUL)
  fun getTeamsManagingCase(crn: String) = getRequest<ManagingTeamsResponse> {
    path = "/teams/managingCase/$crn"
  }
}

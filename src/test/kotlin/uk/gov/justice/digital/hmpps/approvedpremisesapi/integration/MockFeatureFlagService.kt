package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService

@Primary
@Service
class MockFeatureFlagService : FeatureFlagService {
  override fun getBooleanFlag(key: String, default: Boolean) = true
}

package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "prison-adjudications")
class PrisonAdjudicationsConfigBindingModel {
  var prisonApiPageSize: Int? = null
}

data class PrisonAdjudicationsConfig(
  val prisonApiPageSize: Int
)

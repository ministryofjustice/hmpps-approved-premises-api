package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NewPlacementApplication(

  @get:JsonProperty("applicationId", required = true) val applicationId: java.util.UUID,
)

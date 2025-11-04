package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 */
class UpdateTemporaryAccommodationApplication(

  @get:JsonProperty("type", required = true) override val type: UpdateApplicationType,

  @get:JsonProperty("data", required = true) override val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,
) : UpdateApplication

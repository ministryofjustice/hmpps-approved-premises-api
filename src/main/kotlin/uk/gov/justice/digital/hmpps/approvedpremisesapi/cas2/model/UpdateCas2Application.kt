package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType

/**
 *
 */
class UpdateCas2Application(

  @get:JsonProperty("type", required = true) override val type: UpdateApplicationType,

  @get:JsonProperty("data", required = true) override val `data`: Map<String, Any>,
) : UpdateApplication

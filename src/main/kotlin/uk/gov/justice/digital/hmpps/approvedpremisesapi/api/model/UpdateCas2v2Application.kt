package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class UpdateCas2v2Application(

  @get:JsonProperty("type", required = true) override val type: UpdateApplicationType,

  @get:JsonProperty("data", required = true) override val `data`: Map<String, Any>,

  @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,
) : UpdateApplication

package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import java.time.LocalDate

data class UpdateCas2v2Application(

    @get:JsonProperty("type", required = true) override val type: UpdateApplicationType,

    @get:JsonProperty("data", required = true) override val `data`: Map<String, Any>,

    @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,

    @get:JsonProperty("cohort") val cohort: Cas2CohortDto? = null,

    ) : UpdateApplication
